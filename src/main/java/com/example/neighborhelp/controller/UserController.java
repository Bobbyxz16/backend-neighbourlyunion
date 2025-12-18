package com.example.neighborhelp.controller;

import com.example.neighborhelp.dto.*;
import com.example.neighborhelp.dto.AdminDto.AdminUserListResponse;
import com.example.neighborhelp.dto.AdminDto.CreateUserRequest;
import com.example.neighborhelp.dto.AdminDto.UpdateRoleRequest;
import com.example.neighborhelp.service.UserService;
import com.example.neighborhelp.service.UserStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

/**
 * User Controller - Handles user profile and management operations
 *
 * Provides endpoints for:
 * - Getting and updating user profiles
 * - Admin operations (verify organizations, block/unblock users)
 * - Public user information retrieval
 *
 * Supports dual identifier access:
 * - Username for individual users
 * - Organization name for organization users
 *
 * @author NeighborHelp Team
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserStatisticsService userStatisticsService;

    // ========== AUTHENTICATED USER ENDPOINTS ==========

    /**
     * GET /api/users/me
     * Get current authenticated user's profile
     */
    @GetMapping(
            value = "/me",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<UserResponseRequest> getCurrentUser(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        String email = userDetails.getUsername();
        UserResponseRequest userResponse = userService.getUserProfileByEmail(email);
        return ResponseEntity.ok(userResponse);
    }

    /**
     * PUT /api/users/me
     * Update current authenticated user's profile
     */
    @PutMapping(
            value = "/me",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<UserResponseRequest> updateCurrentUser(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
            @Valid @RequestBody UpdateUserRequest request) {

        String email = userDetails.getUsername();
        UserResponseRequest userResponse = userService.updateUserProfileByEmail(email, request);
        return ResponseEntity.ok(userResponse);
    }

    /**
     * DELETE /api/users/me
     * Delete current user's account (soft delete)
     */
    @DeleteMapping(
            value = "/me",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<MessageResponseRequest> deleteCurrentUser(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        try {
            String email = userDetails.getUsername();
            userService.softDeleteUserByEmail(email);

            return ResponseEntity.ok()
                    .body(new MessageResponseRequest("Account successfully deleted"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponseRequest("Error deleting account: " + e.getMessage()));
        }
    }

    // ========== PUBLIC USER PROFILE ENDPOINTS ==========

    /**
     * GET /api/users/{identifier}
     * Get user profile by username (individuals) or organization name (organizations)
     */
    @GetMapping(
            value = "/{identifier}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<UserResponseRequest> getUserByIdentifier(@PathVariable("identifier") String identifier) {
        UserResponseRequest userResponse = userService.getPublicUserProfileByIdentifier(identifier);
        return ResponseEntity.ok(userResponse);
    }

    /**
     * GET /api/users/{identifier}/statistics
     * Get user statistics by identifier
     */
    @GetMapping(
            value = "/{identifier}/statistics",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<UserStatisticsResponseRequest> getUserStatistics(@PathVariable("identifier") String identifier) {
        UserStatisticsResponseRequest statistics = userStatisticsService.getUserStatisticsByIdentifier(identifier);
        return ResponseEntity.ok(statistics);
    }

    /**
     * GET /api/users/{identifier}/statistics/basic
     * Get basic statistics by identifier
     */
    @GetMapping(
            value = "/{identifier}/statistics/basic",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<UserStatisticsResponseRequest> getBasicStatistics(@PathVariable("identifier") String identifier) {
        UserStatisticsResponseRequest statistics = userStatisticsService.getBasicStatisticsByIdentifier(identifier);
        return ResponseEntity.ok(statistics);
    }

    // ========== ADMIN/MODERATOR ENDPOINTS ==========

    /**
     * PATCH /api/users/{identifier}/verify
     * Verify an organization (Admin/Moderator only)
     */
    @PatchMapping(
            value = "/{identifier}/verify",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<MessageResponseRequest> verifyOrganization(@PathVariable("identifier") String identifier) {
        userService.verifyOrganizationByIdentifier(identifier);
        return ResponseEntity.ok(new MessageResponseRequest("Organization verified successfully"));
    }

    /**
     * PATCH /api/users/{identifier}/status
     * Update user account status - enable/disable (Admin/Moderator only)
     */
    @PatchMapping(
            value = "/{identifier}/status",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<MessageResponseRequest> updateUserStatus(
            @PathVariable("identifier") String identifier,
            @RequestBody Map<String, Boolean> requestBody) {

        Boolean enabled = requestBody.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponseRequest("'enabled' is required"));
        }

        userService.updateUserStatusByIdentifier(identifier, enabled);

        String message = enabled
                ? "User account enabled successfully"
                : "User account disabled successfully";

        return ResponseEntity.ok(new MessageResponseRequest(message));
    }


    /**
     * GET /api/users
     * Get paginated list of all users with filtering (Admin only)
     *
     * Query params:
     * - role: Filter by role (USER, ADMIN, MODERATOR)
     * - type: Filter by type (INDIVIDUAL, ORGANIZATION)
     * - enabled: Filter by enabled status (true/false)
     * - verified: Filter by verified status (true/false)
     * - includeDeleted: Include deleted users (true/false, default: false)
     * - search: Search in username, email, firstName, lastName, organizationName
     * - page: Page number (default 0)
     * - size: Page size (default 20)
     * - sort: Sort field and direction (default: createdAt,desc)
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AdminUserListResponse>> getAllUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) Boolean includeDeleted,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<AdminUserListResponse> users = userService.getAllUsers(
                role, type, enabled, verified, includeDeleted, search, pageable
        );
        return ResponseEntity.ok(users);
    }


    /**
     * POST /api/users
     * Create a new user (Admin only)
     * Allows admins to create users with any role (USER, MODERATOR, ADMIN)
     */
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseRequest> createUser(
            @Valid @RequestBody CreateUserRequest request) {

        UserResponseRequest userResponse = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

    /**
     * PATCH /api/users/{identifier}/role
     * Update user role (Admin only)
     */
    @PatchMapping(
            value = "/{identifier}/role",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseRequest> updateUserRole(
            @PathVariable("identifier") String identifier,
            @Valid @RequestBody UpdateRoleRequest request) {

        UserResponseRequest userResponse = userService.updateUserRole(identifier, request.getRole());
        return ResponseEntity.ok(userResponse);
    }

    /**
     * DELETE /api/users/{identifier}
     * Soft delete a user account (Admin only)
     */
    @DeleteMapping(
            value = "/{identifier}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponseRequest> deleteUser(@PathVariable("identifier") String identifier) {
        userService.deleteUserByIdentifier(identifier);
        return ResponseEntity.ok(new MessageResponseRequest("User deleted successfully"));
    }


}
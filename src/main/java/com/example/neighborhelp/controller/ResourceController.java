package com.example.neighborhelp.controller;

import com.example.neighborhelp.dto.FileUploadResponse;
import com.example.neighborhelp.dto.MessageResponseRequest;
import com.example.neighborhelp.dto.ResourcesDto.CreateResourceRequest;
import com.example.neighborhelp.dto.ResourcesDto.ResourceResponseRequest;
import com.example.neighborhelp.dto.ResourcesDto.ResourceStatusOverviewResponse;
import com.example.neighborhelp.dto.ResourcesDto.UpdateResourceRequest;
import com.example.neighborhelp.entity.Resource;
import com.example.neighborhelp.entity.User;
import com.example.neighborhelp.repository.UserRepository;
import com.example.neighborhelp.service.FileStorageService;
import com.example.neighborhelp.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    /**
     * GET /api/resources
     * Search and filter resources
     * Public endpoint - no auth required
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<ResourceResponseRequest>> searchResources(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String postalCode,
            @RequestParam(required = false) String cost,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<ResourceResponseRequest> resources = resourceService.searchResources(
                categoryId, city, postalCode, cost, null, search, pageable
        );
        return ResponseEntity.ok(resources);
    }

    /**
     * POST /api/resources
     * Create new resource
     * Auth required - USER, ORGANIZATION, ADMIN
     */
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZATION', 'ADMIN')")
    public ResponseEntity<ResourceResponseRequest> createResource(
            @Valid @RequestBody CreateResourceRequest request) {

        // Obtener el email del usuario autenticado
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName(); // Esto devuelve el email

        // Buscar el usuario por email en la base de datos
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        ResourceResponseRequest resource = resourceService.createResource(
                request, currentUser.getId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(resource);
    }

    /**
     * GET /api/resources/{resourceName}
     * Get resource by name (slug)
     * Public endpoint - Increments view count automatically
     */
    @GetMapping(value = "/{resourceName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResourceResponseRequest> getResourceByName(
            @PathVariable String resourceName) {
        ResourceResponseRequest resource = resourceService.getResourceBySlug(resourceName);
        return ResponseEntity.ok(resource);
    }

    /**
     * PUT /api/resources/{resourceName}
     * Update resource
     * Auth required - Owner or ADMIN
     */
    @PutMapping(
            value = "/{resourceName}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResourceResponseRequest> updateResource(
            @PathVariable String resourceName,
            @Valid @RequestBody UpdateResourceRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        // Buscar el usuario por email en la base de datos
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        ResourceResponseRequest updatedResource = resourceService.updateResourceBySlug(
                resourceName, request, currentUser.getId()
        );
        return ResponseEntity.ok(updatedResource);
    }


    /**
     * DELETE /api/resources/{resourceName}/permanent
     * PERMANENTLY DELETE resource (completely remove from database)
     * Auth required - Owner or ADMIN
     */
    @DeleteMapping(value = "/{resourceName}/permanent", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MessageResponseRequest> permanentlyDeleteResource(
            @PathVariable String resourceName) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        // Buscar el usuario por email en la base de datos
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));


        resourceService.permanentlyDeleteResourceBySlug(
                resourceName, currentUser.getId()
        );
        return ResponseEntity.ok(new MessageResponseRequest("Resource permanently deleted"));
    }

    /**
     * PATCH /api/resources/{resourceName}/deactivate
     * DEACTIVATE resource (set status to INACTIVE - hide from public)
     * Auth required - Owner or ADMIN
     */
    @PatchMapping(value = "/{resourceName}/deactivate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MessageResponseRequest> deactivateResource(
            @PathVariable String resourceName) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        // Buscar el usuario por email en la base de datos
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));


        resourceService.deactivateResourceBySlug(
                resourceName, currentUser.getId()
        );
        return ResponseEntity.ok(new MessageResponseRequest("Resource deactivated and hidden from public"));
    }

    /**
     * PATCH /api/resources/{resourceName}/activate
     * ACTIVATE resource (set status to ACTIVE - make visible to public)
     * Auth required - Owner or ADMIN
     */
    @PatchMapping(value = "/{resourceName}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MessageResponseRequest> activateResource(
            @PathVariable String resourceName) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        // Buscar el usuario por email en la base de datos
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        resourceService.activateResourceBySlug(
                resourceName, currentUser.getId()
        );
        return ResponseEntity.ok(new MessageResponseRequest("Resource activated and visible to public"));
    }


    /**
     * GET /api/resources/admin/status-overview
     * Get comprehensive status overview for all resources
     * Format: username - resource title - status
     * Auth required - ADMIN, MODERATOR
     *
     * Query params:
     * - status: Filter by specific status (ACTIVE, INACTIVE, PENDING, REJECTED)
     * - search: Search in resource title/description
     * - username: Filter by username/organization name
     * - page: Page number (default 0)
     * - size: Page size (default 20)
     */
    @GetMapping(value = "/admin/status-overview", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<Page<ResourceStatusOverviewResponse>> getResourcesStatusOverview(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String username,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<ResourceStatusOverviewResponse> overview = resourceService.getResourcesStatusOverview(
                status, search, username, pageable
        );
        return ResponseEntity.ok(overview);
    }

    /**
     * PATCH /api/resources/{resourceName}/status
     * Change resource status
     * Auth required - Owner or ADMIN
     * Status: ACTIVE, INACTIVE, PENDING, REJECTED
     */
    @PatchMapping(
            value = "/{resourceName}/status",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<MessageResponseRequest> updateResourceStatus(
            @PathVariable String resourceName,
            @RequestBody Map<String, String> requestBody) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        // Buscar el usuario por email en la base de datos
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        String status = requestBody.get("status");
        if (status == null || status.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponseRequest("'status' field is required"));
        }

        try {
            Resource.ResourceStatus resourceStatus = Resource.ResourceStatus.valueOf(status.toUpperCase());
            resourceService.updateResourceStatusBySlug(
                    resourceName, resourceStatus, currentUser.getId()
            );
            return ResponseEntity.ok(
                    new MessageResponseRequest("Resource status updated to: " + status)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponseRequest("Invalid status. Valid values: ACTIVE, INACTIVE, PENDING, REJECTED"));
        }
    }

    /**
     * GET /api/resources/user/{userId}
     * Get all public resources by a user
     * Public endpoint
     */
    @GetMapping(value = "/user/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<ResourceResponseRequest>> getUserResources(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<ResourceResponseRequest> resources = resourceService.getUserActiveResources(
                userId, pageable
        );
        return ResponseEntity.ok(resources);
    }

    /**
     * GET /api/resources/my-resources
     * Get current user's resources (all statuses)
     * Auth required
     */
    @GetMapping(value = "/my-resources", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<ResourceResponseRequest>> getMyResources(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        // Buscar el usuario por email en la base de datos
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        Page<ResourceResponseRequest> resources = resourceService.getUserResources(
                currentUser.getId(), pageable
        );
        return ResponseEntity.ok(resources);
    }


    /**
     * GET /api/resources/pending
     * Get pending resources for moderation
     * Auth required - ADMIN, MODERATOR
     */
    @GetMapping(value = "/pending", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<Page<ResourceResponseRequest>> getPendingResources(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC)
            Pageable pageable) {

        Page<ResourceResponseRequest> resources = resourceService.getPendingResources(pageable);
        return ResponseEntity.ok(resources);
    }

    /**
     * PATCH /api/resources/{resourceName}/approve
     * Approve pending resource
     * Auth required - ADMIN, MODERATOR
     */
    @PatchMapping(value = "/{resourceName}/approve", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<MessageResponseRequest> approveResource(@PathVariable String resourceName) {
        resourceService.approveResourceBySlug(resourceName);
        return ResponseEntity.ok(new MessageResponseRequest("Resource approved successfully"));
    }

    /**
     * PATCH /api/resources/{resourceName}/reject
     * Reject pending resource
     * Auth required - ADMIN, MODERATOR
     */
    @PatchMapping(
            value = "/{resourceName}/reject",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<MessageResponseRequest> rejectResource(
            @PathVariable String resourceName,
            @RequestBody(required = false) Map<String, String> requestBody) {

        String reason = requestBody != null ? requestBody.get("reason") : null;
        resourceService.rejectResourceBySlug(resourceName, reason);
        return ResponseEntity.ok(new MessageResponseRequest("Resource rejected successfully"));
    }

    /**
     * POST /api/resources/{resourceName}/save
     * Save/favorite a resource
     * Auth required
     */
    @PostMapping(value = "/{resourceName}/save", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MessageResponseRequest> saveResource(@PathVariable String resourceName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        resourceService.saveResource(resourceName, currentUser.getId());
        return ResponseEntity.ok(new MessageResponseRequest("Resource saved successfully"));
    }

    /**
     * DELETE /api/resources/{resourceName}/save
     * Unsave/unfavorite a resource
     * Auth required
     */
    @DeleteMapping(value = "/{resourceName}/save", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MessageResponseRequest> unsaveResource(@PathVariable String resourceName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        resourceService.unsaveResource(resourceName, currentUser.getId());
        return ResponseEntity.ok(new MessageResponseRequest("Resource unsaved successfully"));
    }

    /**
     * GET /api/resources/saved
     * Get all saved resources for current user
     * Auth required
     */
    @GetMapping(value = "/saved", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<ResourceResponseRequest>> getSavedResources(
            @PageableDefault(size = 20, sort = "savedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        Page<ResourceResponseRequest> resources = resourceService.getSavedResources(
                currentUser.getId(), pageable
        );
        return ResponseEntity.ok(resources);
    }

    /**
     * GET /api/resources/{resourceName}/is-saved
     * Check if resource is saved by current user
     * Auth required
     */
    @GetMapping(value = "/{resourceName}/is-saved", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Boolean>> isResourceSaved(@PathVariable String resourceName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        boolean isSaved = resourceService.isResourceSaved(resourceName, currentUser.getId());
        return ResponseEntity.ok(Map.of("isSaved", isSaved));
    }

    /**
     * POST /api/resources/upload
     * Upload image file for resource
     * Auth required - USER, ORGANIZATION, ADMIN
     *
     * @param file - MultipartFile (max 5MB, JPEG/PNG/GIF only)
     * @return FileUploadResponse with public URL
     */
    @PostMapping(
            value = "/uploads",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZATION', 'ADMIN')")
    public ResponseEntity<FileUploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file) {

        String fileUrl = fileStorageService.storeFile(file);

        FileUploadResponse response = FileUploadResponse.builder()
                .url(fileUrl)
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .size(file.getSize())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/resources/upload/multiple
     * Upload multiple images (up to 5 files)
     * Auth required - USER, ORGANIZATION, ADMIN
     */
    @PostMapping(
            value = "/uploads/multiple",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasAnyRole('USER', 'ORGANIZATION', 'ADMIN')")
    public ResponseEntity<?> uploadMultipleImages(
            @RequestParam("files") MultipartFile[] files) {

        if (files.length > 5) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Maximum 5 files allowed"));
        }

        List<FileUploadResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            String fileUrl = fileStorageService.storeFile(file);
            responses.add(FileUploadResponse.builder()
                    .url(fileUrl)
                    .fileName(file.getOriginalFilename())
                    .fileType(file.getContentType())
                    .size(file.getSize())
                    .build());
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("files", responses));
    }


}
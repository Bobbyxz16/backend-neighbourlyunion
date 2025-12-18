package com.example.neighborhelp.controller;

import com.example.neighborhelp.dto.*;
import com.example.neighborhelp.entity.VerificationCode;
import com.example.neighborhelp.exception.InvalidTokenException;
import com.example.neighborhelp.dto.TokenResponseRequest;
import com.example.neighborhelp.service.AuthService;
import com.example.neighborhelp.service.VerificationService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication Controller - Handles all authentication-related operations
 * including registration, email verification, login, and password reset.
 *
 * This controller provides the main authentication API endpoints for the NeighborHelp application.
 *
 * @author NeighborHelp Team
 */
@RestController  // Marks this class as a Spring REST Controller
@RequestMapping("/api/auth")  // Base path for all endpoints in this controller
@RequiredArgsConstructor  // Lombok annotation to generate constructor with required fields
public class AuthController {

    // Service dependencies injected via constructor
    private final AuthService authService;  // Handles authentication business logic
    private final VerificationService verificationService;  // Handles email verification codes

    /**
     * POST /api/auth/register
     * Registers a new user and sends verification code via email
     *
     * Flow:
     * 1. Validates registration request
     * 2. Creates user account (unverified)
     * 3. Sends 6-digit verification code to user's email
     * 4. Returns user details without sensitive information
     *
     * @param request UpdatedRegisterRequest containing user registration data
     * @return ResponseEntity with success message and user details (HTTP 201 Created)
     */
    @PostMapping(
            value = "/register",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        // Register user and get response without sensitive data
        UserResponseRequest userResponse = authService.register(request);

        // Build response object
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Registration successful. Please check your email for the verification code.");
        response.put("user", userResponse);
        response.put("email", userResponse.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/verify
     * Verifies user's email using the 6-digit verification code
     *
     * Flow:
     * 1. Validates verification code for the given email
     * 2. Marks user as verified if code is valid
     * 3. Allows user to login after successful verification
     *
     * @param request VerifyEmailRequest containing email and verification code
     * @return ResponseEntity with success or error message
     */
    @PostMapping(
            value = "/verify",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<MessageResponseRequest> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        try {
            // Verify the 6-digit code for email verification type
            verificationService.verifyCode(
                    request.getEmail(),
                    request.getCode(),
                    VerificationCode.VerificationType.EMAIL_VERIFICATION
            );

            return ResponseEntity.ok(
                    new MessageResponseRequest("Email verified successfully. You can now log in.")
            );

        } catch (InvalidTokenException e) {
            // Return 400 Bad Request for invalid verification codes
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponseRequest(e.getMessage()));
        }
    }

    /**
     * POST /api/auth/resend
     * Resends verification code to user's email
     *
     * Useful when:
     * - User didn't receive the first code
     * - Code expired
     * - User requests a new code
     *
     * @param request ResendCodeRequest containing user's email
     * @return ResponseEntity with success or error message
     */
    @PostMapping(
            value = "/resend",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<MessageResponseRequest> resendVerificationCode(@Valid @RequestBody ResendCodeRequest request) {
        try {
            // Generate and send new verification code
            verificationService.resendVerificationCode(
                    request.getEmail(),
                    VerificationCode.VerificationType.EMAIL_VERIFICATION
            );

            return ResponseEntity.ok(
                    new MessageResponseRequest("Verification code resent. Please check your email.")
            );

        } catch (IllegalStateException e) {
            // Return 400 Bad Request for issues like email not found
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponseRequest(e.getMessage()));
        }
    }

    /**
     * POST /api/auth/login
     * Authenticates user and returns JWT tokens
     *
     * Requirements:
     * - User must have verified email
     * - Valid email and password
     *
     * @param request LoginRequest containing email and password
     * @return ResponseEntity with JWT tokens or verification error
     */
    @PostMapping(
            value = "/login",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Object> login(@Valid @RequestBody LoginRequest request) {
        try {
            // Attempt login - will fail if email not verified
            TokenResponseRequest tokenResponse = authService.login(request);
            return ResponseEntity.ok(tokenResponse);

        } catch (DisabledException e) {
            // User exists but email is not verified
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("verified", false);
            errorResponse.put("email", request.getEmail());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }
    }

    /**
     * POST /api/auth/refresh-token
     * Refreshes expired access token using valid refresh token
     *
     * Security:
     * - Refresh tokens have longer expiration
     * - Allows users to stay authenticated without re-login
     *
     * @param request RefreshTokenRequest containing refresh token
     * @return ResponseEntity with new JWT tokens or error
     */
    @PostMapping(
            value = "/refresh-token",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Object> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            // Generate new access token using refresh token
            TokenResponseRequest tokenResponse = authService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(tokenResponse);

        } catch (InvalidTokenException e) {
            // Return 401 Unauthorized for invalid refresh tokens
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    /**
     * POST /api/auth/forgot-password
     * Initiates password reset process by sending reset code
     *
     * Security Note:
     * - Always returns success message regardless of email existence
     * - Prevents email enumeration attacks
     *
     * @param request ForgotPasswordRequest containing user's email
     * @return ResponseEntity with generic success message
     */
    @PostMapping(
            value = "/forgot-password",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<MessageResponseRequest> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            // Send password reset code (6-digit)
            verificationService.resendVerificationCode(
                    request.getEmail(),
                    VerificationCode.VerificationType.PASSWORD_RESET
            );

            return ResponseEntity.ok(
                    new MessageResponseRequest("Password reset code sent. Please check your email.")
            );

        } catch (Exception e) {
            // Security: Never reveal if email exists or not
            return ResponseEntity.ok(
                    new MessageResponseRequest("If the email exists, a password reset code has been sent.")
            );
        }
    }

    /**
     * POST /api/auth/verify-reset-code
     * Verifies password reset code before allowing password change
     *
     * @param request VerifyEmailRequest containing email and reset code
     * @return ResponseEntity with success or error message
     */
    @PostMapping(
            value = "/verify-reset-code",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<MessageResponseRequest> verifyResetCode(@Valid @RequestBody VerifyEmailRequest request) {
        try {
            // Verify the 6-digit password reset code
            verificationService.verifyCode(
                    request.getEmail(),
                    request.getCode(),
                    VerificationCode.VerificationType.PASSWORD_RESET
            );

            return ResponseEntity.ok(
                    new MessageResponseRequest("Code verified. You can now reset your password.")
            );

        } catch (InvalidTokenException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponseRequest(e.getMessage()));
        }
    }

    /**
     * POST /api/auth/reset-password
     * Resets user password after code verification
     *
     * Flow:
     * 1. Verify reset code is valid
     * 2. Update user password with new one
     * 3. Invalidate used reset code
     *
     * @param request ResetPasswordRequest containing email, code, and new password
     * @return ResponseEntity with success or error message
     */
    @PostMapping(
            value = "/reset-password",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<MessageResponseRequest> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {

        try{
        // Then reset the password
        authService.resetPasswordWithCode(request.getEmail(), request.getNewPassword());

        return ResponseEntity.ok(
                new MessageResponseRequest("Password reset successful. You can now log in with your new password.")
        );
        } catch (Exception e){

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponseRequest(e.getMessage()));
        }


    }

    /**
     * GET /api/auth/check-verification-status
     * Checks if a user's email is verified
     *
     * TODO: Implement proper service method in UserService
     *
     * @param email User's email address as request parameter
     * @return ResponseEntity with verification status or error
     */
    @GetMapping(
            value = "/check-verification-status",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> checkVerificationStatus(@RequestParam String email) {
        try {
            // Placeholder implementation - needs proper service method
            Map<String, Object> status = new HashMap<>();
            status.put("email", email);
            status.put("message", "Check implementation in UserService");

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Unable to check verification status");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}
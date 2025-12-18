package com.example.neighborhelp.service;

import com.example.neighborhelp.entity.User;
import com.example.neighborhelp.entity.VerificationCode;
import com.example.neighborhelp.exception.InvalidTokenException;
import com.example.neighborhelp.exception.ResourceNotFoundException;
import com.example.neighborhelp.repository.UserRepository;
import com.example.neighborhelp.repository.VerificationCodeRepository;
import com.example.neighborhelp.security.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Verification Service for managing email verification and password reset codes.
 *
 * This service handles the complete lifecycle of verification codes including
 * generation, validation, resending, and cleanup. It supports both email
 * verification during registration and password reset functionality.
 */
@Service  // Marks this class as a Spring Service component
@RequiredArgsConstructor  // Lombok - generates constructor with required final fields
public class VerificationService {

    private final VerificationCodeRepository verificationCodeRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * Cryptographically secure random number generator for code generation
     */
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generates a 6-digit verification code.
     *
     * Uses cryptographically secure random number generation to ensure
     * unpredictable codes. The code range is 100000 to 999999 (inclusive).
     *
     * @return 6-digit verification code as String
     *
     * Security: Uses SecureRandom instead of Math.random() for better security
     */
    private String generateVerificationCode() {
        int code = 100000 + random.nextInt(900000); // Range: 100000-999999
        return String.valueOf(code);
    }

    /**
     * Creates and sends a verification code via email.
     *
     * This method handles both email verification and password reset codes.
     * It ensures only one active code per user per type exists at any time.
     *
     * @param user the user to send the code to
     * @param type the type of verification (EMAIL_VERIFICATION or PASSWORD_RESET)
     *
     * Flow:
     * 1. Delete any existing unused codes for this user and type
     * 2. Generate new 6-digit code
     * 3. Save code to database with 1-hour expiration
     * 4. Send appropriate email based on code type
     */
    @Transactional  // Ensures all database operations succeed or fail together
    public void createAndSendVerificationCode(User user, VerificationCode.VerificationType type) {
        // 1. Clean up previous codes for this user and type
        verificationCodeRepository.deleteByUserAndType(user, type);

        // 2. Generate and create new verification code
        String code = generateVerificationCode();
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setCode(code);
        verificationCode.setUser(user);
        verificationCode.setType(type);
        verificationCode.setExpiryDate(LocalDateTime.now().plusHours(1)); // 1-hour expiration
        verificationCode.setUsed(false);

        verificationCodeRepository.save(verificationCode);

        // 3. Send appropriate email based on code type
        if (type == VerificationCode.VerificationType.EMAIL_VERIFICATION) {
            emailService.sendVerificationCodeEmail(user.getEmail(), code, user.getUsername());
        } else if (type == VerificationCode.VerificationType.PASSWORD_RESET) {
            emailService.sendPasswordResetCodeEmail(user.getEmail(), code, user.getUsername());
        }
    }

    /**
     * Verifies a user-provided code against the stored code.
     *
     * Performs comprehensive validation including:
     * - Code existence and usage status
     * - User ownership verification
     * - Code type validation
     * - Expiration check
     *
     * @param email the user's email address
     * @param code the 6-digit code provided by the user
     * @param type the expected code type
     * @throws InvalidTokenException if code validation fails
     * @throws ResourceNotFoundException if user not found
     *
     * Security: Multiple validation layers prevent code misuse
     */
    @Transactional
    public void verifyCode(String email, String code, VerificationCode.VerificationType type)
            throws InvalidTokenException {

        // 1. Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // 2. Find unused verification code
        VerificationCode verificationCode = verificationCodeRepository
                .findByCodeAndUsedFalse(code)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired verification code"));

        // 3. Verify code belongs to the correct user
        if (!verificationCode.getUser().getId().equals(user.getId())) {
            throw new InvalidTokenException("Verification code does not match user");
        }

        // 4. Verify code type matches expected type
        if (verificationCode.getType() != type) {
            throw new InvalidTokenException("Invalid verification code type");
        }

        // 5. Check code expiration
        if (verificationCode.isExpired()) {
            throw new InvalidTokenException("Verification code has expired");
        }

        // 6. Mark code as used to prevent reuse
        verificationCode.setUsed(true);
        verificationCodeRepository.save(verificationCode);

        // 7. If email verification, activate user account
        if (type == VerificationCode.VerificationType.EMAIL_VERIFICATION) {
            user.setVerified(true);
            user.setEnabled(true);
            userRepository.save(user);
        }
    }

    /**
     * Resends a verification code to the user's email.
     *
     * Useful when:
     * - User didn't receive the original code
     * - Code expired before use
     * - User requests a new code
     *
     * @param email the user's email address
     * @param type the type of code to resend
     * @throws ResourceNotFoundException if user not found
     * @throws IllegalStateException if email is already verified (for email verification)
     */
    @Transactional
    public void resendVerificationCode(String email, VerificationCode.VerificationType type) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // Prevent resending email verification if already verified
        if (type == VerificationCode.VerificationType.EMAIL_VERIFICATION && user.getVerified()) {
            throw new IllegalStateException("Email is already verified");
        }

        createAndSendVerificationCode(user, type);
    }

    /**
     * Scheduled task to clean up expired verification codes.
     *
     * Runs every hour to remove expired codes from the database,
     * maintaining performance and preventing database bloat.
     *
     * Cron expression: "0 0 * * * *" = at second 0, minute 0, of every hour
     */
    @Scheduled(cron = "0 0 * * * *")  // Runs at the start of every hour
    @Transactional
    public void cleanupExpiredCodes() {
        verificationCodeRepository.deleteExpiredCodes(LocalDateTime.now());
    }
}
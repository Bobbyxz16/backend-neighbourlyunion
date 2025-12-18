package com.example.neighborhelp.service;

import com.example.neighborhelp.entity.RefreshToken;
import com.example.neighborhelp.entity.User;
import com.example.neighborhelp.repository.RefreshTokenRepository;
import com.example.neighborhelp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing refresh tokens in the authentication system.
 *
 * This service handles the complete lifecycle of refresh tokens including
 * creation, validation, invalidation, and cleanup. Refresh tokens are
 * long-lived tokens that allow users to obtain new access tokens without
 * re-authenticating.
 */
@Service  // Marks this class as a Spring Service component
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    /**
     * Refresh token expiration time in days, configurable via application properties.
     * Default: 7 days if not specified in configuration.
     */
    @Value("${app.jwt.refresh-token.expiry-days:7}")
    private int refreshTokenExpiryDays;

    /**
     * Constructor for dependency injection.
     */
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new refresh token for a user.
     *
     * This method generates a unique token, associates it with the user,
     * and sets an expiration date based on configuration.
     */
    @Transactional  // Ensures the entire operation is atomic
    public RefreshToken createRefreshToken(Long userId) {
        // Optional: Enable for single-session per user (uncomment if needed)
        // refreshTokenRepository.deleteByUserId(userId);

        // Generate unique token string
        String token = generateToken();

        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Calculate expiration date
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(refreshTokenExpiryDays);

        // Create and configure refresh token entity
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setExpiryDate(expiryDate);
        refreshToken.setUser(user);

        // Persist and return the token
        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Generates a unique cryptographically secure token string.
     *
     * Uses UUID random generation and removes hyphens for a compact format.
     *
     * @return unique token string (32 characters)
     */
    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Finds a refresh token by its token value.
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Verifies if a refresh token is valid (exists and not expired).
     */
    public boolean isTokenValid(String token) {
        return findByToken(token)
                .map(refreshToken -> !refreshToken.isExpired())
                .orElse(false);
    }

    /**
     * Invalidates all refresh tokens for a specific user.
     *
     * This is typically used during:
     * - User logout from all devices
     * - Password change for security
     * - Account suspension or deletion
     */
    @Transactional
    public void invalidateAllUserTokens(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    /**
     * Invalidates a specific refresh token.
     *
     * Used for:
     * - Single device logout
     * - Token rotation during refresh
     * - Security revocation of compromised tokens
     *
     * @param token the specific token to invalidate
     */
    @Transactional
    public void invalidateRefreshToken(String token) {
        findByToken(token).ifPresent(refreshTokenRepository::delete);
    }

    /**
     * Manually cleans up expired refresh tokens from the database.
     *
     * This method should be called periodically (e.g., via scheduled task)
     * to remove expired tokens and maintain database performance.
     */
    @Transactional
    public void cleanupExpiredTokens() {
        List<RefreshToken> allTokens = refreshTokenRepository.findAll();
        allTokens.stream()
                .filter(RefreshToken::isExpired)
                .forEach(refreshTokenRepository::delete);
    }
}
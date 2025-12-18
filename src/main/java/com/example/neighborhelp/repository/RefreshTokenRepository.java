package com.example.neighborhelp.repository;

import com.example.neighborhelp.entity.RefreshToken;
import com.example.neighborhelp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; // Opcional, pero recomendado

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for RefreshToken entity operations.
 *
 * This repository provides data access methods for managing refresh tokens
 * used in JWT authentication. It handles token lookup, user-based operations,
 * and cleanup of tokens for security and maintenance purposes.
 *
 * @author NeighborHelp Team
 */
@Repository // Marks this interface as a Spring Data Repository component
// Optional but recommended: Enables exception translation and makes component scanning explicit
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Finds a refresh token by its token string.
     *
     * This method is used during token refresh operations to validate
     * and retrieve the refresh token details from the database.
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Deletes all refresh tokens associated with a specific user.
     *
     * This method is typically used for:
     * - Logging out a user from all devices
     * - Security measures when suspicious activity is detected
     * - Account deletion or disablement
     * - Password change scenarios
     */
    void deleteByUser(User user);

    /**
     * Finds all refresh tokens associated with a specific user.
     *
     * Useful for:
     * - Displaying active sessions to the user
     * - Administrative purposes to monitor user sessions
     * - Debugging and auditing user activity
     */
    List<RefreshToken> findByUser(User user);

    /**
     * Deletes all refresh tokens associated with a specific user ID.
     *
     * This method provides an alternative to deleteByUser that accepts
     * just the user ID instead of the full User entity, which can be
     * more efficient when you only have the ID.
     */
    void deleteByUserId(Long userId);
    // /**
    //  * Finds non-expired refresh tokens for a specific user.
    //  */
    // List<RefreshToken> findByUserAndExpiryDateAfter(User user, LocalDateTime now);
}
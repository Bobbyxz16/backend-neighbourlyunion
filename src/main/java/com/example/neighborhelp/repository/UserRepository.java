package com.example.neighborhelp.repository;

import com.example.neighborhelp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository interface for User entity database operations.
 *
 * Provides methods to query users by various identifiers:
 * - Email (for authentication)
 * - Username (for individuals)
 * - Organization name (for organizations)
 * - Firebase UID (for Firebase auth)
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> , JpaSpecificationExecutor<User> {

    /**
     * Find user by email address
     * Primary method for authentication
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username
     * Used for individual user profiles
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by organization name
     * Used for organization profiles
     */
    Optional<User> findByOrganizationName(String organizationName);

    @Query("SELECT COUNT(u) FROM User u WHERE u.type = :type")
    long countByType(@Param("type") User.UserType type);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") User.Role role);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :date")
    long countByCreatedAtAfter(@Param("date") LocalDateTime date);

    /**
     * Find user by Firebase UID
     * Used for Firebase authentication
     */
    Optional<User> findByFirebaseUid(String firebaseUid);

    /**
     * Check if email exists
     * Used for registration validation
     */
    boolean existsByEmail(String email);

    /**
     * Check if username exists
     * Used for registration and update validation
     */
    boolean existsByUsername(String username);

    /**
     * Check if organization name exists
     * Used for organization registration validation
     */
    boolean existsByOrganizationName(String organizationName);

    /**
     * Check if Firebase UID exists
     * Used for Firebase authentication
     */
    boolean existsByFirebaseUid(String firebaseUid);

    /**
     * Find all enabled users with role USER (excluding ADMIN and MODERATOR)
     * Used for messaging user list
     */
    @Query("SELECT u FROM User u WHERE u.enabled = true AND u.deleted = false AND u.role = 'USER' ORDER BY u.username ASC")
    java.util.List<User> findAllActiveRegularUsers();

}
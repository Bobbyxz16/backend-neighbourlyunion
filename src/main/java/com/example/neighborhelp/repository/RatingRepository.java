package com.example.neighborhelp.repository;

import com.example.neighborhelp.entity.Rating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    // Count ratings given by a specific user
    Long countByUserId(Long userId);

    // Count ratings received on a user's resources
    @Query("SELECT COUNT(r) FROM Rating r WHERE r.resource.user.id = :userId")
    Long countByResourceUserId(@Param("userId") Long userId);

    // Get rating count for a resource
    @Query("SELECT COUNT(r) FROM Rating r WHERE r.resource.id = :resourceId")
    Integer getRatingCountByResourceId(@Param("resourceId") Long resourceId);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.resource.id = :resourceId")
    Long countByResourceId(@Param("resourceId") Long resourceId);

    // Average rating given by a user
    @Query("SELECT AVG(r.rating) FROM Rating r WHERE r.user.id = :userId")
    Double getAverageRatingGivenByUser(@Param("userId") Long userId);

    // Average rating received on user's resources
    @Query("SELECT AVG(r.rating) FROM Rating r WHERE r.resource.user.id = :userId")
    Double getAverageRatingByResourceId(@Param("userId") Long userId);

    // Sum helpful votes on user's ratings
    //@Query("SELECT SUM(r.helpful_count) FROM Rating r WHERE r.user.id = :userId")
   // Long sumHelpfulVotesByUserId(@Param("userId") Long userId);

    // Find last rating date by user
    @Query("SELECT MAX(r.createdAt) FROM Rating r WHERE r.user.id = :userId")
    LocalDateTime findLastRatingDateByUserId(@Param("userId") Long userId);



    // Find ratings by resource ID
    List<Rating> findByResourceId(Long resourceId);

    // Find rating by user and resource
    Optional<Rating> findByUserIdAndResourceId(Long userId, Long resourceId);

    // Find ratings by user ID
    List<Rating> findByUserId(Long userId);

    // Find ratings by resource ID with pagination
    Page<Rating> findByResourceId(Long resourceId, Pageable pageable);

    // Find ratings by resource name with pagination
    @Query("SELECT r FROM Rating r WHERE r.resource.slug = :resourceSlug")
    Page<Rating> findByResourceSlug(@Param("resourceSlug") String resourceSlug, Pageable pageable);

    // Find rating by user and resource slug
    @Query("SELECT r FROM Rating r WHERE r.user.id = :userId AND r.resource.slug = :resourceSlug")
    Optional<Rating> findByUserIdAndResourceSlug(@Param("userId") Long userId, @Param("resourceSlug") String resourceSlug);

    // Calculate average rating for a resource
    @Query("SELECT AVG(r.rating) FROM Rating r WHERE r.resource.id = :resourceId")
    Double calculateAverageRatingByResourceId(@Param("resourceId") Long resourceId);

    // Calculate average rating for a resource by slug
    @Query("SELECT AVG(r.rating) FROM Rating r WHERE r.resource.slug = :resourceSlug")
    Double calculateAverageRatingByResourceSlug(@Param("resourceSlug") String resourceSlug);

    // Count ratings for a resource by slug
    @Query("SELECT COUNT(r) FROM Rating r WHERE r.resource.slug = :resourceSlug")
    Long countByResourceSlug(@Param("resourceSlug") String resourceSlug);

    @Query("SELECT AVG(r.rating) FROM Rating r")
    Double getGlobalAverageRating();

    // Find all ratings by a user
    Page<Rating> findByUserId(Long userId, Pageable pageable);

    // Check if user has rated a specific resource
    boolean existsByUserIdAndResourceId(Long userId, Long resourceId);

    // Count ratings by score for a resource by slug
    @Query("SELECT COUNT(r) FROM Rating r WHERE r.resource.slug = :resourceSlug AND r.rating = :score")
    Long countByResourceSlugAndScore(@Param("resourceSlug") String resourceSlug, @Param("score") Integer score);

    // Get rating distribution for a resource by slug
    @Query("SELECT r.rating, COUNT(r) FROM Rating r WHERE r.resource.slug = :resourceSlug GROUP BY r.rating ORDER BY r.rating DESC")
    List<Object[]> getRatingDistributionByResourceSlug(@Param("resourceSlug") String resourceSlug);

    // En RatingRepository
    @Query("SELECT r FROM Rating r WHERE r.resource.slug = :resourceSlug AND r.user.id = :userId")
    Optional<Rating> findByResourceSlugAndUserId(@Param("resourceSlug") String resourceSlug, @Param("userId") Long userId);

    @Query("SELECT r FROM Rating r WHERE r.resource.slug = :resourceSlug")
    List<Rating> findAllByResourceSlug(@Param("resourceSlug") String resourceSlug);

    /**
     * Calcula el promedio de calificaciones para un recurso específico
     */
    @Query("SELECT AVG(r.rating) FROM Rating r WHERE r.resource.id = :resourceId")
    Double findAverageRatingByResourceId(@Param("resourceId") Long resourceId);



}
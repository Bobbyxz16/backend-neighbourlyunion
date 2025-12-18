package com.example.neighborhelp.repository;

import com.example.neighborhelp.entity.Category;
import com.example.neighborhelp.entity.Resource;
import com.example.neighborhelp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Resource entity
 *
 * Provides CRUD operations and custom queries for resources.
 * Includes dynamic search support using Specifications.
 *
 * @author NeighborHelp
 */
@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long>, JpaSpecificationExecutor<Resource> {

    /** Find by unique slug (used in public routes) */
    Optional<Resource> findBySlug(String slug);

    /** Find all resources created by a specific user */
    Page<Resource> findByUser(User user, Pageable pageable);

    /** Find all ACTIVE resources by a specific user */
    Page<Resource> findByUserAndStatus(User user, Resource.ResourceStatus status, Pageable pageable);

    /** Find all resources with specific status (e.g., PENDING, ACTIVE) */
    Page<Resource> findByStatus(Resource.ResourceStatus status, Pageable pageable);

    List<Resource> findByStatus(Resource.ResourceStatus status);

    @Query("SELECT r.location.city, COUNT(r) FROM Resource r GROUP BY r.location.city")
    List<Object[]> countResourcesByCity();

    @Query("SELECT COUNT(r) FROM Resource r WHERE r.status = :status")
    long countByStatus(@Param("status") Resource.ResourceStatus status);

    @Query("SELECT COUNT(r) FROM Resource r WHERE r.createdAt >= :date")
    long countByCreatedAtAfter(@Param("date") LocalDateTime date);

    @Query("SELECT COUNT(r) FROM Resource r WHERE r.status = :status AND r.updatedAt >= :date")
    long countByStatusAndUpdatedAtAfter(@Param("status") Resource.ResourceStatus status, @Param("date") LocalDateTime date);

    @Query("SELECT SUM(r.viewsCount) FROM Resource r")
    Long sumAllViewsCounts();


    /** Count total resources created by a user */
    @Query("SELECT COUNT(r) FROM Resource r WHERE r.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    /** Count resources by user and status */
    @Query("SELECT COUNT(r) FROM Resource r WHERE r.user.id = :userId AND r.status = :status")
    Long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Resource.ResourceStatus status);

    /** Sum total views across all user’s resources */
    @Query("SELECT COALESCE(SUM(r.viewsCount), 0) FROM Resource r WHERE r.user.id = :userId")
    Long sumViewsByUserId(@Param("userId") Long userId);

    /**
     * Count resources by category
     * @param category The category
     * @return Number of resources in the category
     */
    long countByCategory(Category category);

    /** Get last resource creation timestamp for a user */
    @Query("SELECT MAX(r.createdAt) FROM Resource r WHERE r.user.id = :userId")
    LocalDateTime findLastCreatedDateByUserId(@Param("userId") Long userId);

    /** Find top viewed resources for a user */
    @Query("SELECT r FROM Resource r WHERE r.user.id = :userId ORDER BY r.viewsCount DESC")
    List<Resource> findTopResourcesByViews(@Param("userId") Long userId);

    /** Check if user has any resources */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Resource r WHERE r.user.id = :userId")
    boolean existsByUserId(@Param("userId") Long userId);
}

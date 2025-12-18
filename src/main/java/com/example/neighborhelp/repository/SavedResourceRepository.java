package com.example.neighborhelp.repository;

import com.example.neighborhelp.entity.Resource;
import com.example.neighborhelp.entity.SavedResource;
import com.example.neighborhelp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SavedResourceRepository extends JpaRepository<SavedResource, Long> {

    /**
     * Check if a user has saved a specific resource
     */
    boolean existsByUserAndResource(User user, Resource resource);

    /**
     * Find a saved resource by user and resource
     */
    Optional<SavedResource> findByUserAndResource(User user, Resource resource);


    /**
     * Get all saved resources for a user
     */
    Page<SavedResource> findByUser(User user, Pageable pageable);

    /**
     * Count saved resources for a user
     */
    long countByUser(User user);

    /**
     * Delete saved resource by user and resource
     */
    void deleteByUserAndResource(User user, Resource resource);

    /**
     * Check if resource is saved by user ID and resource ID
     */
    @Query("SELECT CASE WHEN COUNT(sr) > 0 THEN true ELSE false END " +
            "FROM SavedResource sr " +
            "WHERE sr.user.id = :userId AND sr.resource.id = :resourceId")
    boolean isResourceSavedByUser(@Param("userId") Long userId, @Param("resourceId") Long resourceId);

    boolean existsByUserIdAndResourceId(Long userId, Long resourceId);
}
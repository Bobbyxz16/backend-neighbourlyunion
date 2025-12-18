package com.example.neighborhelp.repository;

import com.example.neighborhelp.entity.Resource;
import com.example.neighborhelp.entity.ResourceView;
import com.example.neighborhelp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface ResourceViewRepository extends JpaRepository<ResourceView, Long> {

    /**
     * Check if a user has viewed a resource
     */
    boolean existsByUserAndResource(User user, Resource resource);

    /**
     * Check if an IP has viewed a resource (for anonymous users)
     */
    boolean existsByIpHashAndResource(String ipHash, Resource resource);

    /**
     * Find view record by user and resource
     */
    Optional<ResourceView> findByUserAndResource(User user, Resource resource);

    /**
     * Find view record by IP and resource (for anonymous users)
     */
    Optional<ResourceView> findByIpHashAndResource(String ipHash, Resource resource);

    /**
     * Count unique views (unique users + unique IPs) for a resource
     */
    @Query("SELECT COUNT(rv) FROM ResourceView rv WHERE rv.resource.id = :resourceId")
    Long countUniqueViewsByResourceId(@Param("resourceId") Long resourceId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ResourceView rv WHERE rv.resource.id = :resourceId")
    void deleteAllByResourceId(@Param("resourceId") Long resourceId);

    /**
     * Count total view instances (sum of all view_count) for a resource
     */
    @Query("SELECT COALESCE(SUM(rv.viewCount), 0) FROM ResourceView rv WHERE rv.resource.id = :resourceId")
    Long countTotalViewsByResourceId(@Param("resourceId") Long resourceId);
}
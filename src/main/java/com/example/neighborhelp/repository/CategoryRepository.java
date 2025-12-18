package com.example.neighborhelp.repository;

import com.example.neighborhelp.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Category entity
 *
 * Now includes method to find by name (case-insensitive)
 * for auto-resolution when users create resources
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Find category by name (case-insensitive)
     *
     * @param name category name to search
     * @return Optional containing the category if found
     */
    Optional<Category> findByNameIgnoreCase(String name);

    /**
     * Check if category exists by name (case-insensitive)
     *
     * @param name category name to check
     * @return true if exists, false otherwise
     */
    boolean existsByNameIgnoreCase(String name);
}
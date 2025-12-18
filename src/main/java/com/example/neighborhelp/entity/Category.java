package com.example.neighborhelp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Category entity - Represents resource categories for organizing community resources
 *
 * Categories help users browse and filter resources by type (e.g., Tools, Services, Food).
 * Each resource belongs to one category for better organization and discovery.
 *
 * @author NeighborHelp Team
 */
@Entity
@Table(
        name = "categories",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "name")
        },
        indexes = {
                @Index(name = "idx_category_name", columnList = "name")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Category name (e.g., "Tools", "Services", "Food", "Clothing")
     * Must be unique across all categories
     */
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    /**
     * Optional description explaining what types of resources belong to this category
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Timestamp when category was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when category was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Resources belonging to this category
     * One-to-many relationship with Resource entity
     */
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Resource> resources = new ArrayList<>();

    /**
     * Validates category name before persistence
     */
    @PrePersist
    @PreUpdate
    private void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty");
        }
        if (name.length() > 50) {
            throw new IllegalArgumentException("Category name cannot exceed 50 characters");
        }
    }

    /**
     * Adds a resource to this category and establishes bidirectional relationship
     *
     * @param resource The resource to add to this category
     */
    public void addResource(Resource resource) {
        resources.add(resource);
        resource.setCategory(this);
    }

    /**
     * Removes a resource from this category
     *
     * @param resource The resource to remove from this category
     */
    public void removeResource(Resource resource) {
        resources.remove(resource);
        resource.setCategory(null);
    }

    /**
     * Returns the number of resources in this category
     *
     * @return Count of resources in this category
     */
    public int getResourceCount() {
        return resources != null ? resources.size() : 0;
    }

    /**
     * Checks if category has any resources
     *
     * @return true if category has resources, false otherwise
     */
    public boolean hasResources() {
        return resources != null && !resources.isEmpty();
    }
}
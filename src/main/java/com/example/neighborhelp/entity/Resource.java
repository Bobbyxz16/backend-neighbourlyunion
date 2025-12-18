package com.example.neighborhelp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Enhanced Resource entity with embedded location
 *
 * No longer depends on separate Location table.
 * All location data is stored directly in the resource.
 */
@Entity
@Table(name = "resources")
@SQLDelete(sql = "UPDATE resources SET deleted = true, deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted = false")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(unique = true, nullable = false, length = 255)
    private String slug;

    // ========== CATEGORY ==========

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // ========== EMBEDDED LOCATION ==========

    @Embedded
    private ResourceLocation location;

    // ========== CONTACT & AVAILABILITY ==========

    @Column(length = 500)
    private String contactInfo;

    @Column(length = 500)
    private String availability;

    // ========== COST ==========

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CostType cost;

    // ========== STATUS ==========

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceStatus status = ResourceStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // ========== ADDITIONAL DETAILS ==========

    @Column(length = 500)
    private String requirements;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date deletedAt;

    @Column(name = "additional_notes", columnDefinition = "TEXT")
    private String additionalNotes;

    private Integer capacity;

    @Column(name = "wheelchair_accessible")
    private Boolean wheelchairAccessible;

    @Column(length = 200)
    private String languages;

    @Column(name = "target_audience", length = 500)
    private String targetAudience;

    // ========== MEDIA ==========

    @ElementCollection
    @CollectionTable(name = "resource_images", joinColumns = @JoinColumn(name = "resource_id"))
    @Column(name = "image_url", length = 500)
    private List<String> imageUrl = new ArrayList<>();


    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    // ========== METRICS ==========

    @Column(name = "views_count", nullable = false)
    private Integer viewsCount = 0;

    // ========== RELATIONSHIPS ==========

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ========== TIMESTAMPS ==========

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ========== ENUMS ==========

    public enum CostType {
        FREE,
        LOW_COST,
        AFFORDABLE,
        VARIES
    }

    public enum ResourceStatus {
        ACTIVE,
        INACTIVE,
        PENDING,
        REJECTED
    }

    // ========== UTILITY METHODS ==========

    /**
     * Generate URL-friendly slug from title
     */
    public void generateSlug() {
        if (this.title != null) {
            this.slug = this.title
                    .toLowerCase()
                    .replaceAll("[áàäâ]", "a")
                    .replaceAll("[éèëê]", "e")
                    .replaceAll("[íìïî]", "i")
                    .replaceAll("[óòöô]", "o")
                    .replaceAll("[úùüû]", "u")
                    .replaceAll("ñ", "n")
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("^-|-$", "");

            if (this.slug.length() > 120) {
                this.slug = this.slug.substring(0, 120);
            }
        }
    }

    /**
     * Get full address as single string
     */
    public String getFullAddress() {
        if (location == null) return null;
        return location.getFullAddress();
    }

    /**
     * Check if resource is publicly visible
     */
    public boolean isPubliclyVisible() {
        return status == ResourceStatus.ACTIVE;
    }

    /**
     * Increment view count
     */
    public void incrementViews() {
        this.viewsCount = (this.viewsCount == null ? 0 : this.viewsCount) + 1;
    }
}
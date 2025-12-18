package com.example.neighborhelp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Rating entity - Represents user ratings and reviews for resources
 *
 * Users can rate resources they've used and provide feedback.
 * Implements unique constraint to prevent multiple ratings per user-resource pair.
 * Includes helpful voting system for community moderation.
 *
 * @author NeighborHelp Team
 */
@Entity
@Table(
        name = "ratings",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "resource_id"})
        },
        indexes = {
                @Index(name = "idx_rating_user", columnList = "user_id"),
                @Index(name = "idx_rating_resource", columnList = "resource_id"),
                @Index(name = "idx_rating_score", columnList = "score"),
                @Index(name = "idx_rating_created", columnList = "created_at")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User who created this rating
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Resource being rated
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    /**
     * Rating score (1-5 stars)
     * 1 = Poor, 2 = Fair, 3 = Good, 4 = Very Good, 5 = Excellent
     */
    @Column(name = "score", nullable = false)
    private Integer rating;

    /**
     * Optional comment/review text providing detailed feedback
     */
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    /**
     * Number of users who found this rating helpful
     * Used for sorting and highlighting useful reviews
     */
    @Builder.Default
    @Column(name = "helpful_count", nullable = false)
    private Integer helpfulCount = 0;

    /**
     * Timestamp when rating was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when rating was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Validates rating score is within valid range (1-5)
     */
    @PrePersist
    @PreUpdate
    private void validateScore() {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating score must be between 1 and 5");
        }
    }

    /**
     * Increments helpful count when users mark this rating as helpful
     */
    public void markAsHelpful() {
        this.helpfulCount++;
    }

    /**
     * Checks if this rating has a comment
     */
    public boolean hasComment() {
        return comment != null && !comment.trim().isEmpty();
    }

    /**
     * Returns a string representation of star rating
     */
    public String getStarRating() {
        return "★".repeat(rating) + "☆".repeat(5 - rating);
    }
}
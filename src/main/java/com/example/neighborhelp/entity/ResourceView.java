
package com.example.neighborhelp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Tracks unique views per resource per user
 * Ensures each user is counted only once regardless of how many times they view the resource
 */
@Entity
@Table(name = "resource_views", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "resource_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true) // Nullable para usuarios anónimos
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    /**
     * IP address for tracking anonymous views (hashed for privacy)
     * Used when user_id is null
     */
    @Column(name = "ip_hash", length = 64)
    private String ipHash;

    /**
     * First time this user/IP viewed this resource
     */
    @CreationTimestamp
    @Column(name = "first_viewed_at", nullable = false, updatable = false)
    private LocalDateTime firstViewedAt;

    /**
     * Last time this user/IP viewed this resource
     */
    @Column(name = "last_viewed_at", nullable = false)
    private LocalDateTime lastViewedAt;

    /**
     * Number of times this user/IP has viewed this resource
     */
    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 1;

    @PrePersist
    protected void onCreate() {
        if (lastViewedAt == null) {
            lastViewedAt = LocalDateTime.now();
        }
    }
}
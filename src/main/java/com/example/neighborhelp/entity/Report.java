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
 * Entity representing a user report about a resource
 */
@Entity
@Table(name = "reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id") // Add this
    private Rating comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.UNDER_REVIEW;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String resolutionNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enums
    public enum ReportType {
        INACCURATE_INFORMATION,
        INAPPROPRIATE_CONTENT,
        OUTDATED_INFORMATION,
        SPAM_CONTENT,
        PRIVACY_VIOLATION,
        DISCRIMINATION,
        FRAUD,
        HARASSMENT,           // For offensive/harassing comments
        HATE_SPEECH,          // For hate speech
        OFFENSIVE_CONTENT,    // For offensive language
        MISLEADING_INFO,      // For misleading information in comments
        PERSONAL_ATTACK,
        OTHER
    }

    public enum ReportSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum ReportStatus {
        UNDER_REVIEW,
        ESCALATED,
        RESOLVED,
        DISMISSED
    }

    // Add validation method
    public void validate() {
        if (resource == null && comment == null) {
            throw new IllegalArgumentException("Report must target either a resource or a comment");
        }
        if (resource != null && comment != null) {
            throw new IllegalArgumentException("Report cannot target both resource and comment");
        }
    }
}
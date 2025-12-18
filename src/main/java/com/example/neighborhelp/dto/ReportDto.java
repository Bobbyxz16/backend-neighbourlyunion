package com.example.neighborhelp.dto;

import com.example.neighborhelp.entity.Report;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

public class ReportDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateReportRequest {
        private Long resourceId;
        private Long commentId;

        @NotNull(message = "Report type is required")
        private Report.ReportType reportType; // Use entity enum directly

        @NotNull(message = "Severity is required")
        private Report.ReportSeverity severity; // Use entity enum directly

        @NotBlank(message = "Description is required")
        @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportResponse {
        private Long id;
        private Long resourceId;
        private String resourceTitle;
        private Long commentId;           // New field
        private String commentText;       // New field
        private Long commentAuthorId;     // New field
        private String commentAuthorName; // New field
        private Long reporterId;
        private String reporterName;
        private String reporterEmail;
        private Report.ReportType reportType;
        private Report.ReportSeverity severity;
        private Report.ReportStatus status;
        private String description;
        private String resolutionNotes;
        private Long resolvedById;
        private String resolvedByName;
        private LocalDateTime resolvedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String reportTarget; // "RESOURCE" or "COMMENT"
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResolveReportRequest {
        @NotBlank(message = "Resolution notes are required")
        @Size(min = 10, max = 2000, message = "Resolution notes must be between 10 and 2000 characters")
        private String resolutionNotes;
    }

    // Enums (you might have these in your entity, but include in DTO if needed)
    public enum ReportType {
        INAPPROPRIATE_CONTENT,
        SPAM,
        SCAM,
        FAKE_INFORMATION,
        OUTDATED,
        INAPPROPRIATE_CONTACT,
        HARASSMENT,
        HATE_SPEECH,
        OFFENSIVE_CONTENT,
        MISLEADING_INFO,
        PERSONAL_ATTACK,
        OTHER
    }

    public enum ReportSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum ReportStatus {
        UNDER_REVIEW, RESOLVED, ESCALATED
    }
}
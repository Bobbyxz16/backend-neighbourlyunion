package com.example.neighborhelp.service;

import com.example.neighborhelp.dto.ReportDto;
import com.example.neighborhelp.entity.Report;
import com.example.neighborhelp.entity.Resource;
import com.example.neighborhelp.entity.Rating; // Add this import
import com.example.neighborhelp.entity.User;
import com.example.neighborhelp.exception.ResourceNotFoundException;
import com.example.neighborhelp.repository.ReportRepository;
import com.example.neighborhelp.repository.ResourceRepository;
import com.example.neighborhelp.repository.RatingRepository; // Add this import
import com.example.neighborhelp.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final ResourceRepository resourceRepository;
    private final RatingRepository ratingRepository; // Add this
    private final UserRepository userRepository;

    @Transactional
    public ReportDto.ReportResponse createReport(ReportDto.CreateReportRequest request, Long reporterId) {
        log.info("Creating report by user {}", reporterId);

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate that either resourceId or commentId is provided (XOR)
        if ((request.getResourceId() == null && request.getCommentId() == null) ||
                (request.getResourceId() != null && request.getCommentId() != null)) {
            throw new IllegalArgumentException("You must provide either resourceId or commentId, but not both");
        }

        Report report = Report.builder()
                .reporter(reporter)
                .reportType(request.getReportType())
                .severity(request.getSeverity())
                .description(request.getDescription())
                .status(Report.ReportStatus.UNDER_REVIEW)
                .build();

        // Handle resource report
        if (request.getResourceId() != null) {
            Resource resource = resourceRepository.findById(request.getResourceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

            // Check if reporter is the owner of the resource
            if (resource.getUser().getId().equals(reporterId)) {
                throw new IllegalArgumentException("You cannot report your own resource");
            }

            report.setResource(resource);
            log.info("Creating report for resource {} by user {}", request.getResourceId(), reporterId);
        }

        // Handle comment report
        if (request.getCommentId() != null) {
            Rating comment = ratingRepository.findById(request.getCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

            // Check if reporter is the author of the comment
            if (comment.getUser().getId().equals(reporterId)) {
                throw new IllegalArgumentException("You cannot report your own comment");
            }

            // Set both comment and its associated resource for context
            report.setComment(comment);
            report.setResource(comment.getResource()); // Link to the resource for context
            log.info("Creating report for comment {} by user {}", request.getCommentId(), reporterId);
        }

        Report savedReport = reportRepository.save(report);
        return mapToResponse(savedReport);
    }

    @Transactional(readOnly = true)
    public Page<ReportDto.ReportResponse> getReports(
            String status, String severity, String type,
            String targetType, // Add this parameter: "RESOURCE" or "COMMENT"
            Pageable pageable) {

        Specification<Report> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null && !status.trim().isEmpty()) {
                try {
                    Report.ReportStatus reportStatus = Report.ReportStatus.valueOf(status.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("status"), reportStatus));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid status filter: {}", status);
                }
            }

            if (severity != null && !severity.trim().isEmpty()) {
                try {
                    Report.ReportSeverity reportSeverity = Report.ReportSeverity.valueOf(severity.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("severity"), reportSeverity));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid severity filter: {}", severity);
                }
            }

            if (type != null && !type.trim().isEmpty()) {
                try {
                    Report.ReportType reportType = Report.ReportType.valueOf(type.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("reportType"), reportType));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid type filter: {}", type);
                }
            }

            // Add target type filter (RESOURCE or COMMENT)
            if (targetType != null && !targetType.trim().isEmpty()) {
                if (targetType.equalsIgnoreCase("RESOURCE")) {
                    predicates.add(criteriaBuilder.isNotNull(root.get("resource")));
                    predicates.add(criteriaBuilder.isNull(root.get("comment")));
                } else if (targetType.equalsIgnoreCase("COMMENT")) {
                    predicates.add(criteriaBuilder.isNotNull(root.get("comment")));
                } else {
                    log.warn("Invalid targetType filter: {}", targetType);
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return reportRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ReportDto.ReportResponse getReportById(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        return mapToResponse(report);
    }

    @Transactional
    public ReportDto.ReportResponse resolveReport(Long id, ReportDto.ResolveReportRequest request, Long moderatorId) {
        log.info("Resolving report {} by moderator {}", id, moderatorId);

        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        User moderator = userRepository.findById(moderatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Moderator not found"));

        report.setStatus(Report.ReportStatus.RESOLVED);
        report.setResolutionNotes(request.getResolutionNotes());
        report.setResolvedBy(moderator);
        report.setResolvedAt(LocalDateTime.now());

        Report savedReport = reportRepository.save(report);
        return mapToResponse(savedReport);
    }

    @Transactional
    public ReportDto.ReportResponse escalateReport(Long id, Long moderatorId) {
        log.info("Escalating report {} by moderator {}", id, moderatorId);

        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        report.setStatus(Report.ReportStatus.ESCALATED);
        report.setSeverity(Report.ReportSeverity.CRITICAL);

        Report savedReport = reportRepository.save(report);
        return mapToResponse(savedReport);
    }

    @Transactional
    public void deleteReport(Long reportId, Long userId) {
        log.info("Deleting report {} by user {}", reportId, userId);

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + reportId));

        // Verify the user owns this report
        if (!report.getReporter().getId().equals(userId)) {
            throw new SecurityException("You can only delete your own reports");
        }

        reportRepository.delete(report);
        log.info("Successfully deleted report {}", reportId);
    }

    // Helper method to determine report target type
    private String getReportTargetType(Report report) {
        if (report.getComment() != null) {
            return "COMMENT";
        } else if (report.getResource() != null) {
            return "RESOURCE";
        }
        return "UNKNOWN";
    }

    private ReportDto.ReportResponse mapToResponse(Report report) {
        ReportDto.ReportResponse response = ReportDto.ReportResponse.builder()
                .id(report.getId())
                .resourceId(report.getResource() != null ? report.getResource().getId() : null)
                .resourceTitle(report.getResource() != null ? report.getResource().getTitle() : null)
                .reporterId(report.getReporter().getId())
                .reporterName(report.getReporter().getUsername())
                .reporterEmail(report.getReporter().getEmail())
                .reportType(report.getReportType())
                .severity(report.getSeverity())
                .status(report.getStatus())
                .description(report.getDescription())
                .resolutionNotes(report.getResolutionNotes())
                .resolvedById(report.getResolvedBy() != null ? report.getResolvedBy().getId() : null)
                .resolvedByName(report.getResolvedBy() != null ? report.getResolvedBy().getUsername() : null)
                .resolvedAt(report.getResolvedAt())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();

        // Add comment information if this is a comment report
        if (report.getComment() != null) {
            response.setCommentId(report.getComment().getId());
            response.setCommentText(report.getComment().getComment());
            response.setCommentAuthorId(report.getComment().getUser().getId());
            response.setCommentAuthorName(report.getComment().getUser().getUsername());
            response.setReportTarget("COMMENT");
        } else {
            response.setReportTarget("RESOURCE");
        }

        return response;
    }
}
package com.example.neighborhelp.controller;

import com.example.neighborhelp.dto.ReportDto;
import com.example.neighborhelp.entity.User;
import com.example.neighborhelp.repository.UserRepository;
import com.example.neighborhelp.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final UserRepository userRepository;

    /**
     * POST /api/reports
     * Create a new report
     */
    @PostMapping
    public ResponseEntity<ReportDto.ReportResponse> createReport(
            @Valid @RequestBody ReportDto.CreateReportRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        ReportDto.ReportResponse report = reportService.createReport(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    /**
     * GET /api/reports
     * Get all reports (with filters)
     * Auth: ADMIN, MODERATOR
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<Page<ReportDto.ReportResponse>> getReports(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String targetType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<ReportDto.ReportResponse> reports = reportService.getReports(
                status, severity, type, targetType, pageable);
        return ResponseEntity.ok(reports);
    }

    /**
     * GET /api/reports/{id}
     * Get report by ID
     * Auth: ADMIN, MODERATOR
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<ReportDto.ReportResponse> getReportById(@PathVariable Long id) {
        ReportDto.ReportResponse report = reportService.getReportById(id);
        return ResponseEntity.ok(report);
    }

    /**
     * PATCH /api/reports/{id}/resolve
     * Resolve a report
     * Auth: ADMIN, MODERATOR
     */
    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<ReportDto.ReportResponse> resolveReport(
            @PathVariable Long id,
            @Valid @RequestBody ReportDto.ResolveReportRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        ReportDto.ReportResponse report = reportService.resolveReport(
                id, request, currentUser.getId());
        return ResponseEntity.ok(report);
    }

    /**
     * PATCH /api/reports/{id}/escalate
     * Escalate a report
     * Auth: ADMIN, MODERATOR
     */
    @PatchMapping("/{id}/escalate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public ResponseEntity<ReportDto.ReportResponse> escalateReport(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        ReportDto.ReportResponse report = reportService.escalateReport(id, currentUser.getId());
        return ResponseEntity.ok(report);
    }

    /**
     * DELETE /api/reports/{id}
     * Delete a report - Users can only delete their own reports
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        reportService.deleteReport(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
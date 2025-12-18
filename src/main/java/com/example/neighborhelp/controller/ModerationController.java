package com.example.neighborhelp.controller;

import com.example.neighborhelp.dto.ModerationDto;
import com.example.neighborhelp.service.ModerationMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/moderation")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class ModerationController {

    private final ModerationMetricsService moderationMetricsService;

    /**
     * GET /api/moderation/metrics/daily
     * Get daily moderation metrics
     */
    @GetMapping("/metrics/daily")
    public ResponseEntity<ModerationDto.DailyMetricsResponse> getDailyMetrics() {
        ModerationDto.DailyMetricsResponse metrics = moderationMetricsService.getDailyMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * GET /api/moderation/metrics/weekly
     * Get weekly moderation metrics
     */
    @GetMapping("/metrics/weekly")
    public ResponseEntity<ModerationDto.WeeklyMetricsResponse> getWeeklyMetrics() {
        ModerationDto.WeeklyMetricsResponse metrics = moderationMetricsService.getWeeklyMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * GET /api/moderation/metrics/monthly
     * Get monthly moderation metrics
     */
    @GetMapping("/metrics/monthly")
    public ResponseEntity<ModerationDto.MonthlyMetricsResponse> getMonthlyMetrics() {
        ModerationDto.MonthlyMetricsResponse metrics = moderationMetricsService.getMonthlyMetrics();
        return ResponseEntity.ok(metrics);
    }
}
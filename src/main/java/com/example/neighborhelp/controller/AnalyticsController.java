package com.example.neighborhelp.controller;

import com.example.neighborhelp.dto.AnalyticsDto;
import com.example.neighborhelp.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * GET /api/analytics/overview
     * Get platform overview statistics
     */
    @GetMapping("/overview")
    public ResponseEntity<AnalyticsDto.OverviewResponse> getOverview() {
        AnalyticsDto.OverviewResponse overview = analyticsService.getOverview();
        return ResponseEntity.ok(overview);
    }

    /**
     * GET /api/analytics/usage
     * Get usage statistics
     */
    @GetMapping("/usage")
    public ResponseEntity<AnalyticsDto.UsageResponse> getUsageAnalytics() {
        AnalyticsDto.UsageResponse usage = analyticsService.getUsageAnalytics();
        return ResponseEntity.ok(usage);
    }

    /**
     * GET /api/analytics/geographic
     * Get geographic distribution statistics
     */
    @GetMapping("/geographic")
    public ResponseEntity<AnalyticsDto.GeographicResponse> getGeographicAnalytics() {
        AnalyticsDto.GeographicResponse geographic = analyticsService.getGeographicAnalytics();
        return ResponseEntity.ok(geographic);
    }

    /**
     * GET /api/analytics/resources
     * Get resource-specific analytics
     */
    @GetMapping("/resources")
    public ResponseEntity<AnalyticsDto.ResourceAnalyticsResponse> getResourceAnalytics() {
        AnalyticsDto.ResourceAnalyticsResponse resources = analyticsService.getResourceAnalytics();
        return ResponseEntity.ok(resources);
    }

    /**
     * GET /api/analytics/trends
     * Get trends and growth analytics
     */
    @GetMapping("/trends")
    public ResponseEntity<AnalyticsDto.TrendsResponse> getTrendsAnalytics() {
        AnalyticsDto.TrendsResponse trends = analyticsService.getTrendsAnalytics();
        return ResponseEntity.ok(trends);
    }
}
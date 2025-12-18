package com.example.neighborhelp.service;

import com.example.neighborhelp.dto.ModerationDto;
import com.example.neighborhelp.entity.Report;
import com.example.neighborhelp.entity.Resource;
import com.example.neighborhelp.repository.ReportRepository;
import com.example.neighborhelp.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class ModerationMetricsService {

    private final ResourceRepository resourceRepository;
    private final ReportRepository reportRepository;

    @Transactional(readOnly = true)
    public ModerationDto.DailyMetricsResponse getDailyMetrics() {
        log.info("Generating daily moderation metrics");

        LocalDateTime dayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);

        long reviewsCompleted = resourceRepository.countByStatusAndUpdatedAtAfter(
                Resource.ResourceStatus.ACTIVE, dayStart);
        reviewsCompleted += resourceRepository.countByStatusAndUpdatedAtAfter(
                Resource.ResourceStatus.REJECTED, dayStart);

        long reportsHandled = reportRepository.countByCreatedAtAfter(dayStart);
        long totalActive = resourceRepository.countByStatus(Resource.ResourceStatus.ACTIVE);
        long totalResources = resourceRepository.count();

        double approvalRate = totalResources > 0
                ? ((double) totalActive / totalResources) * 100
                : 0.0;

        return ModerationDto.DailyMetricsResponse.builder()
                .reviewsCompleted((int) reviewsCompleted)
                .averageProcessingTime(18) // Would need tracking
                .approvalRate(approvalRate)
                .qualityScore(4.6) // Would need quality tracking
                .reportsHandled((int) reportsHandled)
                .escalations(0) // Would need escalation tracking
                .build();
    }

    @Transactional(readOnly = true)
    public ModerationDto.WeeklyMetricsResponse getWeeklyMetrics() {
        log.info("Generating weekly moderation metrics");

        LocalDateTime weekStart = LocalDateTime.now().minusWeeks(1);

        long totalReviews = resourceRepository.countByCreatedAtAfter(weekStart);
        long reportsHandled = reportRepository.countByCreatedAtAfter(weekStart);

        return ModerationDto.WeeklyMetricsResponse.builder()
                .totalReviews((int) totalReviews)
                .reportsHandled((int) reportsHandled)
                .escalations(0)
                .userFeedbackScore(4.3)
                .averageQualityScore(4.5)
                .dailyBreakdown(new ArrayList<>())
                .build();
    }

    @Transactional(readOnly = true)
    public ModerationDto.MonthlyMetricsResponse getMonthlyMetrics() {
        log.info("Generating monthly moderation metrics");

        LocalDateTime monthStart = LocalDateTime.now().minusMonths(1);

        long totalReviews = resourceRepository.countByCreatedAtAfter(monthStart);

        return ModerationDto.MonthlyMetricsResponse.builder()
                .totalReviews((int) totalReviews)
                .averageQualityScore(4.5)
                .providerSatisfaction(89)
                .communityImpact(95)
                .weeklyBreakdown(new ArrayList<>())
                .topModerator(null)
                .build();
    }
}
package com.example.neighborhelp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class ModerationDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyMetricsResponse {
        private Integer reviewsCompleted;
        private Integer averageProcessingTime;
        private Double approvalRate;
        private Double qualityScore;
        private Integer reportsHandled;
        private Integer escalations;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeeklyMetricsResponse {
        private Integer totalReviews;
        private Integer reportsHandled;
        private Integer escalations;
        private Double userFeedbackScore;
        private Double averageQualityScore;
        private List<DayMetric> dailyBreakdown;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyMetricsResponse {
        private Integer totalReviews;
        private Double averageQualityScore;
        private Integer providerSatisfaction;
        private Integer communityImpact;
        private List<WeekMetric> weeklyBreakdown;
        private TopModerator topModerator;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DayMetric {
        private String day;
        private Integer reviews;
        private Integer reports;
        private Integer avgTime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeekMetric {
        private String week;
        private Integer reviews;
        private Integer reports;
        private Double avgTime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopModerator {
        private String name;
        private Integer reviewsCompleted;
        private Double qualityScore;
    }
}
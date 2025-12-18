package com.example.neighborhelp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class AnalyticsDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OverviewResponse {
        private Long totalResources;
        private Long activeResources;
        private Long totalUsers;
        private Long totalCategories;
        private Long totalRatings;
        private Double averageRating;
        private Long pendingReviews;
        private Long openReports;
        private Map<String, Long> resourcesByStatus;
        private Map<String, Long> usersByType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UsageResponse {
        private Long totalViews;
        private Long totalSearches;
        private Long newUsersThisMonth;
        private Long newResourcesThisMonth;
        private Long activeUsersToday;
        private List<DailyStats> dailyStats;
        private Map<String, Long> popularCategories;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GeographicResponse {
        private List<CityStats> citiesStats;
        private Map<String, Long> resourcesByCity;
        private Map<String, Long> usersByCity;
        private String mostActiveCity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResourceAnalyticsResponse {
        private Long totalResources;
        private Long approvedToday;
        private Long rejectedToday;
        private Double approvalRate;
        private Double averageProcessingTime;
        private List<CategoryStats> categoryBreakdown;
        private List<ResourceTrend> recentTrends;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrendsResponse {
        private List<TrendData> userGrowth;
        private List<TrendData> resourceGrowth;
        private List<TrendData> engagementTrends;
        private Map<String, Double> growthRates;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyStats {
        private LocalDate date;
        private Long views;
        private Long searches;
        private Long newUsers;
        private Long newResources;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CityStats {
        private String city;
        private Long resourceCount;
        private Long userCount;
        private Long totalViews;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CategoryStats {
        private String categoryName;
        private Long resourceCount;
        private Long activeCount;
        private Double averageRating;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResourceTrend {
        private LocalDate date;
        private Long created;
        private Long approved;
        private Long rejected;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrendData {
        private LocalDate date;
        private Long count;
    }
}
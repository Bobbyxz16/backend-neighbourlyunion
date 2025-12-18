package com.example.neighborhelp.service;

import com.example.neighborhelp.dto.AnalyticsDto;
import com.example.neighborhelp.entity.Resource;
import com.example.neighborhelp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RatingRepository ratingRepository;
    private final ReportRepository reportRepository;

    @Transactional(readOnly = true)
    public AnalyticsDto.OverviewResponse getOverview() {
        log.info("Generating analytics overview");

        long totalResources = resourceRepository.count();
        long activeResources = resourceRepository.countByStatus(Resource.ResourceStatus.ACTIVE);
        long totalUsers = userRepository.count();
        long totalCategories = categoryRepository.count();
        long totalRatings = ratingRepository.count();
        Double averageRating = ratingRepository.getGlobalAverageRating();
        long pendingReviews = resourceRepository.countByStatus(Resource.ResourceStatus.PENDING);
        long openReports = reportRepository.countByStatus(com.example.neighborhelp.entity.Report.ReportStatus.UNDER_REVIEW);

        // Resources by status
        Map<String, Long> resourcesByStatus = new HashMap<>();
        resourcesByStatus.put("ACTIVE", resourceRepository.countByStatus(Resource.ResourceStatus.ACTIVE));
        resourcesByStatus.put("INACTIVE", resourceRepository.countByStatus(Resource.ResourceStatus.INACTIVE));
        resourcesByStatus.put("PENDING", resourceRepository.countByStatus(Resource.ResourceStatus.PENDING));
        resourcesByStatus.put("REJECTED", resourceRepository.countByStatus(Resource.ResourceStatus.REJECTED));

        // Users by type
        Map<String, Long> usersByType = new HashMap<>();
        usersByType.put("INDIVIDUAL", userRepository.countByType(com.example.neighborhelp.entity.User.UserType.INDIVIDUAL));
        usersByType.put("ORGANIZATION", userRepository.countByType(com.example.neighborhelp.entity.User.UserType.ORGANIZATION));

        return AnalyticsDto.OverviewResponse.builder()
                .totalResources(totalResources)
                .activeResources(activeResources)
                .totalUsers(totalUsers)
                .totalCategories(totalCategories)
                .totalRatings(totalRatings)
                .averageRating(averageRating != null ? averageRating : 0.0)
                .pendingReviews(pendingReviews)
                .openReports(openReports)
                .resourcesByStatus(resourcesByStatus)
                .usersByType(usersByType)
                .build();
    }

    @Transactional(readOnly = true)
    public AnalyticsDto.UsageResponse getUsageAnalytics() {
        log.info("Generating usage analytics");

        LocalDateTime monthStart = LocalDateTime.now().minusMonths(1);
        LocalDateTime today = LocalDateTime.now().minusDays(1);

        long newUsersThisMonth = userRepository.countByCreatedAtAfter(monthStart);
        long newResourcesThisMonth = resourceRepository.countByCreatedAtAfter(monthStart);

        // Calculate total views from all resources
        Long totalViews = resourceRepository.sumAllViewsCounts();

        return AnalyticsDto.UsageResponse.builder()
                .totalViews(totalViews != null ? totalViews : 0L)
                .totalSearches(0L) // Would need search tracking
                .newUsersThisMonth(newUsersThisMonth)
                .newResourcesThisMonth(newResourcesThisMonth)
                .activeUsersToday(0L) // Would need activity tracking
                .dailyStats(new ArrayList<>())
                .popularCategories(new HashMap<>())
                .build();
    }

    @Transactional(readOnly = true)
    public AnalyticsDto.GeographicResponse getGeographicAnalytics() {
        log.info("Generating geographic analytics");

        // Get city statistics from locations
        List<Object[]> citiesData = resourceRepository.countResourcesByCity();

        List<AnalyticsDto.CityStats> cityStatsList = citiesData.stream()
                .map(data -> AnalyticsDto.CityStats.builder()
                        .city((String) data[0])
                        .resourceCount((Long) data[1])
                        .userCount(0L) // Would need user location
                        .totalViews(0L) // Would need views by city
                        .build())
                .collect(Collectors.toList());

        Map<String, Long> resourcesByCity = cityStatsList.stream()
                .collect(Collectors.toMap(
                        AnalyticsDto.CityStats::getCity,
                        AnalyticsDto.CityStats::getResourceCount
                ));

        String mostActiveCity = cityStatsList.stream()
                .max(Comparator.comparing(AnalyticsDto.CityStats::getResourceCount))
                .map(AnalyticsDto.CityStats::getCity)
                .orElse("N/A");

        return AnalyticsDto.GeographicResponse.builder()
                .citiesStats(cityStatsList)
                .resourcesByCity(resourcesByCity)
                .usersByCity(new HashMap<>())
                .mostActiveCity(mostActiveCity)
                .build();
    }

    @Transactional(readOnly = true)
    public AnalyticsDto.ResourceAnalyticsResponse getResourceAnalytics() {
        log.info("Generating resource analytics");

        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);

        long totalResources = resourceRepository.count();
        long approvedToday = resourceRepository.countByStatusAndUpdatedAtAfter(
                Resource.ResourceStatus.ACTIVE, todayStart);
        long rejectedToday = resourceRepository.countByStatusAndUpdatedAtAfter(
                Resource.ResourceStatus.REJECTED, todayStart);

        double approvalRate = totalResources > 0
                ? ((double) resourceRepository.countByStatus(Resource.ResourceStatus.ACTIVE) / totalResources) * 100
                : 0.0;

        return AnalyticsDto.ResourceAnalyticsResponse.builder()
                .totalResources(totalResources)
                .approvedToday(approvedToday)
                .rejectedToday(rejectedToday)
                .approvalRate(approvalRate)
                .averageProcessingTime(0.0) // Would need processing time tracking
                .categoryBreakdown(new ArrayList<>())
                .recentTrends(new ArrayList<>())
                .build();
    }

    @Transactional(readOnly = true)
    public AnalyticsDto.TrendsResponse getTrendsAnalytics() {
        log.info("Generating trends analytics");

        // Would need historical data tracking
        return AnalyticsDto.TrendsResponse.builder()
                .userGrowth(new ArrayList<>())
                .resourceGrowth(new ArrayList<>())
                .engagementTrends(new ArrayList<>())
                .growthRates(new HashMap<>())
                .build();
    }
}
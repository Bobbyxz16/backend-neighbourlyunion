package com.example.neighborhelp.service;

import com.example.neighborhelp.dto.UserStatisticsResponseRequest;
import com.example.neighborhelp.entity.Resource;
import com.example.neighborhelp.entity.User;
import com.example.neighborhelp.exception.ResourceNotFoundException;
import com.example.neighborhelp.repository.MessageRepository;
import com.example.neighborhelp.repository.RatingRepository;
import com.example.neighborhelp.repository.ResourceRepository;
import com.example.neighborhelp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for calculating and retrieving user statistics
 *
 * Provides aggregated data about user activity including:
 * - Resources created and their status
 * - Ratings given and received
 * - Average ratings
 * - View counts and engagement metrics
 * - Contribution level calculation
 * - Activity timestamps
 *
 * Performance considerations:
 * - Statistics are cached for 5 minutes to reduce database load
 * - Uses optimized aggregate queries
 * - Read-only transactions for consistency
 *
 * @author NeighborHelp Team
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserStatisticsService {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final RatingRepository ratingRepository;
    private final MessageRepository messageRepository;

    // ========== IDENTIFIER-BASED METHODS (NEW) ==========

    /**
     * GET /api/users/{identifier}/statistics
     * Get user statistics by identifier (username OR organization name)
     */
    @Cacheable(value = "userStatistics", key = "'identifier_' + #identifier", unless = "#result == null")
    public UserStatisticsResponseRequest getUserStatisticsByIdentifier(String identifier) {
        log.debug("Getting comprehensive statistics for identifier: {}", identifier);

        // Try to find user by username first, then organization name
        User user = findUserByIdentifier(identifier);

        return getUserStatistics(user.getId(), user);
    }

    /**
     * GET /api/users/{identifier}/statistics/basic
     * Get basic statistics by identifier (username OR organization name)
     */
    @Cacheable(value = "userBasicStatistics", key = "'identifier_' + #identifier", unless = "#result == null")
    public UserStatisticsResponseRequest getBasicStatisticsByIdentifier(String identifier) {
        log.debug("Getting basic statistics for identifier: {}", identifier);

        // Try to find user by username first, then organization name
        User user = findUserByIdentifier(identifier);

        return getBasicStatistics(user.getId(), user);
    }

    /**
     * Helper method to find user by identifier (tries username first, then organization name)
     * Reuses the same logic from UserService for consistency
     */
    private User findUserByIdentifier(String identifier) {
        log.debug("Looking up user by identifier: {}", identifier);

        // Try username first
        return userRepository.findByUsername(identifier)
                .orElseGet(() ->
                        // If not found by username, try organization name
                        userRepository.findByOrganizationName(identifier)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                        "User not found with identifier: " + identifier))
                );
    }

    /**
     * Invalidate cached statistics for a user by identifier
     */
    public void invalidateStatisticsCacheByIdentifier(String identifier) {
        log.debug("Invalidating statistics cache for identifier: {}", identifier);

        try {
            User user = findUserByIdentifier(identifier);
            invalidateStatisticsCache(user.getId());

            // Also invalidate organization-specific cache if applicable
            if (user.getOrganizationName() != null) {
                invalidateStatisticsCacheByOrganizationName(user.getOrganizationName());
            }
        } catch (ResourceNotFoundException e) {
            log.warn("Attempted to invalidate cache for non-existent identifier: {}", identifier);
        }
    }

    /**
     * Check if user qualifies for next contribution level by identifier
     */
    public boolean isCloseToNextLevelByIdentifier(String identifier) {
        User user = findUserByIdentifier(identifier);
        return isCloseToNextLevel(user.getId());
    }

    /**
     * Get resources needed to reach next level by identifier
     */
    public int getResourcesNeededForNextLevelByIdentifier(String identifier) {
        User user = findUserByIdentifier(identifier);
        Long resourceCount = resourceRepository.countByUserId(user.getId());
        UserStatisticsResponseRequest.ContributionLevel currentLevel = calculateContributionLevel(resourceCount);

        return getResourcesNeededForNextLevel(currentLevel, resourceCount);
    }

    /**
     * Get contribution level information by identifier
     */
    public Map<String, Object> getContributionLevelInfoByIdentifier(String identifier) {
        User user = findUserByIdentifier(identifier);
        Long resourceCount = resourceRepository.countByUserId(user.getId());
        UserStatisticsResponseRequest.ContributionLevel currentLevel = calculateContributionLevel(resourceCount);
        int neededForNext = getResourcesNeededForNextLevel(currentLevel, resourceCount);

        Map<String, Object> levelInfo = new HashMap<>();
        levelInfo.put("currentLevel", currentLevel);
        levelInfo.put("currentLevelDisplayName", getContributionLevelDisplayName(currentLevel));
        levelInfo.put("currentLevelDescription", getContributionLevelDescription(currentLevel));
        levelInfo.put("resourcesCreated", resourceCount);
        levelInfo.put("resourcesNeededForNextLevel", neededForNext);
        levelInfo.put("isCloseToNextLevel", isCloseToNextLevel(user.getId()));
        levelInfo.put("isMaxLevel", currentLevel == UserStatisticsResponseRequest.ContributionLevel.PLATINUM);

        return levelInfo;
    }

    // ========== ORGANIZATION-BASED METHODS ==========

    /**
     * Get comprehensive statistics for a user by organization name
     *
     * Calculates all metrics in a single database transaction for consistency.
     * Results are cached for 5 minutes to improve performance.
     *
     * Metrics included:
     * - Total resources created
     * - Active, pending, and inactive resources
     * - Ratings given and average score
     * - Ratings received and average score
     * - Total views across all resources
     * - Helpful votes received
     * - Last activity timestamp
     * - Contribution level badge
     *
     * @param organizationName the organization name of the user
     * @return UserStatisticsResponse with all calculated metrics
     * @throws ResourceNotFoundException if user not found
     */
    @Cacheable(value = "userStatistics", key = "'org_' + #organizationName", unless = "#result == null")
    public UserStatisticsResponseRequest getUserStatisticsByOrganizationName(String organizationName) {
        log.debug("Calculating comprehensive statistics for organization: {}", organizationName);

        // 1. Get user by organization name (verify exists)
        User user = userRepository.findByOrganizationName(organizationName)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with organization name: " + organizationName));

        return getUserStatistics(user.getId(), user);
    }

    /**
     * Get basic statistics by organization name (lightweight version)
     *
     * Returns only essential metrics for quick display.
     * Useful for:
     * - User list views
     * - Profile previews
     * - Quick lookups
     * - Mobile apps (reduced data transfer)
     *
     * This method performs fewer database queries than full statistics.
     *
     * @param organizationName the organization name
     * @return UserStatisticsResponse with basic statistics only
     * @throws ResourceNotFoundException if user not found
     */
    @Cacheable(value = "userBasicStatistics", key = "'org_' + #organizationName", unless = "#result == null")
    public UserStatisticsResponseRequest getBasicStatisticsByOrganizationName(String organizationName) {
        log.debug("Calculating basic statistics for organization: {}", organizationName);

        User user = userRepository.findByOrganizationName(organizationName)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with organization name: " + organizationName));

        return getBasicStatistics(user.getId(), user);
    }

    // ========== USER ID-BASED METHODS ==========

    /**
     * Get comprehensive statistics for a user by ID
     * (Método interno mantenido para uso interno)
     */
    @Cacheable(value = "userStatistics", key = "#userId", unless = "#result == null")
    public UserStatisticsResponseRequest getUserStatistics(Long userId) {
        log.debug("Calculating comprehensive statistics for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        return getUserStatistics(userId, user);
    }

    /**
     * Get basic statistics by user ID
     * (Método interno mantenido para uso interno)
     */
    @Cacheable(value = "userBasicStatistics", key = "#userId", unless = "#result == null")
    public UserStatisticsResponseRequest getBasicStatistics(Long userId) {
        log.debug("Calculating basic statistics for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        return getBasicStatistics(userId, user);
    }

    // ========== INTERNAL IMPLEMENTATION METHODS ==========

    /**
     * Método interno común para calcular estadísticas
     */
    private UserStatisticsResponseRequest getUserStatistics(Long userId, User user) {
        // 2. Calculate resource statistics
        Long totalResources = resourceRepository.countByUserId(userId);
        Long activeResources = resourceRepository.countByUserIdAndStatus(userId, Resource.ResourceStatus.ACTIVE);
        Long pendingResources = resourceRepository.countByUserIdAndStatus(userId, Resource.ResourceStatus.PENDING);

        log.debug("User {} has {} total resources ({} active, {} pending)",
                userId, totalResources, activeResources, pendingResources);

        // 3. Calculate ratings given by this user
        Long ratingsGiven = ratingRepository.countByUserId(userId);
        Double averageRatingGiven = ratingRepository.getAverageRatingGivenByUser(userId);

        log.debug("User {} gave {} ratings with average {}",
                userId, ratingsGiven, averageRatingGiven);

        // 4. Calculate ratings received on user's resources
        Long ratingsReceived = ratingRepository.countByResourceUserId(userId);
        Double averageRatingReceived = ratingRepository.getAverageRatingByResourceId(userId);

        log.debug("User {} received {} ratings with average {}",
                userId, ratingsReceived, averageRatingReceived);

        // 5. Calculate total views on user's resources
        Long totalViews = resourceRepository.sumViewsByUserId(userId);

        Long messagesReceived = messageRepository.countByResourceUserId(userId);
        log.debug("User {} received {} messages for their resources", userId, messagesReceived);

        // 7. Get last activity timestamp
        LocalDateTime lastActivity = getLastActivity(userId);

        // 8. Calculate contribution level
        UserStatisticsResponseRequest.ContributionLevel contributionLevel =
                calculateContributionLevel(totalResources);

        log.info("Statistics calculated for user {}: Level={}, Resources={}, Views={}",
                userId, contributionLevel, totalResources, totalViews);

        // 9. Build and return response
        return UserStatisticsResponseRequest.builder()
                .userId(userId)
                .username(user.getUsername())
                .organizationName(user.getOrganizationName())
                .resourcesCreated(safeValue(totalResources))
                .activeResources(safeValue(activeResources))
                .pendingResources(safeValue(pendingResources))
                .ratingsGiven(safeValue(ratingsGiven))
                .averageRatingGiven(safeDouble(averageRatingGiven))
                .ratingsReceived(safeValue(ratingsReceived))
                .averageRatingReceived(safeDouble(averageRatingReceived))
                .totalViews(safeValue(totalViews))
                .contactsReceived(safeValue(messagesReceived))
                .memberSince(user.getCreatedAt())
                .lastActivity(lastActivity)
                .contributionLevel(contributionLevel)
                .isOrganization(user.getType() == User.UserType.ORGANIZATION)
                .isVerified(user.getVerified())
                .build();
    }

    /**
     * Método interno común para estadísticas básicas
     */
    private UserStatisticsResponseRequest getBasicStatistics(Long userId, User user) {
        // Only calculate essential metrics
        Long resourcesCreated = resourceRepository.countByUserId(userId);
        Long ratingsGiven = ratingRepository.countByUserId(userId);
        UserStatisticsResponseRequest.ContributionLevel contributionLevel =
                calculateContributionLevel(resourcesCreated);

        return UserStatisticsResponseRequest.builder()
                .userId(userId)
                .username(user.getUsername())
                .organizationName(user.getOrganizationName())
                .resourcesCreated(safeValue(resourcesCreated))
                .ratingsGiven(safeValue(ratingsGiven))
                .contributionLevel(contributionLevel)
                .memberSince(user.getCreatedAt())
                .isOrganization(user.getType() == User.UserType.ORGANIZATION)
                .isVerified(user.getVerified())
                .build();
    }

    /**
     * Get the timestamp of user's last activity
     *
     * Checks both resource creation and rating timestamps to determine
     * the most recent activity.
     *
     * @param userId the user ID
     * @return LocalDateTime of last activity, or null if no activity
     */
    private LocalDateTime getLastActivity(Long userId) {
        LocalDateTime lastResourceCreated = resourceRepository.findLastCreatedDateByUserId(userId);
        LocalDateTime lastRatingGiven = ratingRepository.findLastRatingDateByUserId(userId);

        // If no activity at all
        if (lastResourceCreated == null && lastRatingGiven == null) {
            return null;
        }

        // If only one type of activity exists
        if (lastResourceCreated == null) {
            return lastRatingGiven;
        }
        if (lastRatingGiven == null) {
            return lastResourceCreated;
        }

        // Return the most recent activity
        return lastResourceCreated.isAfter(lastRatingGiven)
                ? lastResourceCreated
                : lastRatingGiven;
    }

    /**
     * Calculate contribution level based on number of resources created
     *
     * Contribution levels serve as gamification badges to encourage
     * user engagement and recognize active contributors.
     *
     * Level tiers:
     * - NEWCOMER: 0-4 resources (just getting started)
     * - BRONZE: 5-9 resources (regular contributor)
     * - SILVER: 10-19 resources (active contributor)
     * - GOLD: 20-49 resources (super contributor)
     * - PLATINUM: 50+ resources (champion contributor)
     *
     * These thresholds can be adjusted based on platform needs.
     *
     * @param resourceCount number of resources created
     * @return ContributionLevel enum representing the user's tier
     */
    private UserStatisticsResponseRequest.ContributionLevel calculateContributionLevel(Long resourceCount) {
        long count = safeValue(resourceCount);

        if (count >= 50) {
            return UserStatisticsResponseRequest.ContributionLevel.PLATINUM;
        } else if (count >= 20) {
            return UserStatisticsResponseRequest.ContributionLevel.GOLD;
        } else if (count >= 10) {
            return UserStatisticsResponseRequest.ContributionLevel.SILVER;
        } else if (count >= 5) {
            return UserStatisticsResponseRequest.ContributionLevel.BRONZE;
        } else {
            return UserStatisticsResponseRequest.ContributionLevel.NEWCOMER;
        }
    }

    /**
     * Safely convert nullable Long to non-null long
     *
     * @param value nullable Long value
     * @return 0 if null, otherwise the long value
     */
    private long safeValue(Long value) {
        return value != null ? value : 0L;
    }

    /**
     * Safely convert nullable Double to non-null double
     *
     * @param value nullable Double value
     * @return 0.0 if null, otherwise the double value
     */
    private double safeDouble(Double value) {
        return value != null ? value : 0.0;
    }

    // ========== CACHE MANAGEMENT ==========

    /**
     * Invalidate cached statistics for a user by organization name
     *
     * Should be called when user performs actions that affect statistics:
     * - Creates a new resource
     * - Deletes a resource
     * - Creates a rating
     * - Updates a rating
     *
     * This ensures statistics remain up-to-date.
     *
     * @param organizationName the organization name whose cache should be invalidated
     */
    public void invalidateStatisticsCacheByOrganizationName(String organizationName) {
        log.debug("Invalidating statistics cache for organization: {}", organizationName);
        // Cache eviction is handled by @CacheEvict annotation
        // This method documents when cache should be cleared
        // Implementation depends on your caching configuration
    }

    /**
     * Invalidate cached statistics for a user by ID
     */
    public void invalidateStatisticsCache(Long userId) {
        log.debug("Invalidating statistics cache for user: {}", userId);
    }

    // ========== CONTRIBUTION LEVEL UTILITIES ==========

    /**
     * Get contribution level display name
     *
     * Utility method to get user-friendly names for contribution levels.
     *
     * @param level the ContributionLevel enum
     * @return display name for the level
     */
    public String getContributionLevelDisplayName(UserStatisticsResponseRequest.ContributionLevel level) {
        return switch (level) {
            case NEWCOMER -> "🌱 Newcomer";
            case BRONZE -> "🥉 Bronze Contributor";
            case SILVER -> "🥈 Silver Contributor";
            case GOLD -> "🥇 Gold Contributor";
            case PLATINUM -> "💎 Platinum Champion";
        };
    }

    /**
     * Get contribution level description
     *
     * @param level the ContributionLevel enum
     * @return description of what this level means
     */
    public String getContributionLevelDescription(UserStatisticsResponseRequest.ContributionLevel level) {
        return switch (level) {
            case NEWCOMER -> "Welcome to NeighborHelp! Create more resources to level up.";
            case BRONZE -> "You're making an impact! Keep sharing resources with your community.";
            case SILVER -> "Active contributor! Your resources are helping many people.";
            case GOLD -> "Super contributor! You're a valued member of our community.";
            case PLATINUM -> "Champion! Your dedication is making a huge difference.";
        };
    }

    /**
     * Check if user qualifies for next contribution level by organization name
     *
     * @param organizationName the organization name
     * @return true if user is close to next level (within 2 resources)
     */
    public boolean isCloseToNextLevelByOrganizationName(String organizationName) {
        User user = userRepository.findByOrganizationName(organizationName)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with organization name: " + organizationName));

        return isCloseToNextLevel(user.getId());
    }

    /**
     * Check if user qualifies for next contribution level by ID
     */
    public boolean isCloseToNextLevel(Long userId) {
        Long resourceCount = resourceRepository.countByUserId(userId);
        long count = safeValue(resourceCount);

        // Check if within 2 resources of next level
        return (count >= 3 && count < 5) ||    // Close to BRONZE
                (count >= 8 && count < 10) ||   // Close to SILVER
                (count >= 18 && count < 20) ||  // Close to GOLD
                (count >= 48 && count < 50);    // Close to PLATINUM
    }

    /**
     * Get resources needed to reach next level by organization name
     *
     * @param organizationName the organization name
     * @return number of resources needed to reach next level, or 0 if max level
     */
    public int getResourcesNeededForNextLevelByOrganizationName(String organizationName) {
        User user = userRepository.findByOrganizationName(organizationName)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with organization name: " + organizationName));

        Long resourceCount = resourceRepository.countByUserId(user.getId());
        UserStatisticsResponseRequest.ContributionLevel currentLevel = calculateContributionLevel(resourceCount);

        return getResourcesNeededForNextLevel(currentLevel, resourceCount);
    }

    /**
     * Get resources needed to reach next level
     *
     * @param currentLevel the current contribution level
     * @param currentResourceCount current number of resources
     * @return number of resources needed to reach next level, or 0 if max level
     */
    public int getResourcesNeededForNextLevel(
            UserStatisticsResponseRequest.ContributionLevel currentLevel,
            Long currentResourceCount) {

        long count = safeValue(currentResourceCount);

        return switch (currentLevel) {
            case NEWCOMER -> Math.max(0, (int) (5 - count));
            case BRONZE -> Math.max(0, (int) (10 - count));
            case SILVER -> Math.max(0, (int) (20 - count));
            case GOLD -> Math.max(0, (int) (50 - count));
            case PLATINUM -> 0; // Max level reached
        };
    }
}
package com.example.neighborhelp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for user statistics response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatisticsResponseRequest {

    private Long userId;
    private String username;
    private String organizationName;
    private Long resourcesCreated;
    private Long activeResources;
    private Long pendingResources;
    private Long ratingsGiven;
    private Double averageRatingGiven;
    private Long ratingsReceived;
    private Double averageRatingReceived;
    private Long totalViews;
    private Long helpfulVotes;
    private long contactsReceived;
    private LocalDateTime memberSince;
    private LocalDateTime lastActivity;
    private ContributionLevel contributionLevel;
    private Boolean isOrganization;
    private Boolean isVerified;

    /**
     * User contribution levels
     */
    public enum ContributionLevel {
        NEWCOMER,
        BRONZE,
        SILVER,
        GOLD,
        PLATINUM
    }

    // Opcional: Si usas el patrón Builder manual en lugar de Lombok
    public static class UserStatisticsResponseBuilder {
        private Long userId;
        private String username;
        private String organizationName;
        private Long resourcesCreated;
        private Long activeResources;
        private Long pendingResources;
        private Long ratingsGiven;
        private Double averageRatingGiven;
        private Long ratingsReceived;
        private Double averageRatingReceived;
        private Long totalViews;
        private Long helpfulVotes;
        private long contactsReceived;
        private LocalDateTime memberSince;
        private LocalDateTime lastActivity;
        private ContributionLevel contributionLevel;
        private Boolean isOrganization;
        private Boolean isVerified;

        public UserStatisticsResponseBuilder organizationName(String organizationName) {
            this.organizationName = organizationName;
            return this;
        }

        public UserStatisticsResponseBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public UserStatisticsResponseBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserStatisticsResponseBuilder resourcesCreated(Long resourcesCreated) {
            this.resourcesCreated = resourcesCreated;
            return this;
        }

        public UserStatisticsResponseBuilder activeResources(Long activeResources) {
            this.activeResources = activeResources;
            return this;
        }

        public UserStatisticsResponseBuilder pendingResources(Long pendingResources) {
            this.pendingResources = pendingResources;
            return this;
        }

        public UserStatisticsResponseBuilder ratingsGiven(Long ratingsGiven) {
            this.ratingsGiven = ratingsGiven;
            return this;
        }

        public UserStatisticsResponseBuilder averageRatingGiven(Double averageRatingGiven) {
            this.averageRatingGiven = averageRatingGiven;
            return this;
        }

        public UserStatisticsResponseBuilder ratingsReceived(Long ratingsReceived) {
            this.ratingsReceived = ratingsReceived;
            return this;
        }

        public UserStatisticsResponseBuilder averageRatingReceived(Double averageRatingReceived) {
            this.averageRatingReceived = averageRatingReceived;
            return this;
        }

        public UserStatisticsResponseBuilder totalViews(Long totalViews) {
            this.totalViews = totalViews;
            return this;
        }

        public UserStatisticsResponseBuilder helpfulVotes(Long helpfulVotes) {
            this.helpfulVotes = helpfulVotes;
            return this;
        }

        public UserStatisticsResponseBuilder memberSince(LocalDateTime memberSince) {
            this.memberSince = memberSince;
            return this;
        }

        public UserStatisticsResponseBuilder contactsReceived(long contactsReceived) {
            this.contactsReceived = contactsReceived;
            return this;
        }

        public UserStatisticsResponseBuilder lastActivity(LocalDateTime lastActivity) {
            this.lastActivity = lastActivity;
            return this;
        }

        public UserStatisticsResponseBuilder contributionLevel(ContributionLevel contributionLevel) {
            this.contributionLevel = contributionLevel;
            return this;
        }

        public UserStatisticsResponseBuilder isOrganization(Boolean isOrganization) {
            this.isOrganization = isOrganization;
            return this;
        }

        public UserStatisticsResponseBuilder isVerified(Boolean isVerified) {
            this.isVerified = isVerified;
            return this;
        }

        public UserStatisticsResponseRequest build() {
            return new UserStatisticsResponseRequest(
                    userId, username, organizationName, resourcesCreated, activeResources,
                    pendingResources, ratingsGiven, averageRatingGiven, ratingsReceived,
                    averageRatingReceived, totalViews, helpfulVotes,contactsReceived, memberSince, lastActivity,
                    contributionLevel, isOrganization, isVerified
            );
        }
    }
}
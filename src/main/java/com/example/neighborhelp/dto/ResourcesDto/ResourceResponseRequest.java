package com.example.neighborhelp.dto.ResourcesDto;

import com.example.neighborhelp.entity.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceResponseRequest {

    // ========== BASIC INFORMATION ==========
    private Long id;
    private String title;
    private String description;
    private String slug;

    // ========== CATEGORY ==========
    private String categoryName;

    // ========== LOCATION ==========
    private String city;
    private String postalCode;      // Added
    private String fullAddress;

    // ========== COST & STATUS ==========
    private String cost;            // Changed: Now returns additional_notes or "VARIES"
    private Resource.ResourceStatus status;

    // ========== METRICS ==========
    private Integer viewsCount;
    private Double averageRating;
    private Long totalRatings;      // Added - total number of ratings

    // ========== ADDITIONAL DETAILS ==========
    private String requirements;
    private Integer capacity;
    private Boolean wheelchairAccessible;
    private String languages;
    private String targetAudience;

    // ========== CONTACT INFO ==========
    private String contactInfo;     // Added
    private String phone;           // Added - extracted from contactInfo
    private String email;           // Added - extracted from contactInfo
    private String availability;    // Added

    // ========== MEDIA ==========
    private List<String> imageUrl;  // Changed: Now accepts multiple image URLs
    private String websiteUrl;

    // ========== USER INFO ==========
    private UserBasicInfo user;     // Added - information about who created it

    // ========== USER-SPECIFIC ==========
    private Boolean isSaved;        // Added - whether current user saved this resource

    // ========== TIMESTAMPS ==========
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Nested class for user basic information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserBasicInfo {
        private Long id;
        private String username;
        private String organizationName;
        private Boolean verified;
        private String type;        // INDIVIDUAL or ORGANIZATION
    }
}
package com.example.neighborhelp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embeddable class for user profile information.
 *
 * This class contains profile fields that can be used by both
 * individual users and organizations. Fields are optional and
 * used based on the user type.
 *
 * For INDIVIDUAL users:
 * - bio, phone, avatar
 *
 * For ORGANIZATION users:
 * - description, website, phone, address, logo
 */
@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    // ========== COMMON FIELDS ==========

    /**
     * Contact phone number
     */
    @Column(name = "profile_phone")
    private String phone;

    // ========== INDIVIDUAL USER FIELDS ==========

    /**
     * Bio/About me for individual users
     */
    @Column(name = "profile_bio", columnDefinition = "TEXT")
    private String bio;

    /**
     * Avatar image filename/path for individuals
     */
    @Column(name = "profile_avatar")
    private String avatar;

    // ========== ORGANIZATION FIELDS ==========

    /**
     * Description for organizations
     */
    @Column(name = "profile_description", columnDefinition = "TEXT")
    private String description;

    /**
     * Website URL for organizations
     */
    @Column(name = "profile_website")
    private String website;

    /**
     * Physical address for organizations
     */
    @Column(name = "profile_address", columnDefinition = "TEXT")
    private String address;

    /**
     * Logo image filename/path for organizations
     */
    @Column(name = "profile_logo")
    private String logo;

    // ========== ADDITIONAL FIELDS ==========

    /**
     * Social media links (JSON string or comma-separated)
     */
    @Column(name = "profile_social_media", columnDefinition = "TEXT")
    private String socialMedia;

    /**
     * Years of experience (for individuals or organization founding year)
     */
    @Column(name = "profile_years_experience")
    private Integer yearsExperience;

    /**
     * Skills or services offered (comma-separated or JSON)
     */
    @Column(name = "profile_skills", columnDefinition = "TEXT")
    private String skills;

    /**
     * Languages spoken (comma-separated)
     */
    @Column(name = "profile_languages")
    private String languages;

    /**
     * Availability status or hours
     */
    @Column(name = "profile_availability")
    private String availability;
}
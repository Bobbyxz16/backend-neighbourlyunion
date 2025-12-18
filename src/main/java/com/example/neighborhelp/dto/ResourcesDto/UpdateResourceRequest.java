package com.example.neighborhelp.dto.ResourcesDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * DTO for updating an existing resource
 * All fields are optional - only provided fields will be updated
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateResourceRequest {

    // ========== BASIC INFORMATION ==========

    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;

    // ========== CATEGORY ==========

    private String category; // e.g., "Alimentos", "Salud"

    // ========== LOCATION ==========

    @Size(max = 100, message = "City name must not exceed 100 characters")
    private String city;

    @Size(max = 255, message = "Street address must not exceed 255 characters")
    private String street;

    @Size(max = 100, message = "Neighborhood must not exceed 100 characters")
    private String neighborhood;

    @Pattern(regexp = "\\d{5}", message = "Postal code must be 5 digits")
    private String postalCode;

    @Size(max = 100, message = "Province must not exceed 100 characters")
    private String province;

    @Size(max = 50, message = "Country must not exceed 50 characters")
    private String country;

    // ========== CONTACT & AVAILABILITY ==========

    @Size(max = 500, message = "Contact info must not exceed 500 characters")
    private String contactInfo;

    @Size(max = 500, message = "Availability info must not exceed 500 characters")
    private String availability;

    // ========== COST ==========

    @Pattern(regexp = "FREE|LOW_COST|AFFORDABLE",
            message = "Cost must be one of: FREE, LOW_COST, AFFORDABLE")
    private String cost;

    // ========== ADDITIONAL DETAILS ==========

    @Size(max = 500, message = "Requirements must not exceed 500 characters")
    private String requirements;

    @Size(max = 1000, message = "Additional notes must not exceed 1000 characters")
    private String additionalNotes;

    @Min(value = 0, message = "Capacity must be a positive number")
    private Integer capacity;

    private Boolean wheelchairAccessible;

    @Size(max = 200, message = "Languages must not exceed 200 characters")
    private String languages;

    @Size(max = 500, message = "Target audience must not exceed 500 characters")
    private String targetAudience;

    // ========== MEDIA ==========

    private List<String> imageUrl;

    @Pattern(regexp = "^(https?://)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([/\\w \\.-]*)*/?$",
            message = "Invalid website URL")
    private String websiteUrl;
}
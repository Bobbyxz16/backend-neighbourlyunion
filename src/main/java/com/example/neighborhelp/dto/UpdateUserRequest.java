package com.example.neighborhelp.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.Size;
import java.util.List;

/**
 * DTO for updating user profile information.
 * All fields are optional - only provided fields will be updated.
 */
@Data
public class UpdateUserRequest {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    // Individual user fields
    private String firstName;
    private String lastName;

    // Organization fields
    private String organizationName;

    // Profile data
    private ProfileData profile;

    // Nested ProfileData class
    @Data
    public static class ProfileData {
        // Common
        private String phone;

        // Individual-specific
        private String bio;
        private String avatar;

        // Organization-specific
        private String description;

        @URL(message = "Invalid website URL format")
        private String website;

        private String address;
        private String logo;

        // Additional fields
        private String socialMedia;
        private Integer yearsExperience;

        @JsonSetter(nulls = Nulls.AS_EMPTY)
        private List<String> skills;

        @JsonSetter(nulls = Nulls.AS_EMPTY)
        private List<String> languages;

        private String availability;
    }
}
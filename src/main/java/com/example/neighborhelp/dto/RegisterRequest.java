package com.example.neighborhelp.dto;

import com.example.neighborhelp.entity.User.Role;
import com.example.neighborhelp.entity.User.UserType;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for user registration requests.
 * Supports both individual and organization registrations with nested profile data.
 */
@Data
public class RegisterRequest {

    // ========== COMMON FIELDS ==========

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "The password must be at least 8 characters long.")
    private String password;

    private Role role = Role.USER;

    @NotNull(message = "The user type is mandatory")
    private UserType type = UserType.INDIVIDUAL;

    // ========== INDIVIDUAL USER FIELDS ==========

    /**
     * First name (required for INDIVIDUAL users)
     */
    private String firstName;

    /**
     * Last name (required for INDIVIDUAL users)
     */
    private String lastName;

    // ========== ORGANIZATION FIELDS ==========

    /**
     * Organization name (required for ORGANIZATION users)
     */
    private String organizationName;

    // ========== PROFILE DATA ==========

    /**
     * Profile information - structure varies by user type
     */
    private ProfileData profile;

    // ========== NESTED PROFILE DTO ==========

    @Data
    public static class ProfileData {
        // Common
        private String phone;

        // Individual-specific
        private String bio;
        private String avatar;

        // Organization-specific
        private String description;
        private String website;
        private String address;
        private String logo;

        // Additional fields
        private String socialMedia;
        private Integer yearsExperience;
        private String skills;
        private String languages;
        private String availability;
    }
}
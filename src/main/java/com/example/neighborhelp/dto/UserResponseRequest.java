package com.example.neighborhelp.dto;

import com.example.neighborhelp.entity.User.Role;
import com.example.neighborhelp.entity.User.UserType;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for user response data.
 * Used to send user information to clients with profile data.
 */
@Data
public class UserResponseRequest {
    private Long id;
    private String username;
    private String email; // Only included for authenticated user viewing their own profile
    private Role role;
    private UserType type;

    // Individual user fields
    private String firstName;
    private String lastName;

    // Organization fields
    private String organizationName;

    // Profile data
    private ProfileData profile;

    private Boolean verified;
    private Boolean enabled;
    private LocalDateTime createdAt;

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
        private String website;
        private String address;
        private String logo;

        // Additional fields
        private String socialMedia;
        private Integer yearsExperience;
        @JsonSetter(nulls = Nulls.AS_EMPTY)
        private List<String> skills;  // Cambiar de String a List<String>
        @JsonSetter(nulls = Nulls.AS_EMPTY)
        private List<String> languages;
        private String availability;
    }

    // Builder pattern for flexible object creation
    public static class Builder {
        private final UserResponseRequest response = new UserResponseRequest();

        public Builder id(Long id) {
            response.id = id;
            return this;
        }

        public Builder username(String username) {
            response.username = username;
            return this;
        }

        public Builder email(String email) {
            response.email = email;
            return this;
        }

        public Builder role(Role role) {
            response.role = role;
            return this;
        }

        public Builder type(UserType type) {
            response.type = type;
            return this;
        }

        public Builder firstName(String firstName) {
            response.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            response.lastName = lastName;
            return this;
        }

        public Builder organizationName(String organizationName) {
            response.organizationName = organizationName;
            return this;
        }

        public Builder profile(ProfileData profile) {
            response.profile = profile;
            return this;
        }

        public Builder verified(Boolean verified) {
            response.verified = verified;
            return this;
        }

        public Builder enabled(Boolean enabled) {
            response.enabled = enabled;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            response.createdAt = createdAt;
            return this;
        }

        public UserResponseRequest build() {
            return response;
        }
    }
}
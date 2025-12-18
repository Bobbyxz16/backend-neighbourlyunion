package com.example.neighborhelp.dto.AdminDto;

import com.example.neighborhelp.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for admin user list view
 * Includes sensitive information only visible to admins
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserListResponse {

    private Long id;
    private String username;
    private String email;
    private User.Role role;
    private User.UserType type;
    private String firstName;
    private String lastName;
    private String organizationName;
    private Boolean verified;
    private Boolean enabled;
    private Boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private String authProvider;

    // Helper method to get display name
    public String getDisplayName() {
        if (type == User.UserType.ORGANIZATION) {
            return organizationName;
        } else {
            if (firstName != null && lastName != null) {
                return firstName + " " + lastName;
            }
            return username;
        }
    }

    // Helper method to get identifier for URLs
    public String getIdentifier() {
        return (type == User.UserType.ORGANIZATION && organizationName != null)
                ? organizationName
                : username;
    }
}
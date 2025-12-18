package com.example.neighborhelp.dto.ResourcesDto;

import com.example.neighborhelp.entity.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceStatusOverviewResponse {
    private Long id;
    private String slug;
    private String username;
    private String email;
    private String resourceTitle;
    private String resourceDescription;
    private String category;
    private String city;
    private Resource.ResourceStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Helper method to get the display format you wanted
    public String getDisplayFormat() {
        return String.format("%s - %s - %s", username, resourceTitle, status);
    }
}
package com.example.neighborhelp.dto.RatingsDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingResponse {
    private Long id;
    private Integer rating;
    private String comment;
    private Integer helpfulCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // User info (minimal to avoid circular references)
    private Long userId;
    private String userName;

    // Resource info (minimal)
    private Long resourceId;
    private String resourceName;
    private String resourceSlug;

    // Computed fields
    private String starRating;
    private boolean hasComment;
}

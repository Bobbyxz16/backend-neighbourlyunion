package com.example.neighborhelp.dto.RatingsDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedRatingResponse {
    private java.util.List<RatingResponse> ratings;
    private int currentPage;
    private int totalPages;
    private long totalItems;
    private boolean hasNext;
    private boolean hasPrevious;
    private RatingSummary summary;
}

package com.example.neighborhelp.service;

import com.example.neighborhelp.dto.RatingsDto.PaginatedRatingResponse;
import com.example.neighborhelp.dto.RatingsDto.RatingRequest;
import com.example.neighborhelp.dto.RatingsDto.RatingResponse;
import com.example.neighborhelp.dto.RatingsDto.RatingSummary;
import com.example.neighborhelp.entity.Rating;
import com.example.neighborhelp.entity.Resource;
import com.example.neighborhelp.entity.User;
import com.example.neighborhelp.repository.RatingRepository;
import com.example.neighborhelp.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RatingService {

    private final RatingRepository ratingRepository;
    private final ResourceRepository resourceRepository;

    /**
     * Get paginated ratings for a resource by slug
     */
    @Transactional(readOnly = true)
    public PaginatedRatingResponse getRatingsByResourceSlug(String resourceSlug, Pageable pageable) {
        log.debug("Fetching ratings for resource slug: {}", resourceSlug);

        // Verify resource exists
        Resource resource = resourceRepository.findBySlug(resourceSlug)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found with slug: " + resourceSlug));

        Page<Rating> ratingsPage = ratingRepository.findByResourceSlug(resourceSlug, pageable);
        RatingSummary summary = getRatingSummaryBySlug(resourceSlug);

        return buildPaginatedResponse(ratingsPage, summary);
    }

    /**
     * Create a new rating for a resource by slug
     */
    public RatingResponse createRatingForResourceSlug(String resourceSlug,
                                                                RatingRequest request,
                                                                User currentUser) {
        log.debug("Creating rating for resource slug: {} by user: {}", resourceSlug, currentUser.getEmail());

        Resource resource = resourceRepository.findBySlug(resourceSlug)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found with slug: " + resourceSlug));

        return createRating(currentUser, resource, request);
    }

    public void deleteRatingByResourceSlug(String resourceSlug, User currentUser) {
        log.debug("Deleting rating for resource slug: {} by user: {}", resourceSlug, currentUser.getEmail());

        // Find the rating for this user and resource
        Rating rating = ratingRepository.findByResourceSlugAndUserId(resourceSlug, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("You haven't rated this resource or rating not found"));

        ratingRepository.delete(rating);
        log.info("Rating deleted for resource {} by user {}", resourceSlug, currentUser.getId());
    }

    /**
     * Delete a specific rating by ID (user can delete their own, admin can delete any)
     */
    public void deleteRating(Long ratingId, User currentUser) {
        log.debug("Deleting rating with ID: {} by user: {}", ratingId, currentUser.getEmail());

        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new IllegalArgumentException("Rating not found with ID: " + ratingId));

        // User can delete their own rating, admin can delete any rating
        boolean isOwner = rating.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You can only delete your own ratings");
        }

        ratingRepository.delete(rating);
        log.info("Rating {} deleted by user {}", ratingId, currentUser.getId());
    }

    /**
     * Mark a rating as helpful
     */
    public RatingResponse markAsHelpful(Long ratingId, User currentUser) {
        log.debug("Marking rating {} as helpful by user: {}", ratingId, currentUser.getEmail());

        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new IllegalArgumentException("Rating not found with ID: " + ratingId));

        rating.markAsHelpful();
        Rating savedRating = ratingRepository.save(rating);

        log.info("Rating {} marked as helpful by user {}", ratingId, currentUser.getId());
        return convertToResponse(savedRating);
    }

    /**
     * Get rating summary for a resource by slug
     */
    @Transactional(readOnly = true)
    public RatingSummary getRatingSummaryBySlug(String resourceSlug) {
        Double averageRating = ratingRepository.calculateAverageRatingByResourceSlug(resourceSlug);
        Long totalRatings = ratingRepository.countByResourceSlug(resourceSlug);

        // Get rating distribution
        Integer fiveStarCount = ratingRepository.countByResourceSlugAndScore(resourceSlug, 5).intValue();
        Integer fourStarCount = ratingRepository.countByResourceSlugAndScore(resourceSlug, 4).intValue();
        Integer threeStarCount = ratingRepository.countByResourceSlugAndScore(resourceSlug, 3).intValue();
        Integer twoStarCount = ratingRepository.countByResourceSlugAndScore(resourceSlug, 2).intValue();
        Integer oneStarCount = ratingRepository.countByResourceSlugAndScore(resourceSlug, 1).intValue();

        return RatingSummary.builder()
                .averageRating(averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : 0.0)
                .totalRatings(totalRatings)
                .fiveStarCount(fiveStarCount)
                .fourStarCount(fourStarCount)
                .threeStarCount(threeStarCount)
                .twoStarCount(twoStarCount)
                .oneStarCount(oneStarCount)
                .build();
    }

    // Private helper methods
    private RatingResponse createRating(User user, Resource resource, RatingRequest request) {
        // Check if user has already rated this resource
        Optional<Rating> existingRating = ratingRepository.findByUserIdAndResourceId(user.getId(), resource.getId());
        if (existingRating.isPresent()) {
            throw new IllegalArgumentException("You have already rated this resource");
        }

        // Create new rating using builder
        Rating rating = Rating.builder()
                .user(user)
                .resource(resource)
                .rating(request.getRating())
                .comment(request.getComment())
                .helpfulCount(0)
                .build();

        Rating savedRating = ratingRepository.save(rating);
        log.info("Rating created for resource {} by user {}", resource.getSlug(), user.getEmail());

        return convertToResponse(savedRating);
    }

    private PaginatedRatingResponse buildPaginatedResponse(Page<Rating> ratingsPage, RatingSummary summary) {
        return PaginatedRatingResponse.builder()
                .ratings(ratingsPage.getContent().stream()
                        .map(this::convertToResponse)
                        .toList())
                .currentPage(ratingsPage.getNumber())
                .totalPages(ratingsPage.getTotalPages())
                .totalItems(ratingsPage.getTotalElements())
                .hasNext(ratingsPage.hasNext())
                .hasPrevious(ratingsPage.hasPrevious())
                .summary(summary)
                .build();
    }

    private RatingResponse convertToResponse(Rating rating) {
        return RatingResponse.builder()
                .id(rating.getId())
                .rating(rating.getRating())
                .comment(rating.getComment())
                .helpfulCount(rating.getHelpfulCount())
                .createdAt(rating.getCreatedAt())
                .updatedAt(rating.getUpdatedAt())
                .userId(rating.getUser().getId())
                .userName(rating.getUser().getUsername())
                .resourceId(rating.getResource().getId())
                .resourceName(rating.getResource().getTitle())
                .resourceSlug(rating.getResource().getSlug())
                .starRating(rating.getStarRating())
                .hasComment(rating.hasComment())
                .build();
    }
}
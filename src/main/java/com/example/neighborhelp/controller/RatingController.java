package com.example.neighborhelp.controller;

import com.example.neighborhelp.dto.RatingsDto.PaginatedRatingResponse;
import com.example.neighborhelp.dto.RatingsDto.RatingRequest;
import com.example.neighborhelp.dto.RatingsDto.RatingResponse;
import com.example.neighborhelp.dto.RatingsDto.RatingSummary;
import com.example.neighborhelp.entity.User;
import com.example.neighborhelp.repository.UserRepository;
import com.example.neighborhelp.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;
    private final UserRepository userRepository;

    /**
     * GET /api/resources/{resourceName}/ratings
     * Get paginated ratings for a resource by resourceName (slug)
     * Public endpoint
     */
    @GetMapping("resources/{resourceName}/ratings")
    public ResponseEntity<PaginatedRatingResponse> getRatingsByResourceName(
            @PathVariable String resourceName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PaginatedRatingResponse response = ratingService.getRatingsByResourceSlug(resourceName, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/resources/{resourceName}/ratings
     * Create a rating for a resource by resourceName (slug)
     * Auth required - USER only
     */
    @PostMapping("resources/{resourceName}/ratings")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<RatingResponse> createRatingForResource(
            @PathVariable String resourceName,
            @Valid @RequestBody RatingRequest request) {

        // Obtener el usuario autenticado
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        RatingResponse response = ratingService.createRatingForResourceSlug(resourceName, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/resources/{resourceName}/ratings/summary
     * Get rating summary for a resource
     * Public endpoint
     */
    @GetMapping("resources/{resourceName}/ratings/summary")
    public ResponseEntity<RatingSummary> getRatingSummary(
            @PathVariable String resourceName) {

        RatingSummary summary = ratingService.getRatingSummaryBySlug(resourceName);
        return ResponseEntity.ok(summary);
    }

    /**
     * DELETE /api/resources/{resourceName}/ratings
     * Delete current user's rating for a resource
     * Auth required - User can delete their own rating
     */
    @DeleteMapping("/resources/{resourceName}/ratings")
    public ResponseEntity<Void> deleteMyRating(
            @PathVariable String resourceName) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        ratingService.deleteRatingByResourceSlug(resourceName, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/ratings/{ratingId}
     * Delete a specific rating by ID
     * Auth required - User can delete their own rating, ADMIN can delete any
     */
    @DeleteMapping("/ratings/{ratingId}")
    public ResponseEntity<Void> deleteRating(
            @PathVariable Long ratingId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        ratingService.deleteRating(ratingId, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/ratings/{ratingId}/helpful
     * Mark a rating as helpful
     * Auth required - any authenticated user
     */
    @PatchMapping("/ratings/{ratingId}/helpful")
    public ResponseEntity<RatingResponse> markAsHelpful(@PathVariable Long ratingId) {

        // Obtener el usuario autenticado (opcional, para tracking)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + userEmail));

        RatingResponse response = ratingService.markAsHelpful(ratingId, currentUser);
        return ResponseEntity.ok(response);
    }
}
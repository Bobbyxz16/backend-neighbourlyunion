package com.example.neighborhelp.controller;

import com.example.neighborhelp.dto.CategoriesDto.CategoryRequest;
import com.example.neighborhelp.dto.CategoriesDto.CategoryResponse;
import com.example.neighborhelp.dto.MessageResponseRequest;
import com.example.neighborhelp.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * Category Controller - Manages resource categories
 *
 * Endpoints:
 * - GET /api/categories - List all categories (public, cached)
 * - POST /api/categories - Create category (admin only)
 * - PUT /api/categories/{id} - Update category (admin only)
 * - DELETE /api/categories/{id} - Delete category (admin only)
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * GET /api/categories
     * List all categories
     *
     * Public endpoint with caching for better performance
     * Returns all categories sorted by name
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Cacheable(value = "categories", key = "'all'")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * GET /api/categories/{id}
     * Get single category by ID
     *
     * Public endpoint
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        CategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(category);
    }

    /**
     * GET /api/categories/search
     * Search categories by name
     *
     * Query param: name - partial match (case-insensitive)
     * Public endpoint
     */
    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CategoryResponse>> searchCategories(
            @RequestParam String name) {
        List<CategoryResponse> categories = categoryService.searchCategories(name);
        return ResponseEntity.ok(categories);
    }

    /**
     * POST /api/categories
     * Create new category
     *
     * Auth required - ADMIN only
     * Clears cache after creation
     */
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse category = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }

    /**
     * PUT /api/categories/{id}
     * Update existing category
     *
     * Auth required - ADMIN only
     * Clears cache after update
     */
    @PutMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse category = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(category);
    }

    /**
     * DELETE /api/categories/{id}
     * Delete category
     *
     * Auth required - ADMIN only
     * Cannot delete if category has resources
     * Clears cache after deletion
     */
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponseRequest> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(new MessageResponseRequest("Category deleted successfully"));
    }

    /**
     * GET /api/categories/{id}/resources/count
     * Get count of resources in a category
     *
     * Public endpoint
     */
    @GetMapping(value = "/{id}/resources/count", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> getResourceCount(@PathVariable Long id) {
        Long count = categoryService.getResourceCount(id);
        return ResponseEntity.ok(count);
    }
}
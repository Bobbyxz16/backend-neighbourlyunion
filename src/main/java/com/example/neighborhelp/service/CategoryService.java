package com.example.neighborhelp.service;

import com.example.neighborhelp.dto.CategoriesDto.CategoryRequest;
import com.example.neighborhelp.dto.CategoriesDto.CategoryResponse;
import com.example.neighborhelp.entity.Category;
import com.example.neighborhelp.exception.ResourceNotFoundException;
import com.example.neighborhelp.repository.CategoryRepository;
import com.example.neighborhelp.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing categories
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ResourceRepository resourceRepository;

    /**
     * Get all categories sorted by name
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'all'")
    public List<CategoryResponse> getAllCategories() {
        log.debug("Fetching all categories");
        return categoryRepository.findAll().stream()
                .sorted((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get category by ID
     */
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        return mapToResponse(category);
    }

    /**
     * Search categories by name (partial match, case-insensitive)
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> searchCategories(String name) {
        log.debug("Searching categories with name: {}", name);
        return categoryRepository.findAll().stream()
                .filter(c -> c.getName().toLowerCase().contains(name.toLowerCase()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create new category
     */
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request) {
        log.info("Creating category: {}", request.getName());

        // Check if category already exists
        if (categoryRepository.findByNameIgnoreCase(request.getName()).isPresent()) {
            throw new IllegalStateException("Category already exists: " + request.getName());
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());

        Category saved = categoryRepository.save(category);
        log.info("Category created: ID={}, name={}", saved.getId(), saved.getName());

        return mapToResponse(saved);
    }

    /**
     * Update existing category
     */
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        log.info("Updating category ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        // Check if new name conflicts with existing category
        if (!category.getName().equalsIgnoreCase(request.getName())) {
            categoryRepository.findByNameIgnoreCase(request.getName()).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new IllegalStateException("Category name already exists: " + request.getName());
                }
            });
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        Category updated = categoryRepository.save(category);
        log.info("Category updated: ID={}, name={}", updated.getId(), updated.getName());

        return mapToResponse(updated);
    }

    /**
     * Delete category
     * Cannot delete if category has resources
     */
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(Long id) {
        log.info("Deleting category ID: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        // Check if category has resources
        long resourceCount = resourceRepository.countByCategory(category);
        if (resourceCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete category. It has " + resourceCount + " resources. " +
                            "Please reassign or delete resources first."
            );
        }

        categoryRepository.delete(category);
        log.info("Category deleted: ID={}, name={}", id, category.getName());
    }

    /**
     * Get count of resources in a category
     */
    @Transactional(readOnly = true)
    public Long getResourceCount(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        return resourceRepository.countByCategory(category);
    }

    /**
     * Map Category entity to CategoryResponse DTO
     */
    private CategoryResponse mapToResponse(Category category) {
        long resourceCount = resourceRepository.countByCategory(category);

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .resourceCount(resourceCount)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
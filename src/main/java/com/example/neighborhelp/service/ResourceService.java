package com.example.neighborhelp.service;

import com.example.neighborhelp.dto.ResourcesDto.CreateResourceRequest;
import com.example.neighborhelp.dto.ResourcesDto.ResourceResponseRequest;
import com.example.neighborhelp.dto.ResourcesDto.ResourceStatusOverviewResponse;
import com.example.neighborhelp.dto.ResourcesDto.UpdateResourceRequest;
import com.example.neighborhelp.entity.*;
import com.example.neighborhelp.exception.ForbiddenException;
import com.example.neighborhelp.exception.ResourceNotFoundException;
import com.example.neighborhelp.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RatingRepository ratingRepository;
    private final SavedResourceRepository savedResourceRepository;
    private final ResourceViewRepository resourceViewRepository;

    // ========== CREATE RESOURCE ==========

    @Transactional
    @CacheEvict(value = {"userStatistics", "userBasicStatistics"}, allEntries = true)
    public ResourceResponseRequest createResource(CreateResourceRequest request, Long userId) {
        log.info("Creating resource '{}' for user with ID: {}", request.getTitle(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        return createResourceInternal(request, user);
    }

    // ========== SEARCH & FILTER ==========

    @Transactional(readOnly = true)
    public Page<ResourceResponseRequest> searchResources(
            Long categoryId, String city, String postalCode,
            String cost, String status, String search, Pageable pageable) {

        log.debug("Searching resources: categoryId={}, city={}, cost={}, search={}",
                categoryId, city, cost, search);

        Specification<Resource> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filtro por estado (ACTIVE por defecto)
            if (status == null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), Resource.ResourceStatus.ACTIVE));
            } else {
                try {
                    Resource.ResourceStatus resourceStatus = Resource.ResourceStatus.valueOf(status.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("status"), resourceStatus));
                } catch (IllegalArgumentException e) {
                    predicates.add(criteriaBuilder.equal(root.get("status"), Resource.ResourceStatus.ACTIVE));
                }
            }

            // Filtro por categoría (ID)
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("category").get("id"),
                        categoryId
                ));
            }

            // Filtro por ciudad
            if (city != null && !city.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("location").get("city")),
                        "%" + city.toLowerCase() + "%"
                ));
            }

            // Filtro por código postal
            if (postalCode != null && !postalCode.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                        root.get("location").get("postalCode"),
                        postalCode
                ));
            }

            // Filtro por costo
            if (cost != null && !cost.trim().isEmpty()) {
                try {
                    Resource.CostType costType = Resource.CostType.valueOf(cost.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("cost"), costType));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid cost type: {}", cost);
                }
            }

            // Búsqueda en título y descripción
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                Predicate titleMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")),
                        searchPattern
                );
                Predicate descriptionMatch = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("description")),
                        searchPattern
                );
                predicates.add(criteriaBuilder.or(titleMatch, descriptionMatch));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Resource> resources = resourceRepository.findAll(spec, pageable);

        // Get current user ID if authenticated
        Long currentUserId = getCurrentUserId();

        return resources.map(resource -> mapToResourceResponse(resource, currentUserId));
    }

    // ========== GET RESOURCE ==========

    @Transactional
    public ResourceResponseRequest getResourceById(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + id));

        Long currentUserId = getCurrentUserId();

        boolean isOwner = currentUserId != null
                && resource.getUser() != null
                && resource.getUser().getId().equals(currentUserId);

        // Optional: allow admins as well
        boolean isAdmin = false;
        if (currentUserId != null) {
            User currentUser = userRepository.findById(currentUserId)
                    .orElse(null);
            isAdmin = currentUser != null && currentUser.getRole() == User.Role.ADMIN;
        }

        // Only ACTIVE resources are public.
        if (resource.getStatus() != Resource.ResourceStatus.ACTIVE && !isOwner && !isAdmin) {
            throw new ResourceNotFoundException("Resource not found with ID: " + id);
        }

        // Track unique view
        trackUniqueView(resource, currentUserId, null);

        return mapToResourceResponse(resource, currentUserId);
    }

    @Transactional
    public ResourceResponseRequest getResourceBySlug(String slug) {
        Resource resource = resourceRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with slug: " + slug));

        Long currentUserId = getCurrentUserId();

        // Check if user is owner
        boolean isOwner = currentUserId != null
                && resource.getUser() != null
                && resource.getUser().getId().equals(currentUserId);

        // Check if user is admin or moderator
        boolean isAdminOrModerator = false;
        if (currentUserId != null) {
            User currentUser = userRepository.findById(currentUserId).orElse(null);
            if (currentUser != null) {
                isAdminOrModerator = currentUser.getRole() == User.Role.ADMIN
                        || currentUser.getRole() == User.Role.MODERATOR;
            }
        }

        // Only ACTIVE resources are public, unless user is owner, admin, or moderator
        if (resource.getStatus() != Resource.ResourceStatus.ACTIVE
                && !isOwner && !isAdminOrModerator) {
            throw new ResourceNotFoundException("Resource not found with slug: " + slug);
        }

        // Track unique view
        trackUniqueView(resource, currentUserId, null);

        return mapToResourceResponse(resource, currentUserId);
    }

    private void trackUniqueView(Resource resource, Long userId, String ipAddress) {
        try {
            if (userId != null) {
                // Authenticated user
                User user = userRepository.findById(userId).orElse(null);
                if (user == null) return;

                Optional<ResourceView> existingView = resourceViewRepository.findByUserAndResource(user, resource);

                if (existingView.isPresent()) {
                    // Update existing view
                    ResourceView view = existingView.get();
                    view.setLastViewedAt(LocalDateTime.now());
                    view.setViewCount(view.getViewCount() + 1);
                    resourceViewRepository.save(view);
                } else {
                    // Create new view record
                    ResourceView newView = ResourceView.builder()
                            .user(user)
                            .resource(resource)
                            .lastViewedAt(LocalDateTime.now())
                            .viewCount(1)
                            .build();
                    resourceViewRepository.save(newView);
                }
            } else if (ipAddress != null) {
                // Anonymous user - track by IP hash
                String ipHash = hashIpAddress(ipAddress);

                Optional<ResourceView> existingView = resourceViewRepository.findByIpHashAndResource(ipHash, resource);

                if (existingView.isPresent()) {
                    // Update existing view
                    ResourceView view = existingView.get();
                    view.setLastViewedAt(LocalDateTime.now());
                    view.setViewCount(view.getViewCount() + 1);
                    resourceViewRepository.save(view);
                } else {
                    // Create new view record
                    ResourceView newView = ResourceView.builder()
                            .resource(resource)
                            .ipHash(ipHash)
                            .lastViewedAt(LocalDateTime.now())
                            .viewCount(1)
                            .build();
                    resourceViewRepository.save(newView);
                }
            }
            // If neither userId nor ipAddress, don't track (e.g., bots)
        } catch (Exception e) {
            log.error("Error tracking view for resource {}: {}", resource.getId(), e.getMessage());
            // Don't fail the request if view tracking fails
        }
    }

    /**
     * Hash IP address for privacy (SHA-256)
     */
    private String hashIpAddress(String ipAddress) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ipAddress.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error hashing IP address: {}", e.getMessage());
            return null;
        }
    }


    // ========== UPDATE RESOURCE ==========

    @Transactional
    public ResourceResponseRequest updateResourceById(Long id, UpdateResourceRequest request, Long userId) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + id));

        validateResourceOwnership(resource, userId);

        return updateResourceFields(resource, request);
    }

    @Transactional
    public ResourceResponseRequest updateResourceBySlug(String slug, UpdateResourceRequest request, Long userId) {
        Resource resource = resourceRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with slug: " + slug));

        validateResourceOwnership(resource, userId);

        return updateResourceFields(resource, request);
    }

    // ========== DELETE RESOURCE ==========

    @Transactional
    public void deleteResourceById(Long id, Long userId) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + id));

        validateResourceOwnership(resource, userId);

        resource.setStatus(Resource.ResourceStatus.INACTIVE);
        resourceRepository.save(resource);
    }

    @Transactional
    public void deleteResourceBySlug(String slug, Long userId) {
        Resource resource = resourceRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with slug: " + slug));

        validateResourceOwnership(resource, userId);

        resource.setStatus(Resource.ResourceStatus.INACTIVE);
        resourceRepository.save(resource);
    }

    @Transactional
    public void permanentlyDeleteResourceById(Long id, Long userId) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + id));

        validateResourceOwnership(resource, userId);

        resourceRepository.delete(resource);
        log.info("Resource permanently deleted: ID={}, Title={}", id, resource.getTitle());
    }

    @Transactional
    public void permanentlyDeleteResourceBySlug(String slug, Long userId) {
        // Buscar el recurso por slug
        Resource resource = resourceRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Resource not found with slug: " + slug));

        // Verificar permisos
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (!resource.getUser().getId().equals(userId) && !currentUser.getRole().equals(User.Role.ADMIN)) {
            throw new AccessDeniedException("You are not authorized to delete this resource");
        }

        // **EN LUGAR DE ELIMINAR, HACER SOFT DELETE**
        resource.setDeleted(true);
        resource.setDeletedAt(new Date());
        resourceRepository.save(resource);

        // Los reportes se mantienen automáticamente con su resource_id intacto
    }



    // ========== DEACTIVATE RESOURCE ==========

    @Transactional
    public void deactivateResourceById(Long id, Long userId) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + id));

        validateResourceOwnership(resource, userId);

        resource.setStatus(Resource.ResourceStatus.INACTIVE);
        resourceRepository.save(resource);
        log.info("Resource deactivated: ID={}, Title={}", id, resource.getTitle());
    }

    @Transactional
    public void deactivateResourceBySlug(String slug, Long userId) {
        Resource resource = resourceRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with slug: " + slug));

        validateResourceOwnership(resource, userId);

        resource.setStatus(Resource.ResourceStatus.INACTIVE);
        resourceRepository.save(resource);
        log.info("Resource deactivated: Slug={}, Title={}", slug, resource.getTitle());
    }

    // ========== ACTIVATE RESOURCE ==========

    @Transactional
    public void activateResourceById(Long id, Long userId) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + id));

        validateResourceOwnership(resource, userId);

        resource.setStatus(Resource.ResourceStatus.ACTIVE);
        resourceRepository.save(resource);
        log.info("Resource activated: ID={}, Title={}", id, resource.getTitle());
    }

    @Transactional
    public void activateResourceBySlug(String slug, Long userId) {
        Resource resource = resourceRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with slug: " + slug));

        validateResourceOwnership(resource, userId);

        resource.setStatus(Resource.ResourceStatus.ACTIVE);
        resourceRepository.save(resource);
        log.info("Resource activated: Slug={}, Title={}", slug, resource.getTitle());
    }

    // ========== STATUS MANAGEMENT ==========

    @Transactional
    public void updateResourceStatusById(Long id, Resource.ResourceStatus status, Long userId) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + id));

        validateResourceOwnership(resource, userId);

        resource.setStatus(status);
        resourceRepository.save(resource);
    }

    @Transactional
    public void updateResourceStatusBySlug(String slug, Resource.ResourceStatus status, Long userId) {
        Resource resource = resourceRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with slug: " + slug));

        validateResourceOwnership(resource, userId);

        resource.setStatus(status);
        resourceRepository.save(resource);
    }

    // ========== ADMIN/MODERATOR OVERVIEW ==========

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
    public Page<ResourceStatusOverviewResponse> getResourcesStatusOverview(
            String statusFilter, String search, String username, Pageable pageable) {

        Specification<Resource> spec = buildStatusOverviewSpecification(statusFilter, search, username);
        Page<Resource> resources = resourceRepository.findAll(spec, pageable);
        return resources.map(this::mapToResourceStatusOverview);
    }

    private Specification<Resource> buildStatusOverviewSpecification(String statusFilter, String search, String username) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (statusFilter != null && !statusFilter.trim().isEmpty()) {
                try {
                    Resource.ResourceStatus status = Resource.ResourceStatus.valueOf(statusFilter.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("status"), status));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid status filter: {}", statusFilter);
                }
            }

            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                Predicate titleMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), searchPattern);
                Predicate descriptionMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), searchPattern);
                predicates.add(criteriaBuilder.or(titleMatch, descriptionMatch));
            }

            if (username != null && !username.trim().isEmpty()) {
                String usernamePattern = "%" + username.toLowerCase() + "%";
                Predicate usernameMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("user").get("username")), usernamePattern);
                Predicate organizationNameMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("user").get("organizationName")), usernamePattern);
                predicates.add(criteriaBuilder.or(usernameMatch, organizationNameMatch));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private ResourceStatusOverviewResponse mapToResourceStatusOverview(Resource resource) {
        String username = resource.getUser().getUsername();
        if (username == null && resource.getUser().getOrganizationName() != null) {
            username = resource.getUser().getOrganizationName() + " (Organization)";
        }

        return new ResourceStatusOverviewResponse(
                resource.getId(),
                resource.getSlug(),
                username,
                resource.getUser().getEmail(),
                resource.getTitle(),
                resource.getDescription(),
                resource.getCategory().getName(),
                resource.getLocation().getCity(),
                resource.getStatus(),
                resource.getCreatedAt(),
                resource.getUpdatedAt()
        );
    }

    // ========== USER RESOURCES ==========

    @Transactional(readOnly = true)
    public Page<ResourceResponseRequest> getUserActiveResources(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Long currentUserId = getCurrentUserId();
        return resourceRepository.findByUserAndStatus(user, Resource.ResourceStatus.ACTIVE, pageable)
                .map(resource -> mapToResourceResponse(resource, currentUserId));
    }

    @Transactional(readOnly = true)
    public Page<ResourceResponseRequest> getUserResources(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        return resourceRepository.findByUser(user, pageable)
                .map(resource -> mapToResourceResponse(resource, userId));
    }

    // ========== MODERATION ==========

    @Transactional(readOnly = true)
    public Page<ResourceResponseRequest> getPendingResources(Pageable pageable) {
        Long currentUserId = getCurrentUserId();
        return resourceRepository.findByStatus(Resource.ResourceStatus.PENDING, pageable)
                .map(resource -> mapToResourceResponse(resource, currentUserId));
    }

    @Transactional
    public void approveResourceById(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + id));
        resource.setStatus(Resource.ResourceStatus.ACTIVE);
        resourceRepository.save(resource);
    }

    @Transactional
    public void approveResourceBySlug(String slug) {
        Resource resource = resourceRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with slug: " + slug));
        resource.setStatus(Resource.ResourceStatus.ACTIVE);
        resourceRepository.save(resource);
    }

    @Transactional
    public void rejectResourceById(Long id, String reason) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + id));
        resource.setStatus(Resource.ResourceStatus.REJECTED);
        resource.setRejectionReason(reason);
        resourceRepository.save(resource);
    }

    @Transactional
    public void rejectResourceBySlug(String slug, String reason) {
        Resource resource = resourceRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with slug: " + slug));
        resource.setStatus(Resource.ResourceStatus.REJECTED);
        resource.setRejectionReason(reason);
        resourceRepository.save(resource);
    }

    // ========== SAVE/UNSAVE RESOURCES ==========

    @Transactional
    public void saveResource(String resourceName, Long userId) {
        log.info("User {} saving resource: {}", userId, resourceName);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Resource resource = resourceRepository.findBySlug(resourceName)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with slug: " + resourceName));

        if (savedResourceRepository.existsByUserAndResource(user, resource)) {
            log.info("Resource already saved by user");
            return;
        }

        SavedResource savedResource = SavedResource.builder()
                .user(user)
                .resource(resource)
                .build();

        savedResourceRepository.save(savedResource);
        log.info("Resource saved successfully");
    }

    @Transactional
    public void unsaveResource(String resourceName, Long userId) {
        log.info("User {} unsaving resource: {}", userId, resourceName);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Resource resource = resourceRepository.findBySlug(resourceName)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with slug: " + resourceName));

        savedResourceRepository.deleteByUserAndResource(user, resource);
        log.info("Resource unsaved successfully");
    }

    @Transactional(readOnly = true)
    public Page<ResourceResponseRequest> getSavedResources(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Page<SavedResource> savedResources = savedResourceRepository.findByUser(user, pageable);

        return savedResources.map(savedResource ->
                mapToResourceResponse(savedResource.getResource(), userId)
        );
    }

    @Transactional(readOnly = true)
    public boolean isResourceSaved(String resourceName, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Resource resource = resourceRepository.findBySlug(resourceName)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with slug: " + resourceName));

        return savedResourceRepository.existsByUserAndResource(user, resource);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private ResourceResponseRequest createResourceInternal(CreateResourceRequest request, User user) {
        Category category = resolveCategoryByName(request.getCategory());
        Resource.CostType costType = parseCostType(request.getCost());

        ResourceLocation location = ResourceLocation.builder()
                .city(request.getCity())
                .street(request.getStreet())
                .neighborhood(request.getNeighborhood())
                .postalCode(request.getPostalCode())
                .province(request.getProvince())
                .country(request.getCountry() != null ? request.getCountry() : "England")
                .build();

        Resource resource = new Resource();
        resource.setTitle(request.getTitle());
        resource.setDescription(request.getDescription());
        resource.setUser(user);
        resource.setCategory(category);
        resource.setLocation(location);
        resource.setContactInfo(request.getContactInfo());
        resource.setAvailability(request.getAvailability());
        resource.setCost(costType);
        resource.setStatus(Resource.ResourceStatus.PENDING);
        resource.setViewsCount(0);
        resource.setRequirements(request.getRequirements());
        resource.setAdditionalNotes(request.getAdditionalNotes());
        resource.setCapacity(request.getCapacity());
        resource.setWheelchairAccessible(request.getWheelchairAccessible());
        resource.setLanguages(request.getLanguages());
        resource.setTargetAudience(request.getTargetAudience());
        resource.setImageUrl(request.getImageUrl());
        resource.setWebsiteUrl(request.getWebsiteUrl());

        resource.generateSlug();
        String uniqueSlug = ensureUniqueSlug(resource.getSlug());
        resource.setSlug(uniqueSlug);

        Resource savedResource = resourceRepository.save(resource);
        log.info("Resource created: ID={}, slug={}", savedResource.getId(), savedResource.getSlug());

        return mapToResourceResponse(savedResource, user.getId());
    }

    private ResourceResponseRequest updateResourceFields(Resource resource, UpdateResourceRequest request) {
        boolean titleChanged = false;

        if (request.getTitle() != null && !request.getTitle().equals(resource.getTitle())) {
            resource.setTitle(request.getTitle());
            titleChanged = true;
        }

        if (request.getDescription() != null) resource.setDescription(request.getDescription());
        if (request.getContactInfo() != null) resource.setContactInfo(request.getContactInfo());
        if (request.getAvailability() != null) resource.setAvailability(request.getAvailability());

        if (request.getCity() != null || request.getStreet() != null || request.getPostalCode() != null) {
            ResourceLocation location = resource.getLocation();
            if (location == null) {
                location = new ResourceLocation();
                resource.setLocation(location);
            }

            if (request.getCity() != null) location.setCity(request.getCity());
            if (request.getStreet() != null) location.setStreet(request.getStreet());
            if (request.getNeighborhood() != null) location.setNeighborhood(request.getNeighborhood());
            if (request.getPostalCode() != null) location.setPostalCode(request.getPostalCode());
            if (request.getProvince() != null) location.setProvince(request.getProvince());
            if (request.getCountry() != null) location.setCountry(request.getCountry());
        }

        if (request.getCategory() != null) {
            Category category = resolveCategoryByName(request.getCategory());
            resource.setCategory(category);
        }

        if (request.getCost() != null) {
            resource.setCost(parseCostType(request.getCost()));
        }

        if (request.getRequirements() != null) resource.setRequirements(request.getRequirements());
        if (request.getAdditionalNotes() != null) resource.setAdditionalNotes(request.getAdditionalNotes());
        if (request.getCapacity() != null) resource.setCapacity(request.getCapacity());
        if (request.getWheelchairAccessible() != null) resource.setWheelchairAccessible(request.getWheelchairAccessible());
        if (request.getLanguages() != null) resource.setLanguages(request.getLanguages());
        if (request.getTargetAudience() != null) resource.setTargetAudience(request.getTargetAudience());
        if (request.getImageUrl() != null) {
            // Convertir List<String> a String separado por comas
            String imageUrlString = String.join(",", request.getImageUrl());
            resource.setImageUrl(Collections.singletonList(imageUrlString));
        }

        if (request.getWebsiteUrl() != null) resource.setWebsiteUrl(request.getWebsiteUrl());

        if (titleChanged) {
            resource.generateSlug();
            String uniqueSlug = ensureUniqueSlug(resource.getSlug());
            resource.setSlug(uniqueSlug);
        }

        Resource updated = resourceRepository.save(resource);
        Long currentUserId = getCurrentUserId();
        return mapToResourceResponse(updated, currentUserId);
    }

    private void validateResourceOwnership(Resource resource, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (!resource.getUser().getId().equals(userId) && user.getRole() != User.Role.ADMIN) {
            throw new ForbiddenException("You are not allowed to perform this action on this resource");
        }
    }

    private Category resolveCategoryByName(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty");
        }

        return categoryRepository.findByNameIgnoreCase(categoryName)
                .orElseGet(() -> {
                    log.info("Creating new category: {}", categoryName);
                    Category newCategory = new Category();
                    newCategory.setName(categoryName);
                    newCategory.setDescription("Auto-created category");
                    return categoryRepository.save(newCategory);
                });
    }

    private Resource.CostType parseCostType(String cost) {
        try {
            return Resource.CostType.valueOf(cost.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid cost type: " + cost +
                    ". Valid values: FREE, LOW_COST, AFFORDABLE");
        }
    }

    private String ensureUniqueSlug(String baseSlug) {
        String slug = baseSlug;
        int counter = 1;

        while (resourceRepository.findBySlug(slug).isPresent()) {
            slug = baseSlug + "-" + counter;
            counter++;

            if (counter > 100) {
                throw new IllegalStateException("Unable to generate unique slug");
            }
        }

        return slug;
    }

    /**
     * Get current authenticated user ID
     * Returns null if no user is authenticated
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() ||
                    "anonymousUser".equals(authentication.getPrincipal())) {
                return null;
            }

            String userEmail = authentication.getName();
            return userRepository.findByEmail(userEmail)
                    .map(User::getId)
                    .orElse(null);
        } catch (Exception e) {
            log.debug("Could not get current user ID: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Map Resource to ResourceResponseRequest WITHOUT user context
     * Used when user context is not needed (admin views, etc.)
     */
    private ResourceResponseRequest mapToResourceResponse(Resource resource) {
        return mapToResourceResponse(resource, null);
    }

    /**
     * Map Resource to ResourceResponseRequest WITH user context
     * Includes isSaved flag based on current user
     */
    private ResourceResponseRequest mapToResourceResponse(Resource resource, Long currentUserId) {
        Double avgRating = ratingRepository.findAverageRatingByResourceId(resource.getId());
        Long totalRatings = ratingRepository.countByResourceId(resource.getId());

        // Get unique views count
        Long uniqueViews = resourceViewRepository.countUniqueViewsByResourceId(resource.getId());

        // Check if saved by current user
        Boolean isSaved = false;
        if (currentUserId != null) {
            isSaved = savedResourceRepository.existsByUserIdAndResourceId(currentUserId, resource.getId());
        }

        // Build user info
        ResourceResponseRequest.UserBasicInfo userInfo = ResourceResponseRequest.UserBasicInfo.builder()
                .id(resource.getUser().getId())
                .username(resource.getUser().getUsername())
                .organizationName(resource.getUser().getOrganizationName())
                .verified(resource.getUser().getVerified())
                .type(resource.getUser().getType().toString())
                .build();

        // Extract phone and email from contactInfo
        String phone = null;
        String email = null;
        if (resource.getContactInfo() != null) {
            String[] parts = resource.getContactInfo().split(",");
            for (String part : parts) {
                part = part.trim();
                if (part.matches(".*@.*\\..*")) {
                    email = part;
                } else if (part.matches(".*\\d{3}.*")) {
                    phone = part;
                }
            }
        }

        // Prepare cost field: return additional_notes or "VARIES" if empty
        String costValue = resource.getAdditionalNotes();
        if (costValue == null || costValue.trim().isEmpty()) {
            costValue = "VARIES";
        } else {
            // Remove [COST_DETAILS] prefix if present
            costValue = costValue.replaceFirst("^\\s*\\[COST_DETAILS\\]\\s*", "").trim();
            
            // If after removing the prefix the string is empty, use "VARIES"
            if (costValue.isEmpty()) {
                costValue = "VARIES";
            }
        }


        // Parse imageUrl: get the list directly from the entity
        List<String> imageUrls = new ArrayList<>();
        if (resource.getImageUrl() != null && !resource.getImageUrl().isEmpty()) {
            // Filter out empty or null URLs
            imageUrls = resource.getImageUrl().stream()
                    .filter(url -> url != null && !url.trim().isEmpty())
                    .map(String::trim)
                    .limit(10)
                    .toList();
        }

        return ResourceResponseRequest.builder()
                .id(resource.getId())
                .title(resource.getTitle())
                .description(resource.getDescription())
                .slug(resource.getSlug())
                .categoryName(resource.getCategory() != null ? resource.getCategory().getName() : null)
                .city(resource.getLocation() != null ? resource.getLocation().getCity() : null)
                .postalCode(resource.getLocation() != null ? resource.getLocation().getPostalCode() : null)
                .fullAddress(resource.getFullAddress())
                .cost(costValue)
                .status(resource.getStatus())
                .viewsCount((int) (uniqueViews != null ? uniqueViews : 0L)) // Unique views instead of total
                .averageRating(avgRating != null ? avgRating : 0.0)
                .totalRatings(totalRatings != null ? totalRatings : 0L)
                .requirements(resource.getRequirements())
                .capacity(resource.getCapacity())
                .wheelchairAccessible(resource.getWheelchairAccessible())
                .languages(resource.getLanguages())
                .targetAudience(resource.getTargetAudience())
                .contactInfo(resource.getContactInfo())
                .phone(phone)
                .email(email)
                .availability(resource.getAvailability())
                .imageUrl(imageUrls)
                .websiteUrl(resource.getWebsiteUrl())
                .user(userInfo)
                .isSaved(isSaved)
                .createdAt(resource.getCreatedAt())
                .updatedAt(resource.getUpdatedAt())
                .build();
    }
}
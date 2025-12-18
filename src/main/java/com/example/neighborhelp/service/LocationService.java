package com.example.neighborhelp.service;

import com.example.neighborhelp.controller.LocationController.LocationStatistics;
import com.example.neighborhelp.dto.LocationsDto.CityResponse;
import com.example.neighborhelp.dto.LocationsDto.PostalCodeResponse;
import com.example.neighborhelp.entity.Resource;
import com.example.neighborhelp.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for extracting location data from resources
 *
 * Since locations are now embedded in resources, this service
 * extracts unique cities and postal codes from existing active resources.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private final ResourceRepository resourceRepository;

    /**
     * Get all unique cities from active resources
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "cities", key = "'all'")
    public List<CityResponse> getAllCities() {
        log.debug("Fetching all unique cities");

        List<Resource> resources = resourceRepository.findByStatus(Resource.ResourceStatus.ACTIVE);

        // Group by city and count resources per city
        Map<String, Long> cityCounts = resources.stream()
                .filter(r -> r.getLocation() != null && r.getLocation().getCity() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getLocation().getCity(),
                        Collectors.counting()
                ));

        // Convert to response DTOs and sort alphabetically
        return cityCounts.entrySet().stream()
                .map(entry -> CityResponse.builder()
                        .city(entry.getKey())
                        .resourceCount(entry.getValue())
                        .build())
                .sorted((c1, c2) -> c1.getCity().compareToIgnoreCase(c2.getCity()))
                .collect(Collectors.toList());
    }

    /**
     * Get all unique postal codes from active resources
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "postalCodes", key = "'all'")
    public List<PostalCodeResponse> getAllPostalCodes() {
        log.debug("Fetching all unique postal codes");

        List<Resource> resources = resourceRepository.findByStatus(Resource.ResourceStatus.ACTIVE);

        // Group by postal code and count resources
        Map<String, Long> postalCodeCounts = resources.stream()
                .filter(r -> r.getLocation() != null && r.getLocation().getPostalCode() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getLocation().getPostalCode(),
                        Collectors.counting()
                ));

        // Get city for each postal code (first occurrence)
        Map<String, String> postalCodeCities = resources.stream()
                .filter(r -> r.getLocation() != null &&
                        r.getLocation().getPostalCode() != null &&
                        r.getLocation().getCity() != null)
                .collect(Collectors.toMap(
                        r -> r.getLocation().getPostalCode(),
                        r -> r.getLocation().getCity(),
                        (existing, replacement) -> existing // Keep first
                ));

        // Convert to response DTOs and sort by postal code
        return postalCodeCounts.entrySet().stream()
                .map(entry -> PostalCodeResponse.builder()
                        .postalCode(entry.getKey())
                        .city(postalCodeCities.get(entry.getKey()))
                        .resourceCount(entry.getValue())
                        .build())
                .sorted((p1, p2) -> p1.getPostalCode().compareTo(p2.getPostalCode()))
                .collect(Collectors.toList());
    }

    /**
     * Search cities by name (partial match, case-insensitive)
     */
    @Transactional(readOnly = true)
    public List<CityResponse> searchCities(String searchTerm) {
        log.debug("Searching cities with term: {}", searchTerm);

        return getAllCities().stream()
                .filter(city -> city.getCity().toLowerCase().contains(searchTerm.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Get all postal codes for a specific city
     */
    @Transactional(readOnly = true)
    public List<PostalCodeResponse> getPostalCodesForCity(String city) {
        log.debug("Fetching postal codes for city: {}", city);

        List<Resource> resources = resourceRepository.findByStatus(Resource.ResourceStatus.ACTIVE);

        // Filter resources by city and group by postal code
        Map<String, Long> postalCodeCounts = resources.stream()
                .filter(r -> r.getLocation() != null &&
                        r.getLocation().getCity() != null &&
                        r.getLocation().getCity().equalsIgnoreCase(city) &&
                        r.getLocation().getPostalCode() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getLocation().getPostalCode(),
                        Collectors.counting()
                ));

        return postalCodeCounts.entrySet().stream()
                .map(entry -> PostalCodeResponse.builder()
                        .postalCode(entry.getKey())
                        .city(city)
                        .resourceCount(entry.getValue())
                        .build())
                .sorted((p1, p2) -> p1.getPostalCode().compareTo(p2.getPostalCode()))
                .collect(Collectors.toList());
    }

    /**
     * Search postal codes (partial match)
     */
    @Transactional(readOnly = true)
    public List<PostalCodeResponse> searchPostalCodes(String searchTerm) {
        log.debug("Searching postal codes with term: {}", searchTerm);

        return getAllPostalCodes().stream()
                .filter(pc -> pc.getPostalCode().contains(searchTerm))
                .collect(Collectors.toList());
    }

    /**
     * Get location statistics
     */
    @Transactional(readOnly = true)
    public LocationStatistics getLocationStatistics() {
        log.debug("Fetching location statistics");

        List<CityResponse> cities = getAllCities();
        List<PostalCodeResponse> postalCodes = getAllPostalCodes();
        long totalResources = resourceRepository.countByStatus(Resource.ResourceStatus.ACTIVE);

        return new LocationStatistics(
                cities.size(),
                postalCodes.size(),
                totalResources
        );
    }
}
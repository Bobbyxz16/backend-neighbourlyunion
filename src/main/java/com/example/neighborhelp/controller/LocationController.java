package com.example.neighborhelp.controller;

import com.example.neighborhelp.dto.LocationsDto.CityResponse;
import com.example.neighborhelp.dto.LocationsDto.PostalCodeResponse;
import com.example.neighborhelp.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Location Controller - Provides location data from existing resources
 *
 * Since locations are now embedded in resources, these endpoints
 * extract unique cities and postal codes from existing resources
 * to help users discover available locations.
 *
 * Endpoints:
 * - GET /api/locations/cities - List all unique cities
 * - GET /api/locations/postal-codes - List all unique postal codes
 * - GET /api/locations/search/cities - Search cities by name
 * - GET /api/locations/cities/{city}/postal-codes - Get postal codes for a city
 */
@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    /**
     * GET /api/locations/cities
     * Get all unique cities from resources
     *
     * Public endpoint with caching
     * Returns cities sorted alphabetically
     */
    @GetMapping(value = "/cities", produces = MediaType.APPLICATION_JSON_VALUE)
    @Cacheable(value = "cities", key = "'all'")
    public ResponseEntity<List<CityResponse>> getAllCities() {
        List<CityResponse> cities = locationService.getAllCities();
        return ResponseEntity.ok(cities);
    }

    /**
     * GET /api/locations/postal-codes
     * Get all unique postal codes from resources
     *
     * Public endpoint with caching
     * Returns postal codes sorted numerically
     */
    @GetMapping(value = "/postal-codes", produces = MediaType.APPLICATION_JSON_VALUE)
    @Cacheable(value = "postalCodes", key = "'all'")
    public ResponseEntity<List<PostalCodeResponse>> getAllPostalCodes() {
        List<PostalCodeResponse> postalCodes = locationService.getAllPostalCodes();
        return ResponseEntity.ok(postalCodes);
    }

    /**
     * GET /api/locations/search/cities
     * Search cities by name (partial match, case-insensitive)
     *
     * Query param: name - search term
     * Public endpoint
     */
    @GetMapping(value = "/search/cities", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CityResponse>> searchCities(
            @RequestParam String name) {
        List<CityResponse> cities = locationService.searchCities(name);
        return ResponseEntity.ok(cities);
    }

    /**
     * GET /api/locations/cities/{city}/postal-codes
     * Get all postal codes for a specific city
     *
     * Public endpoint
     */
    @GetMapping(value = "/cities/{city}/postal-codes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PostalCodeResponse>> getPostalCodesForCity(
            @PathVariable String city) {
        List<PostalCodeResponse> postalCodes = locationService.getPostalCodesForCity(city);
        return ResponseEntity.ok(postalCodes);
    }

    /**
     * GET /api/locations/search/postal-codes
     * Search postal codes (partial match)
     *
     * Query param: code - search term
     * Public endpoint
     */
    @GetMapping(value = "/search/postal-codes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PostalCodeResponse>> searchPostalCodes(
            @RequestParam String code) {
        List<PostalCodeResponse> postalCodes = locationService.searchPostalCodes(code);
        return ResponseEntity.ok(postalCodes);
    }

    /**
     * GET /api/locations/statistics
     * Get location statistics
     *
     * Public endpoint
     * Returns total unique cities and postal codes
     */
    @GetMapping(value = "/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LocationStatistics> getLocationStatistics() {
        LocationStatistics stats = locationService.getLocationStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Location statistics DTO
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class LocationStatistics {
        private long totalCities;
        private long totalPostalCodes;
        private long totalResources;
    }
}
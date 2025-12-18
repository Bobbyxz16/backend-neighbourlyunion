package com.example.neighborhelp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embeddable location information for resources
 *
 * Stores complete address details directly in the resource table
 * No need for separate Location entity and joins
 */
@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceLocation {

    /**
     * City name (required)
     * e.g., "Madrid", "Barcelona", "Valencia"
     */
    @Column(name = "location_city", nullable = false, length = 100)
    private String city;

    /**
     * Street address (required)
     * e.g., "Calle Gran Vía 123, Piso 2B"
     */
    @Column(name = "location_street", nullable = false, length = 255)
    private String street;

    /**
     * Neighborhood/District (optional)
     * e.g., "Malasaña", "Chamberí", "El Raval"
     */
    @Column(name = "location_neighborhood", length = 100)
    private String neighborhood;

    /**
     * Postal code (required)
     * e.g., "28001", "08001"
     */
    @Column(name = "location_postal_code", nullable = false, length = 10)
    private String postalCode;

    /**
     * Province/State (optional)
     * e.g., "Madrid", "Barcelona", "Valencia"
     */
    @Column(name = "location_province", length = 100)
    private String province;

    /**
     * Country (optional, defaults to España)
     * e.g., "España", "Portugal"
     */
    @Column(name = "location_country", length = 50)
    private String country = "España";

    /**
     * Latitude for map integration (optional)
     */
    @Column(name = "location_latitude")
    private Double latitude;

    /**
     * Longitude for map integration (optional)
     */
    @Column(name = "location_longitude")
    private Double longitude;

    // ========== UTILITY METHODS ==========

    /**
     * Get complete address as single formatted string
     */
    public String getFullAddress() {
        StringBuilder address = new StringBuilder();

        if (street != null && !street.isEmpty()) {
            address.append(street);
        }

        if (neighborhood != null && !neighborhood.isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(neighborhood);
        }

        if (postalCode != null && !postalCode.isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(postalCode);
        }

        if (city != null && !city.isEmpty()) {
            if (address.length() > 0) address.append(" ");
            address.append(city);
        }

        if (province != null && !province.isEmpty() && !province.equals(city)) {
            if (address.length() > 0) address.append(", ");
            address.append(province);
        }

        if (country != null && !country.isEmpty() && !country.equals("España")) {
            if (address.length() > 0) address.append(", ");
            address.append(country);
        }

        return address.toString();
    }

    /**
     * Get short address (street, postal code, city)
     */
    public String getShortAddress() {
        StringBuilder address = new StringBuilder();

        if (street != null && !street.isEmpty()) {
            address.append(street);
        }

        if (postalCode != null && !postalCode.isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(postalCode);
        }

        if (city != null && !city.isEmpty()) {
            if (address.length() > 0) address.append(" ");
            address.append(city);
        }

        return address.toString();
    }

    /**
     * Check if location has coordinates for mapping
     */
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
}
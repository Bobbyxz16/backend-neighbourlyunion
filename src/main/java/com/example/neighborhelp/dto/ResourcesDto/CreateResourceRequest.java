package com.example.neighborhelp.dto.ResourcesDto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;
import java.util.List;

/**
 * DTO for creating a new resource with complete information
 *
 * Users provide human-readable data instead of IDs:
 * - Category name instead of categoryId
 * - Full address instead of locationId
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateResourceRequest {

    // ========== BASIC INFORMATION ==========

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;

    // ========== CATEGORY ==========

    @JsonProperty("category")  // Nombre por defecto
    @JsonAlias({"categoryName", "category"})  // ✅ Acepta ambos nombres
    private String category; // e.g., "Alimentos", "Salud", "Educación"


    // ========== LOCATION ==========

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City name must not exceed 100 characters")
    private String city; // e.g., "Madrid", "Barcelona"

    @NotBlank(message = "Street address is required")
    @Size(max = 255, message = "Street address must not exceed 255 characters")
    private String street; // e.g., "Calle Gran Vía 123"

    @Size(max = 100, message = "Neighborhood must not exceed 100 characters")
    private String neighborhood; // Optional: e.g., "Malasaña", "Chamberí"

    @NotBlank(message = "Postal code is required")
    @Pattern(regexp = "\\d{5}", message = "Postal code must be 5 digits")
    private String postalCode; // e.g., "28001"

    @Size(max = 100, message = "Province must not exceed 100 characters")
    private String province; // Optional: e.g., "Madrid", "Barcelona"

    @Size(max = 50, message = "Country must not exceed 50 characters")
    private String country; // Optional, default: "España"

    // ========== CONTACT & AVAILABILITY ==========

    @NotBlank(message = "Contact information is required")
    @Size(max = 500, message = "Contact info must not exceed 500 characters")
    private String contactInfo; // e.g., "Tel: 612 345 678, Email: maria@email.com"

    @Size(max = 500, message = "Availability info must not exceed 500 characters")
    private String availability; // e.g., "Lunes a Viernes 9:00-18:00"

    // ========== COST ==========

    @NotBlank(message = "Cost type is required")
    @Pattern(regexp = "FREE|LOW_COST|AFFORDABLE",
            message = "Cost must be one of: FREE, LOW_COST, AFFORDABLE")
    private String cost;

    // ========== ADDITIONAL DETAILS ==========

    @Size(max = 500, message = "Requirements must not exceed 500 characters")
    private String requirements; // e.g., "Documento de identidad, comprobante de domicilio"

    @Size(max = 1000, message = "Additional notes must not exceed 1000 characters")
    private String additionalNotes; // Any extra information

    @Min(value = 0, message = "Capacity must be a positive number")
    private Integer capacity; // e.g., Number of people that can be served

    private Boolean wheelchairAccessible; // Accessibility info

    @Size(max = 200, message = "Languages must not exceed 200 characters")
    private String languages; // e.g., "Español, Inglés, Árabe"

    @Size(max = 500, message = "Target audience must not exceed 500 characters")
    private String targetAudience; // e.g., "Familias con niños", "Personas mayores"

    // ========== OPTIONAL MEDIA ==========

    private List<String> imageUrl; // URL to resource image/photo

    @Pattern(regexp = "^(https?://)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([/\\w \\.-]*)*/?$",
            message = "Invalid website URL")
    private String websiteUrl; // Link to more information
}
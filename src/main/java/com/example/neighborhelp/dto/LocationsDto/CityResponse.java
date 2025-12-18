package com.example.neighborhelp.dto.LocationsDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ==================== CITY RESPONSE ====================

/**
 * DTO for city information extracted from resources
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CityResponse {

    private String city;           // City name
    private Long resourceCount;    // Number of resources in this city
}
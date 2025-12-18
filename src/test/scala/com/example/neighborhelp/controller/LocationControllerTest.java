package com.example.neighborhelp.controller;

import com.example.neighborhelp.config.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Location Controller Integration Tests")
class LocationControllerTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should get all cities")
    void shouldGetAllCities() throws Exception {
        mockMvc.perform(get("/api/locations/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Should get all postal codes")
    void shouldGetAllPostalCodes() throws Exception {
        mockMvc.perform(get("/api/locations/postal-codes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Should search cities")
    void shouldSearchCities() throws Exception {
        mockMvc.perform(get("/api/locations/search/cities")
                        .param("name", "mad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Should get location statistics")
    void shouldGetStatistics() throws Exception {
        mockMvc.perform(get("/api/locations/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCities").exists())
                .andExpect(jsonPath("$.totalPostalCodes").exists())
                .andExpect(jsonPath("$.totalResources").exists());
    }
}

package com.example.neighborhelp.controller;

import com.example.neighborhelp.config.BaseIntegrationTest;
import com.example.neighborhelp.dto.CategoriesDto.CategoryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Category Controller Integration Tests")
class CategoryControllerTest extends BaseIntegrationTest {

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        createAdminUser();
        adminToken = getAuthToken("admin@test.com", "TestPass123!");

        // Create some test categories
        createTestCategory("Food");
        createTestCategory("Health");
        createTestCategory("Education");
    }

    @Test
    @DisplayName("Should get all categories")
    void shouldGetAllCategories() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    @DisplayName("Should get category by ID")
    void shouldGetCategoryById() throws Exception {
        Long categoryId = categoryRepository.findAll().get(0).getId();

        mockMvc.perform(get("/api/categories/" + categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoryId))
                .andExpect(jsonPath("$.name").exists());
    }

    @Test
    @DisplayName("Should search categories by name")
    void shouldSearchCategories() throws Exception {
        mockMvc.perform(get("/api/categories/search")
                        .param("name", "foo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Food"));
    }

    @Test
    @DisplayName("Admin should create category")
    void shouldCreateCategory() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("Pet Care");
        request.setDescription("Pet care services");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Pet Care"));
    }

    @Test
    @DisplayName("Should fail to create duplicate category")
    void shouldFailDuplicateCategory() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("Food"); // Already exists
        request.setDescription("Duplicate");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Non-admin should fail to create category")
    void shouldFailWithoutAdminRole() throws Exception {
        createTestUser("user", "user@test.com", com.example.neighborhelp.entity.User.UserType.INDIVIDUAL);
        String userToken = getAuthToken("user@test.com", "TestPass123!");

        CategoryRequest request = new CategoryRequest();
        request.setName("Test");
        request.setDescription("Test");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}

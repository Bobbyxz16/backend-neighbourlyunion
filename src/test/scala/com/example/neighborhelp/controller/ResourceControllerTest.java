package com.example.neighborhelp.controller;

import com.example.neighborhelp.config.BaseIntegrationTest;
import com.example.neighborhelp.dto.ResourcesDto.CreateResourceRequest;
import com.example.neighborhelp.entity.Category;
import com.example.neighborhelp.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Resource Controller Integration Tests")
class ResourceControllerTest extends BaseIntegrationTest {

    private String userToken;
    private String adminToken;
    private Category foodCategory;

    @BeforeEach
    void setUp() throws Exception {
        // Create test data
        User user = createTestUser("testuser", "test@test.com", User.UserType.INDIVIDUAL);
        User admin = createAdminUser();
        foodCategory = createTestCategory("Food");

        userToken = getAuthToken("test@test.com", "TestPass123!");
        adminToken = getAuthToken("admin@test.com", "TestPass123!");
    }

    @Test
    @DisplayName("Should create resource successfully")
    void shouldCreateResource() throws Exception {
        CreateResourceRequest request = new CreateResourceRequest();
        request.setTitle("Banco de Alimentos Test");
        request.setDescription("Test description with minimum 10 characters");
        request.setCategory("Food");
        request.setCity("Madrid");
        request.setStreet("Calle Test 123");
        request.setPostalCode("28001");
        request.setContactInfo("test@contact.com");
        request.setAvailability("Monday to Friday");
        request.setCost("FREE");

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Banco de Alimentos Test"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.slug").exists())
                .andExpect(jsonPath("$.city").value("Madrid"));
    }

    @Test
    @DisplayName("Should get resource by slug")
    void shouldGetResourceBySlug() throws Exception {
        // Create resource first
        CreateResourceRequest request = new CreateResourceRequest();
        request.setTitle("Test Resource for Slug");
        request.setDescription("Description for testing slug retrieval");
        request.setCategory("Food");
        request.setCity("Barcelona");
        request.setStreet("Test Street 456");
        request.setPostalCode("08001");
        request.setContactInfo("contact@test.com");
        request.setAvailability("24/7");
        request.setCost("FREE");

        String createResponse = mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String slug = objectMapper.readTree(createResponse).get("slug").asText();

        // Get by slug (public endpoint)
        mockMvc.perform(get("/api/resources/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(slug))
                .andExpect(jsonPath("$.viewsCount").value(1)); // Should increment
    }

    @Test
    @DisplayName("Should search resources with filters")
    void shouldSearchResources() throws Exception {
        mockMvc.perform(get("/api/resources")
                        .param("city", "Madrid")
                        .param("cost", "FREE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Should get user resources")
    void shouldGetUserResources() throws Exception {
        mockMvc.perform(get("/api/resources/my-resources")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Admin should approve pending resource")
    void shouldApproveResource() throws Exception {
        // Create resource
        CreateResourceRequest request = new CreateResourceRequest();
        request.setTitle("Pending Resource");
        request.setDescription("This resource needs approval");
        request.setCategory("Food");
        request.setCity("Valencia");
        request.setStreet("Street 789");
        request.setPostalCode("46001");
        request.setContactInfo("pending@test.com");
        request.setCost("FREE");

        String createResponse = mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getContentAsString();

        String slug = objectMapper.readTree(createResponse).get("slug").asText();

        // Admin approves
        mockMvc.perform(patch("/api/resources/" + slug + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should fail to create resource without auth")
    void shouldFailWithoutAuth() throws Exception {
        CreateResourceRequest request = new CreateResourceRequest();
        request.setTitle("Test");
        request.setDescription("Test description");
        request.setCategory("Food");
        request.setCity("Madrid");
        request.setStreet("Street");
        request.setPostalCode("28001");
        request.setCost("FREE");

        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}

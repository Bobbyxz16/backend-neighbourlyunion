package com.example.neighborhelp.controller;

import com.example.neighborhelp.config.BaseIntegrationTest;
import com.example.neighborhelp.dto.RegisterRequest;
import com.example.neighborhelp.dto.UpdateUserRequest;
import com.example.neighborhelp.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("User Controller Integration Tests")
class UserControllerTest extends BaseIntegrationTest {

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should register individual user successfully")
    void shouldRegisterIndividualUser() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("maria_garcia");
        request.setEmail("maria@test.com");
        request.setPassword("SecurePass123!");
        request.setFirstName("María");
        request.setLastName("García");
        request.setType(User.UserType.INDIVIDUAL);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("maria_garcia"))
                .andExpect(jsonPath("$.email").value("maria@test.com"))
                .andExpect(jsonPath("$.type").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.firstName").value("María"))
                .andExpect(jsonPath("$.verified").value(false));
    }

    @Test
    @DisplayName("Should register organization successfully")
    void shouldRegisterOrganization() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("ayuda_vecinal");
        request.setEmail("ayuda@test.com");
        request.setPassword("OrgPass123!");
        request.setOrganizationName("Ayuda Vecinal Madrid");
        request.setType(User.UserType.ORGANIZATION);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationName").value("Ayuda Vecinal Madrid"))
                .andExpect(jsonPath("$.type").value("ORGANIZATION"));
    }

    @Test
    @DisplayName("Should fail registration with duplicate email")
    void shouldFailWithDuplicateEmail() throws Exception {
        // Create first user
        User existingUser = createTestUser("user1", "duplicate@test.com", User.UserType.INDIVIDUAL);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("user2_" + System.currentTimeMillis());
        request.setEmail(existingUser.getEmail()); // Use same email
        request.setPassword("Pass123!");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setType(User.UserType.INDIVIDUAL);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should login successfully")
    void shouldLoginSuccessfully() throws Exception {
        createTestUser("testuser", "test@test.com", User.UserType.INDIVIDUAL);

        String loginRequest = """
            {
                "email": "test@test.com",
                "password": "TestPass123!"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("Should get own profile with email")
    void shouldGetOwnProfile() throws Exception {
        User user = createTestUser("testuser", "test@test.com", User.UserType.INDIVIDUAL);
        String token = getAuthToken(user.getEmail(), "TestPass123!");

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.username").value(user.getUsername()));
    }

    @Test
    @DisplayName("Should get public profile without email")
    void shouldGetPublicProfileWithoutEmail() throws Exception {
        User user = createTestUser("publicuser", "public@test.com", User.UserType.INDIVIDUAL);

        mockMvc.perform(get("/api/users/publicuser")
                        .param("email", user.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(user.getUsername()))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    @DisplayName("Should update own profile")
    void shouldUpdateOwnProfile() throws Exception {
        createTestUser("testuser", "test@test.com", User.UserType.INDIVIDUAL);
        String token = getAuthToken("test@test.com", "TestPass123!");

        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Name");

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.lastName").value("Name"));
    }

    @Test
    @DisplayName("Should delete own account")
    void shouldDeleteOwnAccount() throws Exception {
        createTestUser("testuser", "test@test.com", User.UserType.INDIVIDUAL);
        String token = getAuthToken("test@test.com", "TestPass123!");

        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should fail to access protected endpoint without token")
    void shouldFailWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }
}


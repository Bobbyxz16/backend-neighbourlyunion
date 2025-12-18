package com.example.neighborhelp.config;

import com.example.neighborhelp.entity.Category;
import com.example.neighborhelp.entity.User;
import com.example.neighborhelp.repository.CategoryRepository;
import com.example.neighborhelp.repository.RefreshTokenRepository;
import com.example.neighborhelp.repository.UserRepository;
import com.example.neighborhelp.repository.ResourceRepository;
import com.example.neighborhelp.repository.VerificationCodeRepository;
import com.example.neighborhelp.repository.ReportRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for all integration tests
 * Provides common setup and utilities
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected CategoryRepository categoryRepository;

    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;

    @Autowired
    protected VerificationCodeRepository verificationCodeRepository;

    @Autowired
    protected ResourceRepository resourceRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected ReportRepository reportRepository;

    /**
     * Clean up all test data before each test
     */
    @BeforeEach
    void cleanUp() {
        try {
            // Check if verification_codes table exists by trying a simple operation
            verificationCodeRepository.count();

            // If we get here, tables exist - proceed with cleanup
            verificationCodeRepository.deleteAll();
            userRepository.deleteAll();
            resourceRepository.deleteAll();
            reportRepository.deleteAll();
            // ... other repositories
        } catch (Exception e) {
            // Tables don't exist yet - this is normal for first test run
            // Hibernate will create them automatically
            System.out.println("Tables not initialized yet, skipping cleanup...");
        }
    }

    /**
     * Create a test user with unique values
     */
    protected User createTestUser(String username, String email, User.UserType type) {
        // Ensure unique username and email for each test
        String uniqueUsername = username + "_" + System.currentTimeMillis();
        String uniqueEmail = email.replace("@", "_" + System.currentTimeMillis() + "@");

        User user = new User();
        user.setUsername(uniqueUsername);
        user.setEmail(uniqueEmail);
        user.setPassword(passwordEncoder.encode("TestPass123!"));
        user.setType(type);
        user.setVerified(true);
        user.setEnabled(true);
        user.setRole(User.Role.USER);
        user.setAuthProvider("LOCAL");

        if (type == User.UserType.INDIVIDUAL) {
            user.setFirstName("Test");
            user.setLastName("User");
        } else {
            user.setOrganizationName(uniqueUsername + " Organization");
        }

        return userRepository.save(user);
    }

    /**
     * Create an admin user with unique email
     */
    protected User createAdminUser() {
        String uniqueEmail = "admin_" + System.currentTimeMillis() + "@test.com";
        User admin = new User();
        admin.setUsername("admin_" + System.currentTimeMillis());
        admin.setEmail(uniqueEmail);
        admin.setPassword(passwordEncoder.encode("TestPass123!"));
        admin.setType(User.UserType.INDIVIDUAL);
        admin.setVerified(true);
        admin.setEnabled(true);
        admin.setRole(User.Role.ADMIN);
        admin.setAuthProvider("LOCAL");
        admin.setFirstName("Admin");
        admin.setLastName("User");

        return userRepository.save(admin);
    }

    /**
     * Create a test category with unique name
     */
    protected Category createTestCategory(String name) {
        String uniqueName = name + "_" + System.currentTimeMillis();
        Category category = new Category();
        category.setName(uniqueName);
        category.setDescription("Test " + uniqueName);
        return categoryRepository.save(category);
    }

    /**
     * Login and get JWT token
     */
    protected String getAuthToken(String email, String password) throws Exception {
        String loginRequest = String.format("""
            {
                "email": "%s",
                "password": "%s"
            }
            """, email, password);

        String response = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/auth/login")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(loginRequest))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
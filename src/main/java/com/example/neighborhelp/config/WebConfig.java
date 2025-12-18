package com.example.neighborhelp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web configuration class for CORS and MVC settings.
 *
 * Configures Cross-Origin Resource Sharing (CORS) for the application
 * to allow controlled access from specific frontend domains and development servers.
 * Also configures static resource handling for uploaded files.
 *
 * @author NeighborHelp Team
 */

@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class WebConfig {

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * Configures CORS settings for the application.
     *
     * Defines which origins are allowed to access the API endpoints and
     * what HTTP methods and headers are permitted.
     *
     * Configuration details:
     * - Applies to all endpoints under "/api/**" path
     * - Allows specific production and development origins
     * - Permits common HTTP methods (GET, POST, PUT, DELETE, OPTIONS)
     * - Allows all headers
     * - Enables credential sharing (cookies, authorization headers)
     *
     * @return WebMvcConfigurer with CORS configuration
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Add global mapping first
                registry.addMapping("/**")
                        .allowedOrigins(
                                "https://neighborlyunion.com",
                                "https://www.neighborlyunion.com",
                                "http://localhost:3000",
                                "http://localhost:5500",
                                "http://localhost:5173"  // Added Vite default port
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);

                // Keep your specific API mapping too
                registry.addMapping("/api/**")
                        .allowedOrigins(
                                "https://neighborlyunion.com",
                                "https://www.neighborlyunion.com",
                                "http://localhost:3000",
                                "http://localhost:5500",
                                "http://localhost:5173"  // Added Vite default port
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }

            /**
             * Configures static resource handlers for serving uploaded files.
             *
             * Maps the /uploads/** URL pattern to the physical upload directory,
             * allowing the frontend to fetch uploaded images directly.
             *
             * @param registry ResourceHandlerRegistry to configure
             */
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                // Convert relative path to absolute path
                String uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize().toString();

                // Serve uploaded files from /uploads/** URL pattern
                registry.addResourceHandler("/uploads/**")
                        .addResourceLocations("file:///C:/Users/bobby/IdeaProjects/NeighborHelp/uploads/");
            }
        };
    }
}
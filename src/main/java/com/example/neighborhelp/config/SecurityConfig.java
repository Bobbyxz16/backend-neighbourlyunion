package com.example.neighborhelp.config;

import com.example.neighborhelp.security.JwtAuthenticationFilter;
import com.example.neighborhelp.security.JwtService;
import com.example.neighborhelp.security.OAuth2LoginSuccessHandler;
import com.example.neighborhelp.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    public SecurityConfig(UserDetailsService userDetailsService, JwtService jwtService,
                          UserRepository userRepository, OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ========== PUBLIC ENDPOINTS ==========
                        .requestMatchers(
                                "/",
                                "/health",
                                "/actuator/health",
                                "/api/auth/**",
                                "/api/users/{organizationName}",
                                "/api/users/{organizationName}/statistics/**",
                                "/api/resources/user/{userId}",
                                // OAuth2 endpoints
                                "/oauth2/**",
                                "/login/**",
                                "/oauth/**"
                        ).permitAll()

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ========== OAUTH2 ENDPOINTS ==========
                        .requestMatchers("/oauth2/authorization/**").permitAll()

                        // ========== PUBLIC GETs pero POSTs protegidos ==========
                        .requestMatchers(HttpMethod.GET, "/api/locations/**").permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/messages/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/messages/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/messages/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/messages/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/messages/inbox").authenticated()

                        .requestMatchers(HttpMethod.GET, "/api/resources").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/resources/{resourceName}").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/{id}/resources/count").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/resources/*/ratings").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/resources/*/ratings/summary").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/resources/*/ratings").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/ratings/*/helpful").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/resources/{resourceName}/ratings").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/ratings/{ratingId}/helpful").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/ratings/{ratingId}").hasRole("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/resources").hasAnyRole("USER", "ORGANIZATION", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/resources/{resourceName}").hasAnyRole("USER", "ORGANIZATION", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/resources/{resourceName}").hasAnyRole("USER", "ORGANIZATION", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/resources/{resourceName}/status").hasAnyRole("USER", "ORGANIZATION", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/resources/{resourceName}/permanent").hasAnyRole("USER", "ORGANIZATION", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/resources/{resourceName}/deactivate").hasAnyRole("USER", "ORGANIZATION", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/resources/{resourceName}/activate").hasAnyRole("USER", "ORGANIZATION", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/analytics/**").hasAnyRole("ADMIN", "MODERATOR")
                        .requestMatchers(HttpMethod.GET, "/api/moderation/**").hasAnyRole("ADMIN", "MODERATOR")
                        .requestMatchers(HttpMethod.POST, "/api/reports").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/reports").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/reports/**").hasAnyRole("ADMIN", "MODERATOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/reports/**").hasAnyRole("ADMIN", "MODERATOR")

                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/api/resources/upload").authenticated()
                        .requestMatchers("/api/resources/upload/multiple").authenticated()

                        // ========== AUTH REQUIRED (any role) ==========
                        .requestMatchers(
                                "/api/users/me",
                                "/api/profile/**",
                                "/api/resources/my-resources"
                        ).authenticated()

                        // ========== ADMIN & MODERATOR ONLY ==========
                        .requestMatchers(
                                "/api/users/{organizationName}/verify",
                                "/api/users/{organizationName}/status",
                                "/api/resources/pending",
                                "/api/resources/{resourceName}/approve",
                                "/api/resources/{resourceName}/reject",
                                "/api/resources/admin/status-overview"
                        ).hasAnyRole("ADMIN", "MODERATOR")

                        .requestMatchers(HttpMethod.PATCH,
                                "/api/resources/{resourceName}/approve",
                                "/api/resources/{resourceName}/reject"
                        ).hasAnyRole("ADMIN", "MODERATOR")

                        // ========== ADMIN ONLY ENDPOINTS ==========
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/{identifier}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/users/{identifier}/role").hasRole("ADMIN")

                        // ========== ALL OTHER ENDPOINTS REQUIRE AUTH ==========
                        .anyRequest().authenticated()
                )
                // Configure OAuth2 Login
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2LoginSuccessHandler)
                        .loginPage("/login")  // Optional: Custom login page
                )
                .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, authEx) -> {
                            res.setStatus(401);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Authentication required\"}");
                        })
                        .accessDeniedHandler((req, res, accessEx) -> {
                            res.setStatus(403);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"error\": \"Forbidden\", \"message\": \"Access denied\"}");
                        })
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthFilter() {
        return new JwtAuthenticationFilter(jwtService, userDetailsService);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:5500",
                "https://neighborlyunion.com",
                "https://www.neighborlyunion.com",
                "https://api.neighbourlyunion.com"
        ));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        configuration.setAllowedHeaders(Arrays.asList(
                "Origin", "X-Requested-With", "Content-Type", "Accept",
                "Authorization", "X-CSRF-TOKEN", "Access-Control-Allow-Origin"
        ));

        configuration.setExposedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "Access-Control-Allow-Origin"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
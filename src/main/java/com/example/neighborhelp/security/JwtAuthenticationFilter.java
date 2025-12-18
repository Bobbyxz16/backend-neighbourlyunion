package com.example.neighborhelp.security;

import com.example.neighborhelp.repository.UserRepository;
import com.example.neighborhelp.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Base64;

/**
 * JWT Authentication Filter for Spring Security.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            final String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            final String jwt = authHeader.substring(7).trim();

            if (jwt.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }

            // Validate token format first
            try {
                validateJwtFormat(jwt);
                validateAlgorithm(jwt);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid JWT format: " + e.getMessage());
                sendErrorResponse(response, "Invalid token format");
                return;
            }

            // Extract and validate user
            String userEmail = jwtService.extractEmail(jwt);
            if (userEmail == null) {
                logger.error("Could not extract email from token");
                sendErrorResponse(response, "Invalid token");
                return;
            }

            // Check if user is already authenticated
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.debug("✅ Authenticated user: " + userEmail);
                } else {
                    logger.warn("❌ Invalid token for user: " + userEmail);
                    sendErrorResponse(response, "Invalid token");
                    return;
                }
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            logger.error("❌ Authentication error: " + e.getMessage(), e);
            sendErrorResponse(response, "Authentication failed: " + e.getMessage());
        }
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"" + message + "\"}");
    }


    /**
     * ✅ Limpia el token JWT de espacios en blanco
     */
    private String cleanJwtToken(String rawToken) {
        if (rawToken == null) {
            throw new IllegalArgumentException("Token cannot be null");
        }

        String cleaned = rawToken.trim()
                .replaceAll("\\s+", "")
                .replaceAll("\\t", "")
                .replaceAll("\\n", "")
                .replaceAll("\\r", "");

        logger.debug("Token cleaned - Original: " + rawToken.length() + ", Cleaned: " + cleaned.length());

        return cleaned;
    }

    /**
     * ✅ Valida el formato básico del token JWT
     */
    private void validateJwtFormat(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token is empty or null");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException(
                    "Invalid JWT format. Expected 3 parts, found: " + parts.length
            );
        }

        try {
            // Verificar que sea Base64 URL-safe válido
            Base64.getUrlDecoder().decode(parts[0]);
            Base64.getUrlDecoder().decode(parts[1]);
            Base64.getUrlDecoder().decode(parts[2]);

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Base64 encoding in JWT");
        }
    }

    private void validateAlgorithm(String token) {
        try {
            String[] parts = token.split("\\.");
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));

            // Verificar que el algoritmo sea HS256
            if (!headerJson.contains("\"alg\":\"HS256\"")) {
                logger.warn("JWT algorithm mismatch. Expected HS256, but header is: " + headerJson);
                throw new IllegalArgumentException("JWT token must use HS256 algorithm");
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to validate JWT algorithm: " + e.getMessage());
        }
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/auth/") ||
                path.equals("/") ||
                path.equals("/health") ||
                path.equals("/actuator/health") ||
                // ✅ SOLO estos son públicos
                path.matches("/api/users/[^/]+$") ||
                path.matches("/api/users/[^/]+/statistics.*");
    }
}
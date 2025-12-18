package com.example.neighborhelp.security;

import com.example.neighborhelp.entity.User;
import com.example.neighborhelp.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    @Autowired
    private UserDetailsService userDetailsService;

    @Value("${app.oauth.frontend-url:https://neighbourlyunion.com}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        try {
            OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
            String email = oauthUser.getAttribute("email");

            if (email == null) {
                throw new RuntimeException("Email not found in OAuth2 user");
            }

            // Load user details with authorities
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // Generate tokens
            String token = jwtService.generateToken(userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found")));

            String refreshToken = jwtService.generateRefreshToken(email);

            // Set authentication in security context
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Create redirect URL with tokens
            String redirectUrl = String.format(
                    "%s/oauth/callback?token=%s&refreshToken=%s",
                    frontendUrl,
                    URLEncoder.encode(token, StandardCharsets.UTF_8),
                    URLEncoder.encode(refreshToken, StandardCharsets.UTF_8)
            );

            getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        } catch (Exception e) {
            logger.error("OAuth2 authentication error", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Authentication failed: " + e.getMessage());
        }
    }

    private User createNewOAuth2User(String email, String givenName, String familyName,
                                     String picture, String provider) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(email.split("@")[0] + System.currentTimeMillis()); // Make unique
        user.setFirstName(givenName);
        user.setLastName(familyName);
        user.setAuthProvider(provider);
        user.setVerified(true);
        user.setEnabled(true);
        user.setRole(User.Role.USER);
        user.setType(User.UserType.INDIVIDUAL);

        // Set profile if it exists
        if (picture != null) {
            if (user.getProfile() == null) {
                user.setProfile(new com.example.neighborhelp.entity.UserProfile());
            }
            user.getProfile().setAvatar(picture);
        }

        return userRepository.save(user);
    }
}
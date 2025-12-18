package com.example.neighborhelp.service;

import com.example.neighborhelp.dto.LoginRequest;
import com.example.neighborhelp.dto.RegisterRequest;
import com.example.neighborhelp.dto.UserResponseRequest;
import com.example.neighborhelp.entity.RefreshToken;
import com.example.neighborhelp.entity.User;
import com.example.neighborhelp.entity.UserProfile;
import com.example.neighborhelp.entity.VerificationCode;
import com.example.neighborhelp.exception.InvalidTokenException;
import com.example.neighborhelp.exception.ResourceNotFoundException;
import com.example.neighborhelp.repository.RefreshTokenRepository;
import com.example.neighborhelp.repository.UserRepository;
import com.example.neighborhelp.security.JwtService;
import com.example.neighborhelp.dto.TokenResponseRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Authentication Service handling user registration, login, token management,
 * and password reset operations.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationService verificationService;
    private final UserService userService;

    /**
     * Registers a new user and sends verification code via email.
     */
    @Transactional
    public UserResponseRequest register(RegisterRequest request) {
        // 1. VALIDATION CHECKS
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email already exists");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalStateException("Username already exists");
        }

        // Type-specific validations
        if (request.getType() == User.UserType.ORGANIZATION) {
            if (request.getOrganizationName() == null || request.getOrganizationName().trim().isEmpty()) {
                throw new IllegalStateException("Organizations must have an organization name");
            }
            if (userRepository.existsByOrganizationName(request.getOrganizationName())) {
                throw new IllegalStateException("Organization name already exists");
            }
        } else if (request.getType() == User.UserType.INDIVIDUAL) {
            if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
                throw new IllegalStateException("First name is required for individual users");
            }
            if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
                throw new IllegalStateException("Last name is required for individual users");
            }
        }

        // 2. USER CREATION
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setType(request.getType());
        user.setVerified(false);
        user.setEnabled(true);
        user.setAuthProvider("LOCAL");

        // Set type-specific fields
        if (request.getType() == User.UserType.ORGANIZATION) {
            user.setOrganizationName(request.getOrganizationName());
        } else {
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
        }

        // 3. BUILD AND SET PROFILE
        if (request.getProfile() != null) {
            UserProfile profile = buildUserProfile(request.getProfile());
            user.setProfile(profile);
        }

        User savedUser = userRepository.save(user);

        // 4. SEND VERIFICATION CODE
        verificationService.createAndSendVerificationCode(
                savedUser,
                VerificationCode.VerificationType.EMAIL_VERIFICATION
        );

        // 5. RETURN USER RESPONSE
        return userService.mapToUserResponse(savedUser);
    }

    /**
     * Helper method to build UserProfile from ProfileData
     */
    private UserProfile buildUserProfile(RegisterRequest.ProfileData profileData) {
        return UserProfile.builder()
                .phone(profileData.getPhone())
                .bio(profileData.getBio())
                .avatar(profileData.getAvatar())
                .description(profileData.getDescription())
                .website(profileData.getWebsite())
                .address(profileData.getAddress())
                .logo(profileData.getLogo())
                .socialMedia(profileData.getSocialMedia())
                .yearsExperience(profileData.getYearsExperience())
                .skills(profileData.getSkills())
                .languages(profileData.getLanguages())
                .availability(profileData.getAvailability())
                .build();
    }

    /**
     * Authenticates user and returns JWT tokens.
     */
    public TokenResponseRequest login(LoginRequest request) {
        // 1. FIND USER AND CHECK VERIFICATION STATUS
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid email or password"));

        // 2. VERIFY EMAIL IS CONFIRMED
        if (!user.getVerified()) {
            throw new DisabledException("Email not verified. Please check your email for the verification code.");
        }

        // 3. AUTHENTICATE WITH SPRING SECURITY
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // 4. GENERATE TOKENS
        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = createRefreshToken(user.getId());

        // 5. RETURN TOKEN RESPONSE
        return TokenResponseRequest.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .build();
    }

    /**
     * Creates a new refresh token for a user.
     */
    private RefreshToken createRefreshToken(Long userId) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(userRepository.findById(userId).orElseThrow());
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Refreshes an expired access token using a valid refresh token.
     */
    @Transactional
    public TokenResponseRequest refreshToken(String token) throws InvalidTokenException {
        // 1. FIND AND VALIDATE REFRESH TOKEN
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        // 2. CHECK EXPIRATION
        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidTokenException("Refresh token expired. Please log in again");
        }

        // 3. GENERATE NEW ACCESS TOKEN
        User user = refreshToken.getUser();
        String accessToken = jwtService.generateToken(user);

        // 4. RETURN NEW TOKENS
        return TokenResponseRequest.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .build();
    }

    /**
     * Resets user password after code verification.
     */
    @Transactional
    public void resetPasswordWithCode(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
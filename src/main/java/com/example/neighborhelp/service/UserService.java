package com.example.neighborhelp.service;

import com.example.neighborhelp.dto.AdminDto.AdminUserListResponse;
import com.example.neighborhelp.dto.AdminDto.CreateUserRequest;
import com.example.neighborhelp.dto.UpdateUserRequest;
import com.example.neighborhelp.dto.UserResponseRequest;
import com.example.neighborhelp.exception.ResourceNotFoundException;
import com.example.neighborhelp.repository.RefreshTokenRepository;
import com.example.neighborhelp.repository.UserRepository;
import com.example.neighborhelp.entity.User;
import com.example.neighborhelp.entity.UserProfile;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User Service for managing user-related operations.
 */
@Service
public class UserService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ========== METHODS BY EMAIL (for authenticated user) ==========

    public UserResponseRequest getUserProfileByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return mapToUserResponse(user);
    }

    public UserResponseRequest updateUserProfileByEmail(String email, UpdateUserRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return updateUserProfile(user.getId(), request);
    }

    public void softDeleteUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        softDeleteUser(user.getId());
    }

    // ========== METHODS BY IDENTIFIER (username OR organization name) ==========

    public UserResponseRequest getPublicUserProfileByIdentifier(String identifier) {
        User user = findUserByIdentifier(identifier);
        return mapToPublicUserResponse(user);
    }

    @Transactional
    public void verifyOrganizationByIdentifier(String identifier) {
        User user = findUserByIdentifier(identifier);

        if (user.getType() != User.UserType.ORGANIZATION) {
            throw new IllegalStateException("Only organizations can be verified");
        }

        if (user.getVerified()) {
            throw new IllegalStateException("Organization is already verified");
        }

        user.setVerified(true);
        userRepository.save(user);
    }

    @Transactional
    public void updateUserStatusByIdentifier(String identifier, Boolean enabled) {
        User user = findUserByIdentifier(identifier);

        if (user.getRole() == User.Role.ADMIN && !enabled) {
            throw new IllegalStateException("Cannot disable admin accounts");
        }

        if (user.getEnabled().equals(enabled)) {
            String status = enabled ? "enabled" : "disabled";
            throw new IllegalStateException("User is already " + status);
        }

        user.setEnabled(enabled);
        userRepository.save(user);
    }

    private User findUserByIdentifier(String identifier) {
        return userRepository.findByUsername(identifier)
                .orElseGet(() -> userRepository.findByOrganizationName(identifier)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "User not found with identifier: " + identifier))
                );
    }

    // ========== LEGACY METHODS ==========

    public UserResponseRequest getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return mapToUserResponse(user);
    }

    public UserResponseRequest getPublicUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return mapToPublicUserResponse(user);
    }

    public UserResponseRequest getPublicUserProfileByOrganizationName(String organizationName) {
        User user = userRepository.findByOrganizationName(organizationName)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with organization name: " + organizationName));
        return mapToPublicUserResponse(user);
    }

    @Transactional
    public void verifyOrganizationByOrganizationName(String organizationName) {
        User user = userRepository.findByOrganizationName(organizationName)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with organization name: " + organizationName));

        if (user.getType() != User.UserType.ORGANIZATION) {
            throw new IllegalStateException("Only organizations can be verified");
        }

        if (user.getVerified()) {
            throw new IllegalStateException("Organization is already verified");
        }

        user.setVerified(true);
        userRepository.save(user);
    }

    @Transactional
    public void updateUserStatusByOrganizationName(String organizationName, Boolean enabled) {
        User user = userRepository.findByOrganizationName(organizationName)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with organization name: " + organizationName));

        if (user.getRole() == User.Role.ADMIN && !enabled) {
            throw new IllegalStateException("Cannot disable admin accounts");
        }

        if (user.getEnabled().equals(enabled)) {
            String status = enabled ? "enabled" : "disabled";
            throw new IllegalStateException("User is already " + status);
        }

        user.setEnabled(enabled);
        userRepository.save(user);
    }

    // ========== UPDATE USER PROFILE ==========

    @Transactional
    public UserResponseRequest updateUserProfile(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new IllegalStateException("Username already exists");
            }
            user.setUsername(request.getUsername());
        }

        if (user.getType() == User.UserType.ORGANIZATION) {
            if (request.getOrganizationName() != null) {
                if (request.getOrganizationName().trim().isEmpty()) {
                    throw new IllegalStateException("Organization name cannot be empty");
                }
                user.setOrganizationName(request.getOrganizationName());
            }
        } else if (user.getType() == User.UserType.INDIVIDUAL) {
            if (request.getFirstName() != null) {
                user.setFirstName(request.getFirstName());
            }
            if (request.getLastName() != null) {
                user.setLastName(request.getLastName());
            }
        }

        if (request.getProfile() != null) {
            UserProfile profile = user.getProfile();
            if (profile == null) {
                profile = new UserProfile();
                user.setProfile(profile);
            }
            updateProfileFromRequest(profile, request.getProfile());
        }

        User updatedUser = userRepository.save(user);
        return mapToUserResponse(updatedUser);
    }

    // ========== ADMIN USER MANAGEMENT ==========

    @Transactional
    public UserResponseRequest createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email already exists: " + request.getEmail());
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalStateException("Username already exists: " + request.getUsername());
        }

        if (request.getType() == User.UserType.INDIVIDUAL) {
            if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
                throw new IllegalStateException("First name is required for individual users");
            }
            if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
                throw new IllegalStateException("Last name is required for individual users");
            }
        } else if (request.getType() == User.UserType.ORGANIZATION) {
            if (request.getOrganizationName() == null || request.getOrganizationName().trim().isEmpty()) {
                throw new IllegalStateException("Organization name is required for organization users");
            }
            if (userRepository.existsByOrganizationName(request.getOrganizationName())) {
                throw new IllegalStateException("Organization name already exists: " + request.getOrganizationName());
            }
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setType(request.getType());
        user.setVerified(true);
        user.setEnabled(true);
        user.setAuthProvider("LOCAL");

        if (request.getType() == User.UserType.INDIVIDUAL) {
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
        } else {
            user.setOrganizationName(request.getOrganizationName());
        }

        if (request.getProfile() != null) {
            UserProfile profile = new UserProfile();
            updateProfileFromCreateRequest(profile, request.getProfile());
            user.setProfile(profile);
        }

        User savedUser = userRepository.save(user);
        return mapToUserResponse(savedUser);
    }

    private void updateProfileFromCreateRequest(UserProfile profile, CreateUserRequest.ProfileData profileData) {
        if (profileData.getPhone() != null) profile.setPhone(profileData.getPhone());
        if (profileData.getBio() != null) profile.setBio(profileData.getBio());
        if (profileData.getAvatar() != null) profile.setAvatar(profileData.getAvatar());
        if (profileData.getDescription() != null) profile.setDescription(profileData.getDescription());
        if (profileData.getWebsite() != null) profile.setWebsite(profileData.getWebsite());
        if (profileData.getAddress() != null) profile.setAddress(profileData.getAddress());
        if (profileData.getLogo() != null) profile.setLogo(profileData.getLogo());
        if (profileData.getSocialMedia() != null) profile.setSocialMedia(profileData.getSocialMedia());
        if (profileData.getYearsExperience() != null) profile.setYearsExperience(profileData.getYearsExperience());
        if (profileData.getSkills() != null) profile.setSkills(profileData.getSkills());
        if (profileData.getLanguages() != null) profile.setLanguages(profileData.getLanguages());
        if (profileData.getAvailability() != null) profile.setAvailability(profileData.getAvailability());
    }

    @Transactional
    public UserResponseRequest updateUserRole(String identifier, User.Role newRole) {
        User user = findUserByIdentifier(identifier);

        if (user.getRole() == User.Role.ADMIN && newRole != User.Role.ADMIN) {
            long adminCount = userRepository.countByRole(User.Role.ADMIN);
            if (adminCount <= 1) {
                throw new IllegalStateException("Cannot change role of the last admin");
            }
        }

        user.setRole(newRole);
        User updatedUser = userRepository.save(user);
        return mapToUserResponse(updatedUser);
    }

    private void updateProfileFromRequest(UserProfile profile, UpdateUserRequest.ProfileData profileData) {
        if (profileData.getPhone() != null) profile.setPhone(profileData.getPhone());
        if (profileData.getBio() != null) profile.setBio(profileData.getBio());
        if (profileData.getAvatar() != null) profile.setAvatar(profileData.getAvatar());
        if (profileData.getDescription() != null) profile.setDescription(profileData.getDescription());
        if (profileData.getWebsite() != null) profile.setWebsite(profileData.getWebsite());
        if (profileData.getAddress() != null) profile.setAddress(profileData.getAddress());
        if (profileData.getLogo() != null) profile.setLogo(profileData.getLogo());
        if (profileData.getSocialMedia() != null) profile.setSocialMedia(profileData.getSocialMedia());
        if (profileData.getYearsExperience() != null) profile.setYearsExperience(profileData.getYearsExperience());

        // ✅ Convert skills from List to comma-separated String and save to profile
        if (profileData.getSkills() != null) {
            if (profileData.getSkills().isEmpty()) {
                profile.setSkills(null);  // or "" if you prefer empty string
            } else {
                String skillsStr = String.join(",", profileData.getSkills());
                profile.setSkills(skillsStr);
            }
        }

        // ✅ Convert languages from List to comma-separated String and save to profile
        if (profileData.getLanguages() != null) {
            if (profileData.getLanguages().isEmpty()) {
                profile.setLanguages(null);  // or "" if you prefer empty string
            } else {
                String languagesStr = String.join(",", profileData.getLanguages());
                profile.setLanguages(languagesStr);
            }
        }


        if (profileData.getAvailability() != null) profile.setAvailability(profileData.getAvailability());
    }

    // ========== USER STATUS METHODS ==========

    @Transactional
    public void updateUserStatus(Long userId, Boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (user.getRole() == User.Role.ADMIN && !enabled) {
            throw new IllegalStateException("Cannot disable admin accounts");
        }

        if (user.getEnabled().equals(enabled)) {
            String status = enabled ? "enabled" : "disabled";
            throw new IllegalStateException("User is already " + status);
        }

        user.setEnabled(enabled);
        userRepository.save(user);
    }

    // ========== EXISTENCE CHECKS ==========

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    // ========== DELETE METHODS ==========

    @Transactional
    public void deleteUser(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
        userRepository.deleteById(userId);
    }

    @Transactional
    public void softDeleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        user.setEnabled(false);

        userRepository.save(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void restoreUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setDeleted(false);
        user.setDeletedAt(null);
        user.setEnabled(true);

        userRepository.save(user);
    }

    // ========== ADMIN USER MANAGEMENT ==========

    @Transactional
    public Page<AdminUserListResponse> getAllUsers(
            String role,
            String type,
            Boolean enabled,
            Boolean verified,
            Boolean includeDeleted,
            String search,
            Pageable pageable) {

        Specification<User> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by role
            if (role != null && !role.trim().isEmpty()) {
                try {
                    User.Role userRole = User.Role.valueOf(role.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("role"), userRole));
                } catch (IllegalArgumentException e) {
                    // Invalid role, ignore filter
                }
            }

            // Filter by type
            if (type != null && !type.trim().isEmpty()) {
                try {
                    User.UserType userType = User.UserType.valueOf(type.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("type"), userType));
                } catch (IllegalArgumentException e) {
                    // Invalid type, ignore filter
                }
            }

            // Filter by enabled status
            if (enabled != null) {
                predicates.add(criteriaBuilder.equal(root.get("enabled"), enabled));
            }

            // Filter by verified status
            if (verified != null) {
                predicates.add(criteriaBuilder.equal(root.get("verified"), verified));
            }

            // Filter deleted users
            if (includeDeleted == null || !includeDeleted) {
                predicates.add(criteriaBuilder.equal(root.get("deleted"), false));
            }

            // Search in username, email, firstName, lastName, organizationName
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                Predicate usernameLike = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("username")), searchPattern);
                Predicate emailLike = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("email")), searchPattern);
                Predicate firstNameLike = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("firstName")), searchPattern);
                Predicate lastNameLike = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("lastName")), searchPattern);
                Predicate orgNameLike = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("organizationName")), searchPattern);

                predicates.add(criteriaBuilder.or(usernameLike, emailLike, firstNameLike, lastNameLike, orgNameLike));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> users = userRepository.findAll(spec, pageable);
        return users.map(this::mapToAdminUserListResponse);
    }

    @Transactional
    public void deleteUserByIdentifier(String identifier) {
        User user = findUserByIdentifier(identifier);

        if (user.getRole() == User.Role.ADMIN) {
            long adminCount = userRepository.countByRole(User.Role.ADMIN);
            if (adminCount <= 1) {
                throw new IllegalStateException("Cannot delete the last admin");
            }
        }

        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        user.setEnabled(false);
        userRepository.save(user);
    }

    // ========== MAPPING METHODS ==========

    public UserResponseRequest mapToUserResponse(User user) {
        if (user == null) return null;

        return new UserResponseRequest.Builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .type(user.getType())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .organizationName(user.getOrganizationName())
                .profile(mapProfileToResponse(user.getProfile()))
                .verified(user.getVerified())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public UserResponseRequest mapToPublicUserResponse(User user) {
        if (user == null) return null;

        return new UserResponseRequest.Builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .type(user.getType())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .organizationName(user.getOrganizationName())
                .profile(mapProfileToResponse(user.getProfile()))
                .verified(user.getVerified())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private UserResponseRequest.ProfileData mapProfileToResponse(UserProfile profile) {
        if (profile == null) return null;

        UserResponseRequest.ProfileData profileData = new UserResponseRequest.ProfileData();
        profileData.setPhone(profile.getPhone());
        profileData.setBio(profile.getBio());
        profileData.setAvatar(profile.getAvatar());
        profileData.setDescription(profile.getDescription());
        profileData.setWebsite(profile.getWebsite());
        profileData.setAddress(profile.getAddress());
        profileData.setLogo(profile.getLogo());
        profileData.setSocialMedia(profile.getSocialMedia());
        profileData.setYearsExperience(profile.getYearsExperience());

        // Convert skills from String to List
        if (profile.getSkills() != null && !profile.getSkills().isEmpty()) {
            List<String> skillsList = Arrays.asList(profile.getSkills().split(","));
            profileData.setSkills(skillsList);
        }

        if (profile.getLanguages() != null && !profile.getLanguages().isEmpty()) {
            List<String> languagesList = Arrays.asList(profile.getLanguages().split(","));
            profileData.setLanguages(languagesList);
        }


        profileData.setAvailability(profile.getAvailability());

        return profileData;
    }

    private AdminUserListResponse mapToAdminUserListResponse(User user) {
        // Create AdminUserListResponse using your preferred method
        // This depends on how your AdminUserListResponse class is structured
        return AdminUserListResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .type(user.getType())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .organizationName(user.getOrganizationName())
                .verified(user.getVerified())
                .enabled(user.getEnabled())
                .deleted(user.getDeleted())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .deletedAt(user.getDeletedAt())
                .authProvider(user.getAuthProvider())
                .build();
    }
}
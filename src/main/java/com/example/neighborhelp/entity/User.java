package com.example.neighborhelp.entity;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.*;
import org.hibernate.annotations.Where;
import org.signal.libsignal.protocol.IdentityKey;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity class representing a user in the NeighborHelp system.
 *
 * This is the main user entity that stores all user information including
 * authentication details, profile information, and account status.
 * It supports both local authentication and Firebase authentication
 * through a unified user model.
 *
 * Maps to the 'users' table in the database.
 *
 * @author NeighborHelp Team
 */
@Entity
@SQLDelete(sql = "UPDATE users SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
@Table(name = "users")
public class User {

    // ========== PRIMARY KEY & IDENTIFICATION ==========

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    // ========== AUTHENTICATION & SECURITY ==========

    @Column(nullable = true)
    private String password;

    @Column(unique = true, name = "firebase_uid")
    private String firebaseUid;

    @Column(name = "auth_provider")
    private String authProvider = "LOCAL";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(nullable = false)
    private Boolean verified = false;

    @Column(nullable = false)
    private Boolean enabled = false;

    @Column(name = "verification_token")
    private String verificationToken;

    // ========== USER TYPE ==========

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private UserType type;

    // ========== BASIC PROFILE INFORMATION ==========

    /**
     * First name (for individual users)
     */
    @Column(name = "first_name")
    private String firstName;

    /**
     * Last name (for individual users)
     */
    @Column(name = "last_name")
    private String lastName;

    /**
     * Organization name (for organization users)
     */
    @Column(name = "organization_name", unique = true)
    private String organizationName;

    // ========== EMBEDDED PROFILE ==========

    /**
     * Profile information - different fields used based on user type
     */
    @Embedded
    private UserProfile profile;

    // ========== TIMESTAMPS & AUDITING ==========

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updatedAt;

    // ========== RELATIONSHIPS ==========

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<VerificationCode> verificationCodes = new ArrayList<>();

    // ========== ENUM DEFINITIONS ==========

    public enum Role {
        USER,
        ADMIN,
        MODERATOR
    }

    public enum UserType {
        INDIVIDUAL,
        ORGANIZATION
    }

    // ========== CUSTOM METHODS ==========

    public void addVerificationCode(VerificationCode verificationCode) {
        verificationCodes.add(verificationCode);
        verificationCode.setUser(this);
    }

    public boolean isFirebaseUser() {
        return "FIREBASE".equals(authProvider) && firebaseUid != null;
    }

    public boolean isLocalUser() {
        return "LOCAL".equals(authProvider) && password != null;
    }

    /**
     * Get display name based on user type
     */
    public String getDisplayName() {
        if (type == UserType.ORGANIZATION) {
            return organizationName;
        } else {
            if (firstName != null && lastName != null) {
                return firstName + " " + lastName;
            }
            return username;
        }
    }

    /**
     * Get unique identifier for URL (organization name or username)
     */
    public String getUrlIdentifier() {
        return (type == UserType.ORGANIZATION && organizationName != null)
                ? organizationName
                : username;
    }

    // ========== GETTER AND SETTER METHODS ==========

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public Role getRole() { return role; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public Boolean getEnabled() { return enabled; }
    public UserType getType() { return type; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getOrganizationName() { return organizationName; }
    public UserProfile getProfile() { return profile; }
    public Boolean getVerified() { return verified; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getVerificationToken() { return verificationToken; }
    public List<VerificationCode> getVerificationCodes() { return verificationCodes; }
    public String getFirebaseUid() { return firebaseUid; }
    public String getAuthProvider() { return authProvider; }
    public List<RefreshToken> getRefreshTokens() { return refreshTokens; }
    public Boolean getDeleted() { return deleted; }
    public LocalDateTime getDeletedAt() { return deletedAt; }

    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setRole(Role role) { this.role = role; }
//    public void setEmail(String email) { this.email = email; }
public void setEmail(String email) {
    if (email == null || email.trim().isEmpty()) {
        throw new IllegalArgumentException("Email cannot be null or empty");
    }
    this.email = email.trim().toLowerCase();
}

//    public void setPassword(String password) { this.password = password; }
public void setPassword(String password) {
    // Only set password if not null and not empty
    if (password != null && !password.trim().isEmpty()) {
        this.password = password.trim();
    }
}
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public void setType(UserType type) { this.type = type; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
    public void setProfile(UserProfile profile) { this.profile = profile; }
    public void setVerified(Boolean verified) { this.verified = verified; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }
    public void setVerificationCodes(List<VerificationCode> verificationCodes) { this.verificationCodes = verificationCodes; }
    public void setFirebaseUid(String firebaseUid) { this.firebaseUid = firebaseUid; }
    public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }
    public void setRefreshTokens(List<RefreshToken> refreshTokens) { this.refreshTokens = refreshTokens; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }



}
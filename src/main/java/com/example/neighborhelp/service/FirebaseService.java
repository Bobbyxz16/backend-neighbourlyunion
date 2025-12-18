package com.example.neighborhelp.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.UserRecord.CreateRequest;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

/**
 * Firebase Service for integrating with Firebase Authentication.
 *
 * This service provides functionality to manage user accounts in Firebase
 * Authentication system, including user creation, email verification status
 * management, and custom token generation for authentication.
 */
@Service  // Marks this class as a Spring Service component
public class FirebaseService {

    /**
     * Path to Firebase Admin SDK configuration file.
     */
    @Value("${firebase.config.path:C:\\Users\\bobby\\IdeaProjects\\NeighborHelp\\src\\main\\resources\\neighborhelp-e7f2b-firebase-adminsdk-fbsvc-a91ee43094.json}")
    private String firebaseConfigPath;

    /**
     * Initializes Firebase Admin SDK during application startup.
     *
     * This method is called automatically after dependency injection is complete.
     * It loads the Firebase service account credentials and initializes the
     * Firebase application instance if it hasn't been initialized already.
     *
     * @throws RuntimeException if Firebase initialization fails
     */
    @PostConstruct  // Executed after dependency injection is complete
    public void initialize() {
        try {
            // Check if Firebase is already initialized to avoid duplicate initialization
            if (FirebaseApp.getApps().isEmpty()) {
                // Load Firebase service account credentials from classpath
                InputStream serviceAccount = new ClassPathResource(firebaseConfigPath).getInputStream();

                // Configure Firebase options with credentials
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                // Initialize the Firebase application
                FirebaseApp.initializeApp(options);

                System.out.println("✅ Firebase Admin SDK initialized successfully");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Firebase", e);
        }
    }


    /**
     * Generates a custom token for Firebase authentication.
     *
     * Custom tokens allow you to authenticate users with your own authentication
     * system while still using Firebase for the actual authentication process.
     *
     * @param firebaseUid the Firebase user ID
     * @return a signed custom token that can be used for Firebase authentication
     * @throws FirebaseAuthException if token generation fails
     *
     * Usage:
     * - Frontend receives this token and signs in with signInWithCustomToken()
     * - Useful for testing or integrating with existing auth systems
     */
    public String createCustomToken(String firebaseUid) throws FirebaseAuthException {
        return FirebaseAuth.getInstance().createCustomToken(firebaseUid);
    }


}
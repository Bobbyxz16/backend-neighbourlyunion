package com.example.neighborhelp.security;

import com.example.neighborhelp.entity.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

/**
 * Email service for sending transactional emails using Resend.com API.
 *
 * This service handles sending verification codes and password reset emails
 * to users during registration and account recovery processes. It uses
 * Resend.com as the email delivery platform with HTML templates for
 * professional-looking emails.
 */
@Service  // Marks this class as a Spring Service component
public class EmailService {

    /**
     * Resend.com API key injected from application properties
     */
    @Value("${resend.api.key}")  // Injects API key from application.properties/yml
    private String apiKey;

    /**
     * RestTemplate for making HTTP requests to Resend.com API
     */
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Resend.com API endpoint for sending emails
     */
    private final String RESEND_URL = "https://api.resend.com/emails";

    /**
     * Sends email verification code to new users during registration.
     * This method is called when a user registers and needs to verify
     * their email address with a 6-digit code.
     */
    public void sendVerificationCodeEmail(String toEmail, String code, String username) {
        String subject = "Verify your NeighborHelp account";
        String html = buildVerificationCodeEmailHtml(code, username);

        sendEmail(toEmail, subject, html);
    }

    /**
     * Sends password reset code to users who requested password recovery.
     * This method is called when a user initiates the password reset process
     * and needs to verify their identity with a 6-digit code.
     */
    public void sendPasswordResetCodeEmail(String toEmail, String code, String username) {
        String subject = "Reset your NeighborHelp password";
        String html = buildPasswordResetCodeEmailHtml(code, username);

        sendEmail(toEmail, subject, html);
    }

    /**
     * HTML template for email verification codes.
     */
    private String buildVerificationCodeEmailHtml(String code, String username) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background-color: #2E86C1; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;">
                    <h1 style="color: white; margin: 0;">NeighborHelp</h1>
                </div>
                
                <div style="background-color: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px;">
                    <h2 style="color: #333;">Hello %s!</h2>
                    <p style="color: #666; font-size: 16px;">
                        Thank you for registering with NeighborHelp. To complete your registration, 
                        please use the verification code below:
                    </p>
                    
                    <div style="background-color: white; border: 2px solid #2E86C1; 
                                border-radius: 8px; padding: 20px; margin: 30px 0; text-align: center;">
                        <p style="color: #666; margin: 0 0 10px 0; font-size: 14px;">Your verification code:</p>
                        <h1 style="color: #2E86C1; font-size: 36px; letter-spacing: 8px; margin: 0;">
                            %s
                        </h1>
                    </div>
                    
                    <p style="color: #666; font-size: 14px;">
                        This code will expire in <strong>1 hour</strong>.
                    </p>
                </div>
            </div>
            """.formatted(username, code);
    }

    /**
     * HTML template for password reset codes.
     */
    private String buildPasswordResetCodeEmailHtml(String code, String username) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background-color: #E74C3C; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;">
                    <h1 style="color: white; margin: 0;">NeighborHelp</h1>
                </div>
                
                <div style="background-color: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px;">
                    <h2 style="color: #333;">Hello %s!</h2>
                    <p style="color: #666; font-size: 16px;">
                        We received a request to reset your password. Use the code below to proceed:
                    </p>
                    
                    <div style="background-color: white; border: 2px solid #E74C3C; 
                                border-radius: 8px; padding: 20px; margin: 30px 0; text-align: center;">
                        <p style="color: #666; margin: 0 0 10px 0; font-size: 14px;">Your password reset code:</p>
                        <h1 style="color: #E74C3C; font-size: 36px; letter-spacing: 8px; margin: 0;">
                            %s
                        </h1>
                    </div>
                    
                    <p style="color: #666; font-size: 14px;">
                        This code will expire in <strong>1 hour</strong>.
                    </p>
                </div>
            </div>
            """.formatted(username, code);
    }

    /**
     * Core method for sending emails via Resend.com API.
     *
     * This method handles the actual HTTP communication with Resend.com
     * and includes proper error handling to prevent application crashes
     * if email delivery fails.
     */
    public void sendEmail(String toEmail, String subject, String htmlContent) {
        // Set up HTTP headers with authentication
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);  // Uses Bearer token authentication

        // Build the request payload for Resend.com API
        Map<String, Object> request = new HashMap<>();
        request.put("from", "NeighborHelp <hello@neighborlyunion.com>"); // Verified sender domain
        request.put("to", toEmail);
        request.put("subject", subject);
        request.put("html", htmlContent);

        // Create HTTP entity with headers and payload
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            // Send POST request to Resend.com API
            ResponseEntity<String> response = restTemplate.exchange(RESEND_URL, HttpMethod.POST, entity, String.class);
            System.out.println("✅ Email sent successfully: " + response.getStatusCode());

            // Log response body for debugging
            if (response.getBody() != null) {
                System.out.println("Response: " + response.getBody());
            }
        } catch (Exception e) {
            // Graceful error handling - log but don't crash the application
            System.err.println("❌ Email sending failed: " + e.getMessage());
            // Note: Not throwing exception allows user registration to succeed
            // even if email delivery fails (user can request resend later)
        }
    }

    // ========== LEGACY METHODS (MAINTAINED FOR BACKWARD COMPATIBILITY) ==========

    /**
     * Legacy method for sending verification emails (maintained for compatibility).
     *
     */
    public void sendVerificationEmail(String toEmail, String verificationToken) {
        // Implementation can be added if needed
        // Currently maintained for backward compatibility
    }

    /**
     * Legacy method for sending password reset emails (maintained for compatibility).
     *
     * @param toEmail the recipient's email address
     * @param resetUrl the password reset URL (not currently used)
     */
    public void sendPasswordResetEmail(String toEmail, String resetUrl) {
        // Implementation can be added if needed
        // Currently maintained for backward compatibility
    }

    /**
     * Sends message notification email to resource owner
     */
    public void sendMessageNotificationEmail(String toEmail, String recipientName, String senderName,
                                             String messageSubject, String messageContent, String priority,
                                             String contactMethod, String senderPhone, Resource resource) {
        String subject = "New message about your resource: " + resource.getTitle();
        String htmlContent = buildResourceMessageEmailHtml(
                recipientName, senderName, messageSubject, messageContent,
                priority, contactMethod, senderPhone, resource
        );

        sendEmail(toEmail, subject, htmlContent);
    }

    /**
     * HTML template for resource message notification emails
     */
    private String buildResourceMessageEmailHtml(
            String recipientName,
            String senderName,
            String messageSubject,
            String messageContent,
            String priority,
            String contactMethod,
            String senderPhone,
            Resource resource) {

        String priorityColor = getPriorityColor(priority);
        String priorityText = getPriorityText(priority);

        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background-color: #27AE60; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;">
                    <h1 style="color: white; margin: 0;">NeighborHelp</h1>
                    <p style="color: white; margin: 5px 0 0 0;">Community Connection Platform</p>
                </div>
                
                <div style="background-color: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px;">
                    <h2 style="color: #333;">Hello %s!</h2>
                    <p style="color: #666; font-size: 16px;">
                        You have received a new message about your resource from <strong>%s</strong>.
                    </p>
                    
                    <!-- Resource Information -->
                    <div style="background-color: #E8F5E8; border: 1px solid #27AE60; 
                                border-radius: 8px; padding: 15px; margin: 20px 0;">
                        <h3 style="color: #333; margin-top: 0;">📋 About Your Resource</h3>
                        <p style="color: #666; margin: 5px 0;"><strong>Title:</strong> %s</p>
                        <p style="color: #666; margin: 5px 0;"><strong>Category:</strong> %s</p>
                        <p style="color: #666; margin: 5px 0;"><strong>Location:</strong> %s</p>
                        <p style="color: #666; margin: 5px 0;"><strong>Cost:</strong> %s</p>
                    </div>
                    
                    <!-- Message Content -->
                    <div style="background-color: white; border-left: 4px solid %s; 
                                border-radius: 4px; padding: 20px; margin: 20px 0;">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px;">
                            <h3 style="color: #333; margin: 0;">%s</h3>
                            <span style="background-color: %s; color: white; padding: 4px 8px; 
                                        border-radius: 4px; font-size: 12px; font-weight: bold;">
                                %s
                            </span>
                        </div>
                        <p style="color: #666; line-height: 1.6; margin: 0;">%s</p>
                    </div>
                    
                    <!-- Contact Information -->
                    <div style="background-color: #F0F8FF; border: 1px solid #3498DB; 
                                border-radius: 8px; padding: 15px; margin: 20px 0;">
                        <h4 style="color: #333; margin-top: 0;">📞 Contact Information:</h4>
                        <p style="color: #666; margin: 5px 0;">
                            <strong>From:</strong> %s
                        </p>
                        <p style="color: #666; margin: 5px 0;">
                            <strong>Preferred Contact Method:</strong> %s
                        </p>
                        %s
                    </div>
                    
                    <!-- Action Buttons -->
                    <div style="text-align: center; margin-top: 30px;">
                        <a href="https://yourapp.com/messages" 
                           style="background-color: #27AE60; color: white; padding: 12px 24px; 
                                  text-decoration: none; border-radius: 4px; font-weight: bold;
                                  display: inline-block; margin: 0 10px;">
                            💬 Reply in NeighborHelp
                        </a>
                        <a href="https://yourapp.com/resources/%d" 
                           style="background-color: #3498DB; color: white; padding: 12px 24px; 
                                  text-decoration: none; border-radius: 4px; font-weight: bold;
                                  display: inline-block; margin: 0 10px;">
                            📋 View Your Resource
                        </a>
                    </div>
                    
                    <p style="color: #999; font-size: 12px; text-align: center; margin-top: 30px;">
                        This is an automated notification. Please do not reply to this email.
                    </p>
                </div>
            </div>
            """.formatted(
                recipientName,
                senderName,
                resource.getTitle(),
                resource.getCategory() != null ? resource.getCategory().getName() : "Uncategorized",
                resource.getFullAddress() != null ? resource.getFullAddress() : "Location not specified",
                resource.getCost() != null ? resource.getCost().name() : "Not specified",
                priorityColor,
                messageSubject,
                priorityColor,
                priorityText,
                messageContent.replace("\n", "<br>"),
                senderName,
                contactMethod,
                senderPhone != null ?
                        "<p style=\"color: #666; margin: 5px 0;\"><strong>Phone:</strong> " + senderPhone + "</p>" : "",
                resource.getId()
        );
    }

    /**
     * Get color based on message priority
     */


    /**
     * Get priority text for display
     */
    private String getPriorityText(String priority) {
        return switch (priority) {
            case "HIGH" -> "High Priority";
            case "URGENT" -> "Urgent Priority";
            default -> "Normal Priority";
        };
    }
    /**
     * Get color based on message priority
     */
    private String getPriorityColor(String priority) {
        return switch (priority) {
            case "HIGH" -> "#F39C12";    // Orange
            case "URGENT" -> "#E74C3C";  // Red
            default -> "#3498DB";        // Blue (Normal)
        };
    }
}
// src/main/java/com/example/neighborhelp/service/MessageService.java
package com.example.neighborhelp.service;

import com.example.neighborhelp.dto.MessageDto.*;
import com.example.neighborhelp.entity.Message;
import com.example.neighborhelp.entity.Resource;
import com.example.neighborhelp.entity.User;
import com.example.neighborhelp.repository.MessageRepository;
import com.example.neighborhelp.repository.ResourceRepository;
import com.example.neighborhelp.repository.UserRepository;
import com.example.neighborhelp.security.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final EmailService emailService;
    private final EncryptionService encryptionService;

    @Transactional
    public MessageResponse sendMessage(SendMessageRequest request, User sender) {
        User recipient = userRepository.findById(request.getRecipientId())
                .orElseThrow(() -> new RuntimeException("Recipient not found with ID: " + request.getRecipientId()));

        if (recipient.getId().equals(sender.getId())) {
            throw new RuntimeException("You cannot send a message to yourself");
        }

        Resource resource = null;
        if (request.getResourceId() != null) {
            resource = resourceRepository.findById(request.getResourceId())
                    .orElseThrow(() -> new RuntimeException("Resource not found with ID: " + request.getResourceId()));

            if (!resource.isPubliclyVisible()) {
                throw new RuntimeException("This resource is not available for messaging");
            }

            if (resource.getUser().getId().equals(sender.getId()) && recipient.getId().equals(sender.getId())) {
                throw new RuntimeException("You cannot message yourself about your own resource");
            }
        }

        // Encrypt content BEFORE building message
        String encryptedContent = encryptionService.encrypt(request.getContent());

        Message message = Message.builder()
                .sender(sender)
                .recipient(recipient)
                .resource(resource)
                .subject(request.getSubject())
                .encryptedContent(encryptedContent) // ← Store encrypted content
                .priority(Message.MessagePriority.valueOf(request.getPriority()))
                .senderPhone(request.getSenderPhone())
                .isRead(false)
                .deletedBySender(false)
                .deletedByRecipient(false)
                .build();

        message = messageRepository.save(message);

        // Send email with decrypted content
        sendMessageNotification(message, resource, request.getContent());

        return mapToResponse(message);
    }

    private void sendMessageNotification(Message message, Resource resource, String plainContent) {
        try {
            String recipientEmail = message.getRecipient().getEmail();
            String recipientName = message.getRecipient().getOrganizationName() != null ?
                    message.getRecipient().getOrganizationName() :
                    message.getRecipient().getUsername();

            String senderName = message.getSender().getOrganizationName() != null ?
                    message.getSender().getOrganizationName() :
                    message.getSender().getUsername();

            emailService.sendMessageNotificationEmail(
                    recipientEmail,
                    recipientName,
                    senderName,
                    message.getSubject(),
                    plainContent, // ← Use plain content for email
                    message.getPriority().name(),
                    message.getContactMethod(),
                    message.getSenderPhone(),
                    resource
            );

        } catch (Exception e) {
            System.err.println("Failed to send message notification email: " + e.getMessage());
        }
    }


    /**
     * Get inbox messages (received) - using optimized JOIN FETCH
     */
    @Transactional(readOnly = true)
    public Page<MessageResponse> getInboxMessages(User user, Pageable pageable) {
        Page<Message> messages = messageRepository.findByRecipientAndDeletedByRecipientFalseOrderByCreatedAtDesc(user, pageable);
        return messages.map(this::mapToResponse);
    }

    /**
     * Get sent messages - using optimized JOIN FETCH
     */
    @Transactional(readOnly = true)
    public Page<MessageResponse> getSentMessages(User user, Pageable pageable) {
        Page<Message> messages = messageRepository.findBySenderAndDeletedBySenderFalseOrderByCreatedAtDesc(user, pageable);
        return messages.map(this::mapToResponse);
    }

    /**
     * Get unread messages - using optimized JOIN FETCH
     */
    @Transactional(readOnly = true)
    public Page<MessageResponse> getUnreadMessages(User user, Pageable pageable) {
        Page<Message> messages = messageRepository.findByRecipientAndIsReadFalseAndDeletedByRecipientFalseOrderByCreatedAtDesc(user, pageable);
        return messages.map(this::mapToResponse);
    }

    /**
     * Mark message as read - use the JOIN FETCH version to avoid lazy loading issues
     */
    @Transactional
    public MessageResponse markAsRead(Long messageId, User user) {
        Message message = messageRepository.findByIdWithAssociations(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // Only recipient can mark as read
        if (!message.getRecipient().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        message.setIsRead(true);
        message.setReadAt(LocalDateTime.now());
        message = messageRepository.save(message);

        return mapToResponse(message);
    }

    /**
     * Get unread count
     */
    @Transactional(readOnly = true)
    public Long getUnreadCount(User user) {
        return messageRepository.countByRecipientAndIsReadFalseAndDeletedByRecipientFalse(user);
    }

    /**
     * Get all active regular users (excluding ADMIN and MODERATOR) for messaging
     * Returns only basic, non-sensitive information
     * Excludes the current user from the list
     */
    @Transactional(readOnly = true)
    public java.util.List<MessageUserResponse> getAvailableUsersForMessaging(User currentUser) {
        // Get all active users with role USER (not ADMIN or MODERATOR)
        java.util.List<User> allUsers = userRepository.findAllActiveRegularUsers();

        // Map to response DTOs with only essential information
        // Exclude current user from the list
        return allUsers.stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .map(this::mapToMessageUserResponse)
                .collect(java.util.stream.Collectors.toList());
    }



    /**
     * Delete message
     * - If the sender deletes: delete for everyone (both sender and recipient)
     * - If the recipient deletes: delete only for the recipient
     */
    @Transactional
    public void deleteMessage(Long messageId, User user) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        boolean isSender = message.getSender().getId().equals(user.getId());
        boolean isRecipient = message.getRecipient().getId().equals(user.getId());

        if (!isSender && !isRecipient) {
            throw new RuntimeException("Unauthorized - you are not the sender or recipient of this message");
        }

        if (isSender) {
            // Sender deletes -> delete for everyone
            message.setDeletedBySender(true);
            message.setDeletedByRecipient(true); // Also mark for recipient
        } else if (isRecipient) {
            // Recipient deletes -> delete only for recipient
            message.setDeletedByRecipient(true);
        }

        // Now, if both flags are true, we can physically delete the message
        if (message.getDeletedBySender() && message.getDeletedByRecipient()) {
            messageRepository.delete(message);
            System.out.println("Message permanently deleted");
        } else {
            messageRepository.save(message);
            System.out.println("Message soft deleted for " + (isSender ? "sender (and everyone)" : "recipient"));
        }
    }

    // ... existing code ...

    // ... existing code ...

    /**
     * Map entity to response
     */
    /**
     * Map entity to response with error handling for old encrypted data
     */
    private MessageResponse mapToResponse(Message message) {
        // Try to decrypt content, fallback to placeholder if it fails
        String decryptedContent;
        try {
            decryptedContent = encryptionService.decrypt(message.getEncryptedContent());
        } catch (IllegalArgumentException e) {
            // Content encrypted with old key - cannot decrypt
            System.err.println("Cannot decrypt message ID " + message.getId() + ": " + e.getMessage());
            decryptedContent = "[Message content unavailable - encrypted with previous key]";
        }

        return MessageResponse.builder()
                .id(message.getId())
                .sender(SenderInfo.builder()
                        .id(message.getSender().getId())
                        .name(message.getSender().getOrganizationName() != null ?
                                message.getSender().getOrganizationName() :
                                message.getSender().getUsername())
                        .email(message.getSender().getEmail())
                        .avatar(getUserAvatar(message.getSender()))
                        .build())
                .recipient(RecipientInfo.builder()
                        .id(message.getRecipient().getId())
                        .name(message.getRecipient().getOrganizationName() != null ?
                                message.getRecipient().getOrganizationName() :
                                message.getRecipient().getUsername())
                        .email(message.getRecipient().getEmail())
                        .avatar(getUserAvatar(message.getRecipient()))
                        .build())
                .resource(message.getResource() != null ? ResourceInfo.builder()
                        .id(message.getResource().getId())
                        .title(message.getResource().getTitle())
                        .resourceName(message.getResource().getTitle())
                        .city(message.getResource().getLocation().getCity())
                        .build() : null)
                .subject(message.getSubject())
                .content(decryptedContent) // Use the decrypted or fallback content
                .isRead(message.getIsRead())
                .priority(message.getPriority().name())
                .contactMethod(message.getContactMethod())
                .senderPhone(message.getSenderPhone())
                .createdAt(message.getCreatedAt())
                .readAt(message.getReadAt())
                .build();
    }

    /**
     * Helper method to get user avatar/logo
     */
    private String getUserAvatar(User user) {
        if (user == null) return null;

        // If organization, return logo
        if (user.getOrganizationName() != null && user.getOrganizationName().length() > 0) {
            return user.getProfile() != null ? user.getProfile().getLogo() : null;
        }

        // If individual user, return avatar
        return user.getProfile() != null ? user.getProfile().getAvatar() : null;
    }


    /**
     * Map user entity to MessageUserResponse with only essential, non-sensitive information
     */
    private MessageUserResponse mapToMessageUserResponse(User user) {
        String displayName = user.getOrganizationName() != null ?
                user.getOrganizationName() :
                user.getUsername();

        return MessageUserResponse.builder()
                .id(user.getId())
                .displayName(displayName)
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }


}

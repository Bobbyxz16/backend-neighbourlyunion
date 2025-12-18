// src/main/java/com/example/neighborhelp/entity/Message.java
package com.example.neighborhelp.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @Column(nullable = false)
    private String subject;

    // Store encrypted content in database
    @Column(name = "encrypted_content", columnDefinition = "TEXT", nullable = false)
    private String encryptedContent;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessagePriority priority = MessagePriority.NORMAL;

    @Column(name = "contact_method")
    private String contactMethod;

    @Column(name = "sender_phone")
    private String senderPhone;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "deleted_by_sender", nullable = false)
    private Boolean deletedBySender = false;

    @Column(name = "deleted_by_recipient", nullable = false)
    private Boolean deletedByRecipient = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum MessagePriority {
        NORMAL,
        HIGH,
        URGENT
    }
}
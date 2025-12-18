// src/main/java/com/example/neighborhelp/dto/MessageDto.java
package com.example.neighborhelp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

public class MessageDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendMessageRequest {
        private Long recipientId;
        private Long resourceId;
        private String subject;
        private String content;
        private String priority;
        private String senderPhone;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageResponse {
        private Long id;
        private SenderInfo sender;
        private RecipientInfo recipient;
        private ResourceInfo resource;
        private String subject;
        private String content;
        private Boolean isRead;
        private String priority;
        private String contactMethod;
        private String senderPhone;
        private LocalDateTime createdAt;
        private LocalDateTime readAt;
    }

    @Data
    @Builder
    public static class SenderInfo {
        private Long id;
        private String name;
        private String email;
        private String avatar;
    }

    @Data
    @Builder
    public static class RecipientInfo {
        private Long id;
        private String name;
        private String email;
        private String avatar;
    }

    @Data
    @Builder
    public static class ResourceInfo {
        private Long id;
        private String title;
        private String resourceName;
        private String city;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageUserResponse {
        private Long id;
        private String displayName; // username or organizationName
        private String username;
        private String email;
    }


}
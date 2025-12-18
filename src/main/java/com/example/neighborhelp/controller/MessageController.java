// src/main/java/com/example/neighborhelp/controller/MessageController.java
package com.example.neighborhelp.controller;

import com.example.neighborhelp.dto.MessageDto;
import com.example.neighborhelp.dto.MessageDto.*;
import com.example.neighborhelp.entity.User;
import com.example.neighborhelp.repository.UserRepository;
import com.example.neighborhelp.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UserRepository userRepository;

    /**
     * Send a message
     * POST /api/messages
     */
    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @Valid @RequestBody SendMessageRequest request
    ) {
        User sender = getCurrentUser();
        MessageResponse response = messageService.sendMessage(request, sender);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get inbox messages (received)
     * GET /api/messages/inbox
     */
    @GetMapping("/inbox")
    public ResponseEntity<Page<MessageResponse>> getInbox(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User user = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        Page<MessageResponse> messages = messageService.getInboxMessages(user, pageable);
        return ResponseEntity.ok(messages);
    }

    /**
     * Get sent messages
     * GET /api/messages/sent
     */
    @GetMapping("/sent")
    public ResponseEntity<Page<MessageResponse>> getSentMessages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User user = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        Page<MessageResponse> messages = messageService.getSentMessages(user, pageable);
        return ResponseEntity.ok(messages);
    }

    /**
     * Get unread messages
     * GET /api/messages/unread
     */
    @GetMapping("/unread")
    public ResponseEntity<Page<MessageResponse>> getUnreadMessages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User user = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        Page<MessageResponse> messages = messageService.getUnreadMessages(user, pageable);
        return ResponseEntity.ok(messages);
    }

    /**
     * Mark message as read
     * PATCH /api/messages/{id}/read
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<MessageResponse> markAsRead(@PathVariable Long id) {
        User user = getCurrentUser();
        MessageResponse response = messageService.markAsRead(id, user);
        return ResponseEntity.ok(response);
    }

    /**
     * Get unread count
     * GET /api/messages/unread/count
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        User user = getCurrentUser();
        Long count = messageService.getUnreadCount(user);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all active regular users available for messaging
     * GET /api/messages/users
     * Returns only users with role USER (excludes ADMIN and MODERATOR)
     * Returns only essential information: id, displayName, username, email
     */
    @GetMapping("/users")
    public ResponseEntity<java.util.List<MessageDto.MessageUserResponse>> getAvailableUsers() {
        User currentUser = getCurrentUser();
        java.util.List<MessageDto.MessageUserResponse> users = messageService.getAvailableUsersForMessaging(currentUser);
        return ResponseEntity.ok(users);
    }



    /**
     * Delete message
     * DELETE /api/messages/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        User user = getCurrentUser();
        messageService.deleteMessage(id, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
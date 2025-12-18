// src/main/java/com/example/neighborhelp/repository/MessageRepository.java
package com.example.neighborhelp.repository;

import com.example.neighborhelp.entity.Message;
import com.example.neighborhelp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // ========== EXISTING BASIC QUERIES ==========

    // Get all messages received by a user
    Page<Message> findByRecipientOrderByCreatedAtDesc(User recipient, Pageable pageable);

    // Get all messages sent by a user
    Page<Message> findBySenderOrderByCreatedAtDesc(User sender, Pageable pageable);

    // Get unread messages count
    Long countByRecipientAndIsReadFalse(User recipient);

    // Get messages by resource
    Page<Message> findByResourceIdOrderByCreatedAtDesc(Long resourceId, Pageable pageable);

    // Get unread messages
    Page<Message> findByRecipientAndIsReadFalseOrderByCreatedAtDesc(User recipient, Pageable pageable);

    // Para la bandeja de entrada: mensajes donde el usuario es el destinatario y no los ha eliminado.
    Page<Message> findByRecipientAndDeletedByRecipientFalseOrderByCreatedAtDesc(User recipient, Pageable pageable);

    // Para los mensajes enviados: mensajes donde el usuario es el remitente y no los ha eliminado.
    Page<Message> findBySenderAndDeletedBySenderFalseOrderByCreatedAtDesc(User sender, Pageable pageable);

    // Para mensajes no leídos: mensajes donde el usuario es el destinatario, no leídos y no eliminados por el destinatario.
    Page<Message> findByRecipientAndIsReadFalseAndDeletedByRecipientFalseOrderByCreatedAtDesc(User recipient, Pageable pageable);

    // Para contar mensajes no leídos: mismo filtro que arriba.
    Long countByRecipientAndIsReadFalseAndDeletedByRecipientFalse(User recipient);


    // ========== OPTIMIZED JOIN FETCH QUERIES ==========

    /**
     * Get inbox messages with all associations eagerly loaded
     * Fetches: sender, recipient, resource, resource.category, resource.location
     */
    @Query("SELECT DISTINCT m FROM Message m " +
            "LEFT JOIN FETCH m.sender s " +
            "LEFT JOIN FETCH m.recipient r " +
            "LEFT JOIN FETCH m.resource res " +
            "LEFT JOIN FETCH res.category " +
            "LEFT JOIN FETCH res.location " +
            "WHERE m.recipient = :recipient " +
            "ORDER BY m.createdAt DESC")
    Page<Message> findInboxWithAssociations(@Param("recipient") User recipient, Pageable pageable);

    /**
     * Get sent messages with all associations eagerly loaded
     * Fetches: sender, recipient, resource, resource.category, resource.location
     */
    @Query("SELECT DISTINCT m FROM Message m " +
            "LEFT JOIN FETCH m.sender s " +
            "LEFT JOIN FETCH m.recipient r " +
            "LEFT JOIN FETCH m.resource res " +
            "LEFT JOIN FETCH res.category " +
            "LEFT JOIN FETCH res.location " +
            "WHERE m.sender = :sender " +
            "ORDER BY m.createdAt DESC")
    Page<Message> findSentWithAssociations(@Param("sender") User sender, Pageable pageable);

    /**
     * Get unread messages with all associations eagerly loaded
     * Fetches: sender, recipient, resource, resource.category, resource.location
     */
    @Query("SELECT DISTINCT m FROM Message m " +
            "LEFT JOIN FETCH m.sender s " +
            "LEFT JOIN FETCH m.recipient r " +
            "LEFT JOIN FETCH m.resource res " +
            "LEFT JOIN FETCH res.category " +
            "LEFT JOIN FETCH res.location " +
            "WHERE m.recipient = :recipient AND m.isRead = false " +
            "ORDER BY m.createdAt DESC")
    Page<Message> findUnreadWithAssociations(@Param("recipient") User recipient, Pageable pageable);

    /**
     * Get messages by resource with all associations eagerly loaded
     * Fetches: sender, recipient, resource, resource.category, resource.location
     */
    @Query("SELECT DISTINCT m FROM Message m " +
            "LEFT JOIN FETCH m.sender s " +
            "LEFT JOIN FETCH m.recipient r " +
            "LEFT JOIN FETCH m.resource res " +
            "LEFT JOIN FETCH res.category " +
            "LEFT JOIN FETCH res.location " +
            "WHERE m.resource.id = :resourceId " +
            "ORDER BY m.createdAt DESC")
    Page<Message> findByResourceIdWithAssociations(@Param("resourceId") Long resourceId, Pageable pageable);

    /**
     * Get single message by ID with all associations (for detailed view)
     * Fetches: sender, recipient, resource, resource.category, resource.location
     */
    @Query("SELECT m FROM Message m " +
            "LEFT JOIN FETCH m.sender s " +
            "LEFT JOIN FETCH m.recipient r " +
            "LEFT JOIN FETCH m.resource res " +
            "LEFT JOIN FETCH res.category " +
            "LEFT JOIN FETCH res.location " +
            "WHERE m.id = :messageId")
    java.util.Optional<Message> findByIdWithAssociations(@Param("messageId") Long messageId);



    @Query("SELECT m FROM Message m " +
            "LEFT JOIN FETCH m.sender " +
            "LEFT JOIN FETCH m.recipient " +
            "WHERE m.sender = :sender " +
            "ORDER BY m.createdAt DESC")
    Page<Message> findSentWithUsersOnly(@Param("sender") User sender, Pageable pageable);


    Long countByResourceUserId(Long userId);



    /**
     * Get distinct users the current user has sent messages to
     */
    @Query("SELECT DISTINCT m.recipient FROM Message m " +
            "WHERE m.sender = :user AND m.deletedBySender = false")
    java.util.List<User> findDistinctRecipientsByUser(@Param("user") User user);

    /**
     * Get distinct users who have sent messages to the current user
     */
    @Query("SELECT DISTINCT m.sender FROM Message m " +
            "WHERE m.recipient = :user AND m.deletedByRecipient = false")
    java.util.List<User> findDistinctSendersByUser(@Param("user") User user);

}
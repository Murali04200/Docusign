package com.example.Docusign.repository;

import com.example.Docusign.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Find by user
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
    Page<Notification> findByUserId(String userId, Pageable pageable);

    // Find unread notifications
    List<Notification> findByUserIdAndIsReadOrderByCreatedAtDesc(String userId, boolean isRead);
    long countByUserIdAndIsRead(String userId, boolean isRead);

    // Find by type
    List<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(String userId, String type);

    // Find recent unread
    List<Notification> findTop10ByUserIdAndIsReadOrderByCreatedAtDesc(String userId, boolean isRead);

    // Find by envelope
    List<Notification> findByRelatedEnvelopeIdOrderByCreatedAtDesc(String envelopeId);
}

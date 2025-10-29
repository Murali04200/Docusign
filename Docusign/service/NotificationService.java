package com.example.Docusign.service;

import com.example.Docusign.model.Notification;
import com.example.Docusign.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Get all notifications for user
     */
    public List<Notification> getUserNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get unread notifications
     */
    public List<Notification> getUnreadNotifications(String userId) {
        return notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false);
    }

    /**
     * Get unread count
     */
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    /**
     * Mark notification as read
     */
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        });
    }

    /**
     * Mark all as read
     */
    public void markAllAsRead(String userId) {
        List<Notification> unread = notificationRepository
            .findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false);

        for (Notification notification : unread) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
        }

        notificationRepository.saveAll(unread);
        log.info("Marked {} notifications as read for user {}", unread.size(), userId);
    }

    /**
     * Delete notification
     */
    public boolean deleteNotification(Long notificationId, String userId) {
        return notificationRepository.findById(notificationId)
            .filter(n -> n.getUserId().equals(userId))
            .map(n -> {
                notificationRepository.delete(n);
                return true;
            })
            .orElse(false);
    }

    /**
     * Get recent unread notifications (limited)
     */
    public List<Notification> getRecentUnread(String userId, int limit) {
        if (limit <= 10) {
            return notificationRepository.findTop10ByUserIdAndIsReadOrderByCreatedAtDesc(userId, false);
        }
        return notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false);
    }
}

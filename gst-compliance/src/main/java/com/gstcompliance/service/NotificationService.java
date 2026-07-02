package com.gstcompliance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gstcompliance.exception.ResourceNotFoundException;
import com.gstcompliance.model.Notification;
import com.gstcompliance.model.User;
import com.gstcompliance.repository.NotificationRepository;
import com.gstcompliance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // Create notification
    @Transactional
    public Notification createNotification(String email,
                                           Notification.NotificationType type,
                                           String title,
                                           String message,
                                           String actionUrl,
                                           Map<String, Object> metadata) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        String metadataJson = null;
        if (metadata != null) {
            try {
                metadataJson = objectMapper.writeValueAsString(metadata);
            } catch (Exception e) {
                log.warn("Failed to serialize metadata: {}", e.getMessage());
            }
        }

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .actionUrl(actionUrl)
                .metadata(metadataJson)
                .status(Notification.NotificationStatus.UNREAD)
                .build();

        return notificationRepository.save(notification);
    }

    // Get all notifications for user
    public List<Notification> getUserNotifications(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    // Get paginated notifications
    public Page<Notification> getUserNotifications(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        return notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                user.getId(),
                Notification.NotificationStatus.UNREAD,
                pageable
        );
    }

    // Mark as read
    @Transactional
    public void markAsRead(UUID notificationId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Notification not found");
        }

        notification.setStatus(Notification.NotificationStatus.READ);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    // Mark all as read
    @Transactional
    public void markAllAsRead(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndStatusAndCreatedAtAfter(
                user.getId(),
                Notification.NotificationStatus.UNREAD,
                LocalDateTime.now().minusDays(30)
        );

        unreadNotifications.forEach(n -> {
            n.setStatus(Notification.NotificationStatus.READ);
            n.setReadAt(LocalDateTime.now());
        });

        notificationRepository.saveAll(unreadNotifications);
    }

    // Delete notification
    @Transactional
    public void deleteNotification(UUID notificationId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Notification not found");
        }

        notificationRepository.delete(notification);
    }

    // Get unread count
    public long getUnreadCount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        return notificationRepository.countByUserIdAndStatus(
                user.getId(),
                Notification.NotificationStatus.UNREAD
        );
    }

    // Clean up expired notifications
    @Transactional
    public void cleanupExpiredNotifications() {
        LocalDateTime now = LocalDateTime.now();
        notificationRepository.deleteByExpiresAtBefore(now);
        log.info("Cleaned up expired notifications");
    }

    // Async notification creators
    @Async
    public void notifyDeadlineWarning(String email, String formType, String dueDate) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("formType", formType);
        metadata.put("dueDate", dueDate);

        createNotification(
                email,
                Notification.NotificationType.DEADLINE_WARNING,
                "GST Filing Due Soon",
                String.format("Your %s filing is due on %s. Please complete it before the deadline.", formType, dueDate),
                "/compliance",
                metadata
        );
    }

    @Async
    public void notifyDeadlineTomorrow(String email, String formType, String dueDate) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("formType", formType);
        metadata.put("dueDate", dueDate);

        createNotification(
                email,
                Notification.NotificationType.DEADLINE_TOMORROW,
                "GST Filing Due Tomorrow",
                String.format("Your %s filing is due tomorrow (%s). File now to avoid penalties.", formType, dueDate),
                "/compliance",
                metadata
        );
    }

    @Async
    public void notifyReturnGenerated(String email, String returnType, String period) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("returnType", returnType);
        metadata.put("period", period);

        createNotification(
                email,
                Notification.NotificationType.RETURN_GENERATED,
                "Return Generated",
                String.format("Your %s return for %s has been generated. Please review and file.", returnType, period),
                "/compliance",
                metadata
        );
    }

    @Async
    public void notifyReconciliationCompleted(String email, String period, int matchedCount, int mismatchCount) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("period", period);
        metadata.put("matchedCount", matchedCount);
        metadata.put("mismatchCount", mismatchCount);

        createNotification(
                email,
                Notification.NotificationType.RECONCILIATION_COMPLETED,
                "Reconciliation Completed",
                String.format("Reconciliation for %s completed. %d matched, %d mismatches.", period, matchedCount, mismatchCount),
                "/reconciliation",
                metadata
        );
    }

    @Async
    public void notifyImportCompleted(String email, String source, int recordCount) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", source);
        metadata.put("recordCount", recordCount);

        createNotification(
                email,
                Notification.NotificationType.IMPORT_COMPLETED,
                "Import Completed",
                String.format("Successfully imported %d records from %s.", recordCount, source),
                "/gstr2b",
                metadata
        );
    }

    @Async
    public void notifyHighIbcRisk(String email, String period, double riskAmount) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("period", period);
        metadata.put("riskAmount", riskAmount);

        createNotification(
                email,
                Notification.NotificationType.HIGH_ITC_RISK,
                "High ITC Risk Detected",
                String.format("High ITC risk of ₹%.2f detected for %s. Please review your invoices.", riskAmount, period),
                "/reconciliation",
                metadata
        );
    }

    @Async
    public void notifyManualReviewRequired(String email, UUID invoiceId, String reason) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("invoiceId", invoiceId);
        metadata.put("reason", reason);

        createNotification(
                email,
                Notification.NotificationType.MANUAL_REVIEW_REQUIRED,
                "Manual Review Required",
                String.format("Invoice requires manual review: %s", reason),
                "/invoices/" + invoiceId,
                metadata
        );
    }

    @Async
    public void notifyPenaltyCalculated(String email, String period, double penaltyAmount) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("period", period);
        metadata.put("penaltyAmount", penaltyAmount);

        createNotification(
                email,
                Notification.NotificationType.PENALTY_CALCULATED,
                "Penalty Calculated",
                String.format("Penalty of ₹%.2f calculated for late filing of %s.", penaltyAmount, period),
                "/compliance",
                metadata
        );
    }

    // Legacy email methods (kept for backward compatibility)
    @Async
    public void sendEmail(String to, String subject, String body) {
        log.info("📧 EMAIL would be sent to: {}, subject: {}", to, subject);
        log.info("📧 Body: {}", body);
    }

    @Async
    public void sendDeadlineAlert(String to, String formType, String dueDate) {
        log.info("📧 Deadline alert would be sent to: {}", to);
        log.info("📧 Form: {}, Due: {}", formType, dueDate);
    }

    @Async
    public void sendReconciliationReport(String to, int matchedCount, int mismatchCount, String period) {
        log.info("📧 Reconciliation report would be sent to: {}", to);
        log.info("📧 Period: {}, Matched: {}, Mismatches: {}", period, matchedCount, mismatchCount);
    }

    public void sendWhatsAppMessage(String phone, String message) {
        log.info("📱 WhatsApp would be sent to {}: {}", phone, message);
    }

    public Map<String, Boolean> getNotificationStatus() {
        Map<String, Boolean> status = new HashMap<>();
        status.put("email", true);
        status.put("whatsapp", false);
        status.put("sms", false);
        return status;
    }
}
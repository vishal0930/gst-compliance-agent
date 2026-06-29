package com.gstcompliance.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class NotificationService {

    // No JavaMailSender dependency - just logs messages

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
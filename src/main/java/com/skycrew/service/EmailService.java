package com.skycrew.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Email delivery service wrapping Spring's JavaMailSender.
 * Logs emails when notifications are disabled or mail server is unavailable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${skycrew.notifications.enabled:false}")
    private boolean notificationsEnabled;

    @Value("${skycrew.notifications.from-email:noreply@skycrew.com}")
    private String fromEmail;

    /**
     * Sends a plain-text email.
     * @return true if sent successfully (or logged when disabled), false on failure
     */
    public boolean sendEmail(String to, String subject, String body) {
        if (!notificationsEnabled) {
            log.info("[EMAIL-DRY-RUN] To: {}, Subject: {}, Body: {}", to, subject, body);
            return true; // Simulated success
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("[EMAIL-SENT] To: {}, Subject: {}", to, subject);
            return true;
        } catch (Exception e) {
            log.error("[EMAIL-FAILED] To: {}, Subject: {}, Error: {}", to, subject, e.getMessage());
            return false;
        }
    }
}

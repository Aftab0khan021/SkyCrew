package com.skycrew.service;

import com.skycrew.dto.ConflictReport;
import com.skycrew.dto.NotificationPreferenceRequest;
import com.skycrew.dto.PagedResponse;
import com.skycrew.exception.ResourceNotFoundException;
import com.skycrew.model.*;
import com.skycrew.repository.NotificationPreferenceRepository;
import com.skycrew.repository.NotificationRepository;
import com.skycrew.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Notification service — creates, queues, and processes notifications
 * for schedule changes, conflict alerts, and system events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * Creates a notification when a roster assignment changes (created/deleted).
     */
    @Transactional
    public void notifyScheduleChange(Roster roster, String changeType) {
        CrewMember crew = roster.getCrewMember();
        Flight flight = roster.getFlight();

        String subject = String.format("[SkyCrew] Schedule %s: Flight %s",
                changeType, flight.getFlightNumber());

        String body = String.format(
                "Hello %s,\n\n" +
                "Your schedule has been %s.\n\n" +
                "Flight: %s\n" +
                "Route: %s → %s\n" +
                "Departure: %s\n" +
                "Arrival: %s\n" +
                "Status: %s\n\n" +
                "— SkyCrew Roster System",
                crew.getName(), changeType.toLowerCase(),
                flight.getFlightNumber(),
                flight.getOrigin(), flight.getDestination(),
                flight.getDepartureTime(), flight.getArrivalTime(),
                roster.getStatus());

        Notification notification = Notification.builder()
                .recipientEmail(crew.getName().toLowerCase().replaceAll("\\s+", ".") + "@skycrew.com")
                .notificationType(NotificationType.EMAIL)
                .subject(subject)
                .body(body)
                .status(NotificationStatus.PENDING)
                .relatedRosterId(roster.getRosterId())
                .build();

        notificationRepository.save(notification);
        log.info("[NOTIFICATION-QUEUED] {} for crew {} on flight {}",
                changeType, crew.getName(), flight.getFlightNumber());
    }

    /**
     * Creates a notification when a scheduling conflict is detected.
     */
    @Transactional
    public void notifyConflictDetected(ConflictReport conflict) {
        String subject = String.format("[SkyCrew] ⚠️ Conflict Alert: %s",
                conflict.getConflictType());

        Notification notification = Notification.builder()
                .recipientEmail("admin@skycrew.com")
                .notificationType(NotificationType.EMAIL)
                .subject(subject)
                .body(conflict.getMessage())
                .status(NotificationStatus.PENDING)
                .build();

        notificationRepository.save(notification);
    }

    /**
     * Scheduled job: processes all pending notifications every 30 seconds.
     */
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void processPendingNotifications() {
        List<Notification> pending = notificationRepository.findByStatus(NotificationStatus.PENDING);

        for (Notification notification : pending) {
            try {
                boolean sent = emailService.sendEmail(
                        notification.getRecipientEmail(),
                        notification.getSubject(),
                        notification.getBody());

                notification.setStatus(sent ? NotificationStatus.SENT : NotificationStatus.FAILED);
                notification.setSentAt(sent ? LocalDateTime.now() : null);
            } catch (Exception e) {
                notification.setStatus(NotificationStatus.FAILED);
                log.error("[NOTIFICATION-ERROR] Failed to process notification {}: {}",
                        notification.getId(), e.getMessage());
            }
            notificationRepository.save(notification);
        }

        if (!pending.isEmpty()) {
            log.info("[NOTIFICATION-BATCH] Processed {} notifications", pending.size());
        }
    }

    /**
     * Returns paginated notification history.
     */
    @Transactional(readOnly = true)
    public PagedResponse<Notification> getNotifications(Pageable pageable) {
        Page<Notification> page = notificationRepository.findAll(pageable);
        return PagedResponse.<Notification>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    /**
     * Gets notification preferences for a user. Creates default if none exist.
     */
    @Transactional
    public NotificationPreference getPreferences(Long userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                    NotificationPreference pref = NotificationPreference.builder()
                            .user(user)
                            .emailEnabled(true)
                            .smsEnabled(false)
                            .build();
                    return preferenceRepository.save(pref);
                });
    }

    /**
     * Updates notification preferences for a user.
     */
    @Transactional
    public NotificationPreference updatePreferences(Long userId, NotificationPreferenceRequest request) {
        NotificationPreference pref = getPreferences(userId);
        pref.setEmailEnabled(request.isEmailEnabled());
        pref.setSmsEnabled(request.isSmsEnabled());
        pref.setPhoneNumber(request.getPhoneNumber());
        return preferenceRepository.save(pref);
    }
}

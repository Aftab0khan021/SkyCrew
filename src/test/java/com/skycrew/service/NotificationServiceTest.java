package com.skycrew.service;

import com.skycrew.model.*;
import com.skycrew.repository.NotificationPreferenceRepository;
import com.skycrew.repository.NotificationRepository;
import com.skycrew.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("Should create PENDING notification on schedule change")
    void shouldCreateNotification_WhenScheduleChanges() {
        // Arrange
        CockpitCrew crew = new CockpitCrew();
        crew.setCrewId(1L);
        crew.setName("Captain Smith");

        Flight flight = new Flight();
        flight.setFlightId(1L);
        flight.setFlightNumber("SK101");
        flight.setOrigin("JFK");
        flight.setDestination("LAX");
        flight.setDepartureTime(LocalDateTime.of(2026, 12, 1, 10, 0));
        flight.setArrivalTime(LocalDateTime.of(2026, 12, 1, 16, 0));

        Roster roster = new Roster();
        roster.setRosterId(1L);
        roster.setCrewMember(crew);
        roster.setFlight(flight);
        roster.setStatus(RosterStatus.CONFIRMED);

        // Act
        notificationService.notifyScheduleChange(roster, "ASSIGNED");

        // Assert
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getNotificationType()).isEqualTo(NotificationType.EMAIL);
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(saved.getSubject()).contains("SK101");
        assertThat(saved.getBody()).contains("Captain Smith");
        assertThat(saved.getRelatedRosterId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should process pending notifications and mark as SENT")
    void shouldProcessPending_AndMarkAsSent() {
        // Arrange
        Notification pending = Notification.builder()
                .id(1L)
                .recipientEmail("test@skycrew.com")
                .subject("Test")
                .body("Test body")
                .status(NotificationStatus.PENDING)
                .notificationType(NotificationType.EMAIL)
                .build();

        when(notificationRepository.findByStatus(NotificationStatus.PENDING))
                .thenReturn(List.of(pending));
        when(emailService.sendEmail(anyString(), anyString(), anyString()))
                .thenReturn(true);

        // Act
        notificationService.processPendingNotifications();

        // Assert
        verify(notificationRepository).save(argThat(n ->
                n.getStatus() == NotificationStatus.SENT && n.getSentAt() != null));
    }

    @Test
    @DisplayName("Should mark notification as FAILED when email fails")
    void shouldMarkAsFailed_WhenEmailFails() {
        Notification pending = Notification.builder()
                .id(1L)
                .recipientEmail("test@skycrew.com")
                .subject("Test")
                .body("Test body")
                .status(NotificationStatus.PENDING)
                .notificationType(NotificationType.EMAIL)
                .build();

        when(notificationRepository.findByStatus(NotificationStatus.PENDING))
                .thenReturn(List.of(pending));
        when(emailService.sendEmail(anyString(), anyString(), anyString()))
                .thenReturn(false);

        notificationService.processPendingNotifications();

        verify(notificationRepository).save(argThat(n ->
                n.getStatus() == NotificationStatus.FAILED));
    }

    @Test
    @DisplayName("Should do nothing when no pending notifications")
    void shouldDoNothing_WhenNoPending() {
        when(notificationRepository.findByStatus(NotificationStatus.PENDING))
                .thenReturn(Collections.emptyList());

        notificationService.processPendingNotifications();

        verify(notificationRepository, never()).save(any());
    }
}

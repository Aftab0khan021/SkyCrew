package com.skycrew.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * User notification preferences — controls whether they receive email/SMS alerts.
 */
@Entity
@Table(name = "notification_preference")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationPreference extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "email_enabled")
    private boolean emailEnabled = true;

    @Column(name = "sms_enabled")
    private boolean smsEnabled = false;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;
}

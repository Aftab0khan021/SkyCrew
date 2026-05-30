-- ============================================
-- V3: Notification System Tables
-- ============================================

-- Notification log
CREATE TABLE notification (
    id BIGSERIAL PRIMARY KEY,
    recipient_email VARCHAR(255),
    recipient_phone VARCHAR(50),
    notification_type VARCHAR(20) NOT NULL,
    subject VARCHAR(500),
    body TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    related_roster_id BIGINT,
    sent_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    CONSTRAINT fk_notification_roster FOREIGN KEY (related_roster_id) REFERENCES roster(roster_id)
);

-- User notification preferences
CREATE TABLE notification_preference (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    email_enabled BOOLEAN DEFAULT TRUE,
    sms_enabled BOOLEAN DEFAULT FALSE,
    phone_number VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_pref_user FOREIGN KEY (user_id) REFERENCES app_users(id)
);

CREATE INDEX idx_notification_status ON notification(status);
CREATE INDEX idx_notification_type ON notification(notification_type);

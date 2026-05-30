-- ============================================
-- V2: Seed default admin user
-- Password: admin123 (BCrypt encoded)
-- ============================================

INSERT INTO app_users (username, password, role, created_at, updated_at, created_by)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM');

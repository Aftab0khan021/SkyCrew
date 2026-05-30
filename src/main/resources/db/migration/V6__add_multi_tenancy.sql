-- ============================================
-- V6: Multi-Tenancy Support
-- ============================================

ALTER TABLE crew_member ADD COLUMN tenant_id VARCHAR(50) NOT NULL DEFAULT 'DEFAULT';
ALTER TABLE flight ADD COLUMN tenant_id VARCHAR(50) NOT NULL DEFAULT 'DEFAULT';
ALTER TABLE roster ADD COLUMN tenant_id VARCHAR(50) NOT NULL DEFAULT 'DEFAULT';
ALTER TABLE app_users ADD COLUMN tenant_id VARCHAR(50) NOT NULL DEFAULT 'DEFAULT';

CREATE INDEX idx_crew_tenant ON crew_member(tenant_id);
CREATE INDEX idx_flight_tenant ON flight(tenant_id);
CREATE INDEX idx_roster_tenant ON roster(tenant_id);
CREATE INDEX idx_users_tenant ON app_users(tenant_id);

-- ============================================
-- V5: Flight Duty Period (FDP) Columns
-- ============================================

ALTER TABLE flight ADD COLUMN report_time_minutes INT NOT NULL DEFAULT 60;
ALTER TABLE flight ADD COLUMN debrief_time_minutes INT NOT NULL DEFAULT 30;

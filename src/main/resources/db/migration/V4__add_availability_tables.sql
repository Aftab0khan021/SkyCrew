-- ============================================
-- V4: Crew Availability Calendar
-- ============================================

CREATE TABLE crew_availability (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    crew_id BIGINT NOT NULL,
    availability_type VARCHAR(30) NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    notes VARCHAR(500),
    approved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    CONSTRAINT fk_availability_crew FOREIGN KEY (crew_id) REFERENCES crew_member(crew_id)
);

CREATE INDEX idx_availability_crew ON crew_availability(crew_id);
CREATE INDEX idx_availability_dates ON crew_availability(start_date, end_date);
CREATE INDEX idx_availability_type ON crew_availability(availability_type);

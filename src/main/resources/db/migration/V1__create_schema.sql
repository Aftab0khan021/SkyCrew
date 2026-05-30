-- ============================================
-- V1: SkyCrew Initial Schema
-- ============================================

-- Application Users (for authentication)
CREATE TABLE app_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

-- Crew Members (Single Table Inheritance)
CREATE TABLE crew_member (
    crew_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    crew_type VARCHAR(31) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    base_airport VARCHAR(3) NOT NULL,
    max_monthly_hours INT NOT NULL DEFAULT 100,
    -- CockpitCrew fields
    license_number VARCHAR(50),
    type_ratings VARCHAR(500),
    -- CabinCrew fields
    languages_spoken VARCHAR(500),
    safety_training_expiry DATE,
    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

-- Flights
CREATE TABLE flight (
    flight_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    flight_number VARCHAR(20) NOT NULL,
    origin VARCHAR(3) NOT NULL,
    destination VARCHAR(3) NOT NULL,
    departure_time TIMESTAMP NOT NULL,
    arrival_time TIMESTAMP NOT NULL,
    required_pilots INT NOT NULL DEFAULT 2,
    required_cabin_crew INT NOT NULL DEFAULT 4,
    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

-- Roster (Crew-to-Flight assignments)
CREATE TABLE roster (
    roster_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    crew_id BIGINT NOT NULL,
    flight_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED',
    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    -- Foreign Keys
    CONSTRAINT fk_roster_crew FOREIGN KEY (crew_id) REFERENCES crew_member(crew_id),
    CONSTRAINT fk_roster_flight FOREIGN KEY (flight_id) REFERENCES flight(flight_id)
);

-- Indexes for performance
CREATE INDEX idx_crew_role ON crew_member(role);
CREATE INDEX idx_crew_base ON crew_member(base_airport);
CREATE INDEX idx_flight_number ON flight(flight_number);
CREATE INDEX idx_flight_departure ON flight(departure_time);
CREATE INDEX idx_roster_crew ON roster(crew_id);
CREATE INDEX idx_roster_flight ON roster(flight_id);
CREATE INDEX idx_roster_status ON roster(status);

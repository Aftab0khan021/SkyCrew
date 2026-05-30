package com.skycrew.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a scheduled flight with route, timing, and crew requirements.
 */
@Entity
@Table(name = "flight")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Flight extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flight_id")
    private Long flightId;

    @Column(name = "flight_number", nullable = false, length = 20)
    private String flightNumber;

    @Column(nullable = false, length = 3)
    private String origin;

    @Column(nullable = false, length = 3)
    private String destination;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "arrival_time", nullable = false)
    private LocalDateTime arrivalTime;

    @Column(name = "required_pilots", nullable = false)
    private int requiredPilots;

    @Column(name = "required_cabin_crew", nullable = false)
    private int requiredCabinCrew;

    // Flight Duty Period (FDP) fields
    @Column(name = "report_time_minutes", nullable = false)
    private int reportTimeMinutes = 60; // Default: 1 hour before departure

    @Column(name = "debrief_time_minutes", nullable = false)
    private int debriefTimeMinutes = 30; // Default: 30 min after arrival

    // Multi-tenancy
    @Column(name = "tenant_id", nullable = false)
    private String tenantId = "DEFAULT";

    @OneToMany(mappedBy = "flight", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Roster> rosters = new ArrayList<>();
}

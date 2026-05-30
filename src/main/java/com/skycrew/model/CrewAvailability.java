package com.skycrew.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Crew availability block — represents periods when a crew member is unavailable
 * due to leave, training, medical, or vacation.
 */
@Entity
@Table(name = "crew_availability")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CrewAvailability extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_id", nullable = false)
    private CrewMember crewMember;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_type", nullable = false)
    private AvailabilityType availabilityType;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(length = 500)
    private String notes;

    private boolean approved;
}

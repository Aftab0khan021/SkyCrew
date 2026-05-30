package com.skycrew.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base entity for all crew members.
 * Uses Single Table Inheritance — CockpitCrew and CabinCrew share the same table
 * with a discriminator column to distinguish types.
 */
@Entity
@Table(name = "crew_member")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "crew_type", discriminatorType = DiscriminatorType.STRING)
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class CrewMember extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crew_id")
    private Long crewId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CrewRole role;

    @Column(name = "base_airport", nullable = false, length = 3)
    private String baseAirport;

    @Column(name = "max_monthly_hours", nullable = false)
    private int maxMonthlyHours;

    // Multi-tenancy
    @Column(name = "tenant_id", nullable = false)
    private String tenantId = "DEFAULT";

    @OneToMany(mappedBy = "crewMember", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Roster> rosters = new ArrayList<>();
}

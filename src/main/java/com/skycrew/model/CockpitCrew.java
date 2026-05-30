package com.skycrew.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents cockpit crew members: Captains and First Officers.
 * Extends CrewMember with aviation-specific certifications.
 */
@Entity
@DiscriminatorValue("COCKPIT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CockpitCrew extends CrewMember {

    @Column(name = "license_number")
    private String licenseNumber;

    /**
     * Comma-separated list of aircraft type ratings (e.g., "B737,A320,B787").
     */
    @Column(name = "type_ratings")
    private String typeRatings;
}

package com.skycrew.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Represents cabin crew members: Pursers and Flight Attendants.
 * Extends CrewMember with cabin-specific attributes.
 */
@Entity
@DiscriminatorValue("CABIN")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CabinCrew extends CrewMember {

    /**
     * Comma-separated list of spoken languages (e.g., "English,French,Arabic").
     */
    @Column(name = "languages_spoken")
    private String languagesSpoken;

    @Column(name = "safety_training_expiry")
    private LocalDate safetyTrainingExpiry;
}

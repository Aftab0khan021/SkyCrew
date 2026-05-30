package com.skycrew.dto;

import com.skycrew.model.CrewRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrewMemberResponse {

    private Long crewId;
    private String name;
    private CrewRole role;
    private String baseAirport;
    private int maxMonthlyHours;
    private String crewType;

    // CockpitCrew specific
    private String licenseNumber;
    private String typeRatings;

    // CabinCrew specific
    private String languagesSpoken;
    private LocalDate safetyTrainingExpiry;

    // Audit fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}

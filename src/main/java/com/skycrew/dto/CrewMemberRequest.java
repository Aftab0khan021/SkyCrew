package com.skycrew.dto;

import com.skycrew.model.CrewRole;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrewMemberRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Role is required")
    private CrewRole role;

    @NotBlank(message = "Base airport is required")
    @Size(min = 3, max = 3, message = "Base airport must be a 3-letter IATA code")
    private String baseAirport;

    @Min(value = 1, message = "Maximum monthly hours must be at least 1")
    @Max(value = 200, message = "Maximum monthly hours cannot exceed 200")
    private int maxMonthlyHours;

    @NotBlank(message = "Crew type is required (COCKPIT or CABIN)")
    private String crewType;

    // CockpitCrew specific
    private String licenseNumber;
    private String typeRatings;

    // CabinCrew specific
    private String languagesSpoken;
    private LocalDate safetyTrainingExpiry;
}

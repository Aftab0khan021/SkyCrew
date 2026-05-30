package com.skycrew.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RosterAssignRequest {

    @NotNull(message = "Crew ID is required")
    private Long crewId;

    @NotNull(message = "Flight ID is required")
    private Long flightId;
}

package com.skycrew.dto;

import com.skycrew.model.AvailabilityType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AvailabilityRequest {

    @NotNull(message = "Availability type is required")
    private AvailabilityType availabilityType;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    private String notes;
    private boolean approved;
}

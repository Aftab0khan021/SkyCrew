package com.skycrew.dto;

import com.skycrew.model.AvailabilityType;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AvailabilityResponse {

    private Long id;
    private Long crewId;
    private String crewName;
    private AvailabilityType availabilityType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String notes;
    private boolean approved;
    private LocalDateTime createdAt;
    private String createdBy;
}

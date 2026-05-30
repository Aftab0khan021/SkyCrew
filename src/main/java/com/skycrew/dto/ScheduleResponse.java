package com.skycrew.dto;

import com.skycrew.model.RosterStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponse {

    private Long rosterId;
    private RosterStatus status;

    // Flight details
    private Long flightId;
    private String flightNumber;
    private String origin;
    private String destination;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;

    // Assignment audit
    private LocalDateTime assignedAt;
    private String assignedBy;
}

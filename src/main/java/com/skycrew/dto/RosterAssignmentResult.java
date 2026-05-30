package com.skycrew.dto;

import com.skycrew.model.RosterStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RosterAssignmentResult {

    private Long rosterId;
    private Long crewId;
    private Long flightId;
    private RosterStatus status;
    private boolean success;
    private String message;
    private List<String> warnings;

    // Audit
    private LocalDateTime createdAt;
    private String createdBy;
}

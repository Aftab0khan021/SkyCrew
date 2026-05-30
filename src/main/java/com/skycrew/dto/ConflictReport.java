package com.skycrew.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictReport {

    public enum ConflictType {
        OVERLAP,
        FATIGUE,
        HOURS_EXCEEDED,
        CREW_COMPLEMENT_EXCEEDED,
        FDP_EXCEEDED,
        CUMULATIVE_DUTY_EXCEEDED,
        UNAVAILABLE
    }

    private Long crewId;
    private String crewName;
    private ConflictType conflictType;
    private Long flightAId;
    private String flightANumber;
    private Long flightBId;
    private String flightBNumber;
    private String message;
}

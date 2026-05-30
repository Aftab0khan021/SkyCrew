package com.skycrew.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class MonthlyHoursExceededException extends RuntimeException {

    public MonthlyHoursExceededException(String message) {
        super(message);
    }

    public MonthlyHoursExceededException(Long crewId, double currentHours, double maxHours) {
        super(String.format("Crew member %d would exceed monthly flying hours limit. " +
                "Current: %.1f hrs, Max: %.1f hrs", crewId, currentHours, maxHours));
    }
}

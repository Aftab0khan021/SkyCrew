package com.skycrew.exception;

import com.skycrew.dto.ConflictReport;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@ResponseStatus(HttpStatus.CONFLICT)
public class SchedulingConflictException extends RuntimeException {

    private final List<ConflictReport> conflicts;

    public SchedulingConflictException(String message, List<ConflictReport> conflicts) {
        super(message);
        this.conflicts = conflicts;
    }

    public SchedulingConflictException(String message) {
        super(message);
        this.conflicts = List.of();
    }

    public List<ConflictReport> getConflicts() {
        return conflicts;
    }
}

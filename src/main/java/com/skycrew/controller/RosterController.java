package com.skycrew.controller;

import com.skycrew.dto.*;
import com.skycrew.service.RosterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roster")
@RequiredArgsConstructor
@Tag(name = "Roster", description = "Crew-to-flight assignment and conflict detection")
public class RosterController {

    private final RosterService rosterService;

    @PostMapping("/assign")
    @Operation(summary = "Assign crew member to flight",
               description = "Triggers the Smart Rostering Engine to check for overlap, fatigue, " +
                       "and hours violations before saving. Requires ADMIN role.")
    @ApiResponse(responseCode = "201", description = "Assignment successful")
    @ApiResponse(responseCode = "409", description = "Scheduling conflict detected")
    @ApiResponse(responseCode = "404", description = "Crew member or flight not found")
    public ResponseEntity<RosterAssignmentResult> assignCrewToFlight(
            @Valid @RequestBody RosterAssignRequest request) {
        RosterAssignmentResult result = rosterService.assignCrewToFlight(
                request.getCrewId(), request.getFlightId());
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @GetMapping("/conflicts")
    @Operation(summary = "Detect all scheduling conflicts",
               description = "Scans all confirmed rosters and returns a list of overlap, " +
                       "fatigue, and hours violations.")
    public ResponseEntity<List<ConflictReport>> detectAllConflicts() {
        return ResponseEntity.ok(rosterService.detectAllConflicts());
    }

    @GetMapping
    @Operation(summary = "List all roster entries (paginated)")
    public ResponseEntity<PagedResponse<RosterAssignmentResult>> getAllRosters(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(rosterService.getAllRosters(pageable));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a roster assignment", description = "Requires ADMIN role.")
    @ApiResponse(responseCode = "204", description = "Assignment removed")
    public ResponseEntity<Void> removeRoster(@PathVariable Long id) {
        rosterService.removeRoster(id);
        return ResponseEntity.noContent().build();
    }
}

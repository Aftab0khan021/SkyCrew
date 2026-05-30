package com.skycrew.controller;

import com.skycrew.dto.CrewMemberRequest;
import com.skycrew.dto.CrewMemberResponse;
import com.skycrew.dto.PagedResponse;
import com.skycrew.dto.ScheduleResponse;
import com.skycrew.service.CrewService;
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

@RestController
@RequestMapping("/api/v1/crew")
@RequiredArgsConstructor
@Tag(name = "Crew", description = "Crew member management and schedule retrieval")
public class CrewController {

    private final CrewService crewService;

    @PostMapping
    @Operation(summary = "Add a new crew member", description = "Creates a cockpit or cabin crew member. Requires ADMIN role.")
    @ApiResponse(responseCode = "201", description = "Crew member created")
    @ApiResponse(responseCode = "400", description = "Invalid crew data")
    public ResponseEntity<CrewMemberResponse> createCrewMember(
            @Valid @RequestBody CrewMemberRequest request) {
        CrewMemberResponse response = crewService.createCrewMember(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List all crew members (paginated)")
    public ResponseEntity<PagedResponse<CrewMemberResponse>> getAllCrewMembers(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(crewService.getAllCrewMembers(pageable));
    }

    @GetMapping("/{crewId}")
    @Operation(summary = "Get crew member by ID")
    @ApiResponse(responseCode = "200", description = "Crew member found")
    @ApiResponse(responseCode = "404", description = "Crew member not found")
    public ResponseEntity<CrewMemberResponse> getCrewMemberById(@PathVariable Long crewId) {
        return ResponseEntity.ok(crewService.getCrewMemberById(crewId));
    }

    @GetMapping("/{crewId}/schedule")
    @Operation(summary = "Get crew member's flight schedule (paginated)",
               description = "Returns the upcoming flight assignments for a specific crew member.")
    public ResponseEntity<PagedResponse<ScheduleResponse>> getCrewSchedule(
            @PathVariable Long crewId,
            @PageableDefault(size = 10, sort = "flight.departureTime") Pageable pageable) {
        return ResponseEntity.ok(crewService.getCrewSchedule(crewId, pageable));
    }

    @DeleteMapping("/{crewId}")
    @Operation(summary = "Delete a crew member", description = "Requires ADMIN role.")
    @ApiResponse(responseCode = "204", description = "Crew member deleted")
    public ResponseEntity<Void> deleteCrewMember(@PathVariable Long crewId) {
        crewService.deleteCrewMember(crewId);
        return ResponseEntity.noContent().build();
    }
}

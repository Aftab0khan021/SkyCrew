package com.skycrew.controller;

import com.skycrew.dto.AvailabilityRequest;
import com.skycrew.dto.AvailabilityResponse;
import com.skycrew.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/crew/{crewId}/availability")
@RequiredArgsConstructor
@Tag(name = "Crew Availability", description = "Manage crew leave, training, medical, and vacation blocks")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @PostMapping
    @Operation(summary = "Add availability block for a crew member",
               description = "Creates a leave/training/medical/vacation block. Requires ADMIN role.")
    public ResponseEntity<AvailabilityResponse> addAvailability(
            @PathVariable Long crewId,
            @Valid @RequestBody AvailabilityRequest request) {
        return new ResponseEntity<>(
                availabilityService.addAvailability(crewId, request),
                HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "View all availability blocks for a crew member")
    public ResponseEntity<List<AvailabilityResponse>> getAvailability(
            @PathVariable Long crewId) {
        return ResponseEntity.ok(availabilityService.getAvailability(crewId));
    }

    @PutMapping("/{availabilityId}")
    @Operation(summary = "Update an availability block (approve/modify)")
    public ResponseEntity<AvailabilityResponse> updateAvailability(
            @PathVariable Long crewId,
            @PathVariable Long availabilityId,
            @Valid @RequestBody AvailabilityRequest request) {
        return ResponseEntity.ok(
                availabilityService.updateAvailability(crewId, availabilityId, request));
    }

    @DeleteMapping("/{availabilityId}")
    @Operation(summary = "Remove an availability block")
    public ResponseEntity<Void> deleteAvailability(
            @PathVariable Long crewId,
            @PathVariable Long availabilityId) {
        availabilityService.deleteAvailability(crewId, availabilityId);
        return ResponseEntity.noContent().build();
    }
}

package com.skycrew.controller;

import com.skycrew.dto.FlightRequest;
import com.skycrew.dto.FlightResponse;
import com.skycrew.dto.PagedResponse;
import com.skycrew.service.FlightService;
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
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
@Tag(name = "Flights", description = "Flight schedule management")
public class FlightController {

    private final FlightService flightService;

    @PostMapping
    @Operation(summary = "Create a new flight", description = "Creates a new flight schedule. Requires ADMIN role.")
    @ApiResponse(responseCode = "201", description = "Flight created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid flight data")
    public ResponseEntity<FlightResponse> createFlight(@Valid @RequestBody FlightRequest request) {
        FlightResponse response = flightService.createFlight(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List all flights (paginated)", description = "Returns paginated list of flights. Accessible by ADMIN and CREW roles.")
    public ResponseEntity<PagedResponse<FlightResponse>> getAllFlights(
            @PageableDefault(size = 20, sort = "departureTime") Pageable pageable) {
        return ResponseEntity.ok(flightService.getAllFlights(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get flight by ID")
    @ApiResponse(responseCode = "200", description = "Flight found")
    @ApiResponse(responseCode = "404", description = "Flight not found")
    public ResponseEntity<FlightResponse> getFlightById(@PathVariable Long id) {
        return ResponseEntity.ok(flightService.getFlightById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a flight", description = "Updates an existing flight. Requires ADMIN role.")
    public ResponseEntity<FlightResponse> updateFlight(
            @PathVariable Long id,
            @Valid @RequestBody FlightRequest request) {
        return ResponseEntity.ok(flightService.updateFlight(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a flight", description = "Deletes a flight. Requires ADMIN role.")
    @ApiResponse(responseCode = "204", description = "Flight deleted successfully")
    public ResponseEntity<Void> deleteFlight(@PathVariable Long id) {
        flightService.deleteFlight(id);
        return ResponseEntity.noContent().build();
    }
}

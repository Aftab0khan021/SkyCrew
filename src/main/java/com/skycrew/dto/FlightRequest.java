package com.skycrew.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightRequest {

    @NotBlank(message = "Flight number is required")
    private String flightNumber;

    @NotBlank(message = "Origin airport is required")
    @Size(min = 3, max = 3, message = "Origin must be a 3-letter IATA code")
    private String origin;

    @NotBlank(message = "Destination airport is required")
    @Size(min = 3, max = 3, message = "Destination must be a 3-letter IATA code")
    private String destination;

    @NotNull(message = "Departure time is required")
    @Future(message = "Departure time must be in the future")
    private LocalDateTime departureTime;

    @NotNull(message = "Arrival time is required")
    @Future(message = "Arrival time must be in the future")
    private LocalDateTime arrivalTime;

    @Min(value = 1, message = "At least 1 pilot is required")
    private int requiredPilots;

    @Min(value = 1, message = "At least 1 cabin crew member is required")
    private int requiredCabinCrew;
}

package com.skycrew.service;

import com.skycrew.dto.FlightRequest;
import com.skycrew.dto.FlightResponse;
import com.skycrew.dto.PagedResponse;
import com.skycrew.exception.ResourceNotFoundException;
import com.skycrew.model.Flight;
import com.skycrew.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing flight CRUD operations.
 */
@Service
@RequiredArgsConstructor
public class FlightService {

    private final FlightRepository flightRepository;

    @Transactional
    public FlightResponse createFlight(FlightRequest request) {
        validateFlightTimes(request);

        Flight flight = new Flight();
        flight.setFlightNumber(request.getFlightNumber());
        flight.setOrigin(request.getOrigin().toUpperCase());
        flight.setDestination(request.getDestination().toUpperCase());
        flight.setDepartureTime(request.getDepartureTime());
        flight.setArrivalTime(request.getArrivalTime());
        flight.setRequiredPilots(request.getRequiredPilots());
        flight.setRequiredCabinCrew(request.getRequiredCabinCrew());

        Flight saved = flightRepository.save(flight);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public FlightResponse getFlightById(Long id) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", id));
        return mapToResponse(flight);
    }

    @Transactional(readOnly = true)
    public PagedResponse<FlightResponse> getAllFlights(Pageable pageable) {
        Page<Flight> page = flightRepository.findAll(pageable);
        return buildPagedResponse(page);
    }

    @Transactional
    public FlightResponse updateFlight(Long id, FlightRequest request) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", "id", id));

        validateFlightTimes(request);

        flight.setFlightNumber(request.getFlightNumber());
        flight.setOrigin(request.getOrigin().toUpperCase());
        flight.setDestination(request.getDestination().toUpperCase());
        flight.setDepartureTime(request.getDepartureTime());
        flight.setArrivalTime(request.getArrivalTime());
        flight.setRequiredPilots(request.getRequiredPilots());
        flight.setRequiredCabinCrew(request.getRequiredCabinCrew());

        Flight updated = flightRepository.save(flight);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteFlight(Long id) {
        if (!flightRepository.existsById(id)) {
            throw new ResourceNotFoundException("Flight", "id", id);
        }
        flightRepository.deleteById(id);
    }

    // --- Private helpers ---

    private void validateFlightTimes(FlightRequest request) {
        if (request.getArrivalTime().isBefore(request.getDepartureTime()) ||
                request.getArrivalTime().isEqual(request.getDepartureTime())) {
            throw new IllegalArgumentException(
                    "Arrival time must be after departure time");
        }
    }

    private FlightResponse mapToResponse(Flight flight) {
        return FlightResponse.builder()
                .flightId(flight.getFlightId())
                .flightNumber(flight.getFlightNumber())
                .origin(flight.getOrigin())
                .destination(flight.getDestination())
                .departureTime(flight.getDepartureTime())
                .arrivalTime(flight.getArrivalTime())
                .requiredPilots(flight.getRequiredPilots())
                .requiredCabinCrew(flight.getRequiredCabinCrew())
                .createdAt(flight.getCreatedAt())
                .updatedAt(flight.getUpdatedAt())
                .createdBy(flight.getCreatedBy())
                .build();
    }

    private PagedResponse<FlightResponse> buildPagedResponse(Page<Flight> page) {
        return PagedResponse.<FlightResponse>builder()
                .content(page.getContent().stream().map(this::mapToResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}

package com.skycrew.service;

import com.skycrew.dto.FlightRequest;
import com.skycrew.dto.FlightResponse;
import com.skycrew.dto.PagedResponse;
import com.skycrew.exception.ResourceNotFoundException;
import com.skycrew.model.Flight;
import com.skycrew.repository.FlightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightServiceTest {

    @Mock
    private FlightRepository flightRepository;

    @InjectMocks
    private FlightService flightService;

    private FlightRequest validRequest;
    private Flight savedFlight;

    @BeforeEach
    void setUp() {
        validRequest = FlightRequest.builder()
                .flightNumber("SK101")
                .origin("JFK")
                .destination("LAX")
                .departureTime(LocalDateTime.of(2026, 8, 1, 10, 0))
                .arrivalTime(LocalDateTime.of(2026, 8, 1, 16, 0))
                .requiredPilots(2)
                .requiredCabinCrew(4)
                .build();

        savedFlight = new Flight();
        savedFlight.setFlightId(1L);
        savedFlight.setFlightNumber("SK101");
        savedFlight.setOrigin("JFK");
        savedFlight.setDestination("LAX");
        savedFlight.setDepartureTime(LocalDateTime.of(2026, 8, 1, 10, 0));
        savedFlight.setArrivalTime(LocalDateTime.of(2026, 8, 1, 16, 0));
        savedFlight.setRequiredPilots(2);
        savedFlight.setRequiredCabinCrew(4);
    }

    @Test
    @DisplayName("Should create flight successfully with valid data")
    void shouldCreateFlight() {
        when(flightRepository.save(any(Flight.class))).thenReturn(savedFlight);

        FlightResponse response = flightService.createFlight(validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getFlightNumber()).isEqualTo("SK101");
        assertThat(response.getOrigin()).isEqualTo("JFK");
        verify(flightRepository).save(any(Flight.class));
    }

    @Test
    @DisplayName("Should reject flight when arrival is before departure")
    void shouldRejectFlight_WhenArrivalBeforeDeparture() {
        FlightRequest badRequest = FlightRequest.builder()
                .flightNumber("SK999")
                .origin("JFK")
                .destination("LAX")
                .departureTime(LocalDateTime.of(2026, 8, 1, 16, 0))
                .arrivalTime(LocalDateTime.of(2026, 8, 1, 10, 0))  // Before departure
                .requiredPilots(2)
                .requiredCabinCrew(4)
                .build();

        assertThatThrownBy(() -> flightService.createFlight(badRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Arrival time must be after departure time");
    }

    @Test
    @DisplayName("Should throw 404 when flight not found by ID")
    void shouldThrow404_WhenFlightNotFound() {
        when(flightRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> flightService.getFlightById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should return paginated flights")
    void shouldReturnPaginatedFlights() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Flight> page = new PageImpl<>(List.of(savedFlight), pageable, 1);
        when(flightRepository.findAll(pageable)).thenReturn(page);

        PagedResponse<FlightResponse> response = flightService.getAllFlights(pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getPage()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should delete flight successfully")
    void shouldDeleteFlight() {
        when(flightRepository.existsById(1L)).thenReturn(true);
        doNothing().when(flightRepository).deleteById(1L);

        flightService.deleteFlight(1L);

        verify(flightRepository).deleteById(1L);
    }
}

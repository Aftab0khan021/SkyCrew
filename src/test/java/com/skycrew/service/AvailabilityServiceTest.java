package com.skycrew.service;

import com.skycrew.dto.AvailabilityRequest;
import com.skycrew.dto.AvailabilityResponse;
import com.skycrew.exception.ResourceNotFoundException;
import com.skycrew.model.*;
import com.skycrew.repository.AvailabilityRepository;
import com.skycrew.repository.CrewMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private AvailabilityRepository availabilityRepository;

    @Mock
    private CrewMemberRepository crewMemberRepository;

    @InjectMocks
    private AvailabilityService availabilityService;

    private CockpitCrew captain;

    @BeforeEach
    void setUp() {
        captain = new CockpitCrew();
        captain.setCrewId(1L);
        captain.setName("Captain Smith");
        captain.setRole(CrewRole.CAPTAIN);
        captain.setBaseAirport("JFK");
        captain.setMaxMonthlyHours(100);
    }

    @Test
    @DisplayName("Should add availability block for crew member")
    void shouldAddAvailability() {
        AvailabilityRequest request = AvailabilityRequest.builder()
                .availabilityType(AvailabilityType.LEAVE)
                .startDate(LocalDateTime.of(2026, 12, 1, 0, 0))
                .endDate(LocalDateTime.of(2026, 12, 15, 0, 0))
                .notes("Annual leave")
                .approved(true)
                .build();

        when(crewMemberRepository.findById(1L)).thenReturn(Optional.of(captain));
        when(availabilityRepository.save(any(CrewAvailability.class))).thenAnswer(inv -> {
            CrewAvailability a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        AvailabilityResponse result = availabilityService.addAvailability(1L, request);

        assertThat(result.getAvailabilityType()).isEqualTo(AvailabilityType.LEAVE);
        assertThat(result.getCrewName()).isEqualTo("Captain Smith");
        assertThat(result.isApproved()).isTrue();
        verify(availabilityRepository).save(any());
    }

    @Test
    @DisplayName("Should reject when end date is before start date")
    void shouldReject_WhenEndDateBeforeStart() {
        AvailabilityRequest request = AvailabilityRequest.builder()
                .availabilityType(AvailabilityType.MEDICAL)
                .startDate(LocalDateTime.of(2026, 12, 15, 0, 0))
                .endDate(LocalDateTime.of(2026, 12, 1, 0, 0))
                .build();

        when(crewMemberRepository.findById(1L)).thenReturn(Optional.of(captain));

        assertThatThrownBy(() -> availabilityService.addAvailability(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End date must be after start date");
    }

    @Test
    @DisplayName("Should return true when crew has no overlapping blocks")
    void shouldReturnAvailable_WhenNoBlocks() {
        when(availabilityRepository.findOverlappingAvailability(anyLong(), any(), any()))
                .thenReturn(Collections.emptyList());

        boolean available = availabilityService.isCrewAvailable(1L,
                LocalDateTime.of(2026, 12, 1, 10, 0),
                LocalDateTime.of(2026, 12, 1, 16, 0));

        assertThat(available).isTrue();
    }

    @Test
    @DisplayName("Should return false when crew has overlapping leave block")
    void shouldReturnUnavailable_WhenLeaveOverlaps() {
        CrewAvailability leave = CrewAvailability.builder()
                .id(1L)
                .crewMember(captain)
                .availabilityType(AvailabilityType.LEAVE)
                .startDate(LocalDateTime.of(2026, 12, 1, 0, 0))
                .endDate(LocalDateTime.of(2026, 12, 15, 0, 0))
                .approved(true)
                .build();

        when(availabilityRepository.findOverlappingAvailability(anyLong(), any(), any()))
                .thenReturn(List.of(leave));

        boolean available = availabilityService.isCrewAvailable(1L,
                LocalDateTime.of(2026, 12, 5, 10, 0),
                LocalDateTime.of(2026, 12, 5, 16, 0));

        assertThat(available).isFalse();
    }

    @Test
    @DisplayName("Should throw when crew member not found")
    void shouldThrow_WhenCrewNotFound() {
        when(crewMemberRepository.findById(999L)).thenReturn(Optional.empty());

        AvailabilityRequest request = AvailabilityRequest.builder()
                .availabilityType(AvailabilityType.TRAINING)
                .startDate(LocalDateTime.of(2026, 12, 1, 0, 0))
                .endDate(LocalDateTime.of(2026, 12, 5, 0, 0))
                .build();

        assertThatThrownBy(() -> availabilityService.addAvailability(999L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

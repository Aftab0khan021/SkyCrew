package com.skycrew.service;

import com.skycrew.dto.ConflictReport;
import com.skycrew.dto.RosterAssignmentResult;
import com.skycrew.exception.SchedulingConflictException;
import com.skycrew.model.*;
import com.skycrew.repository.AvailabilityRepository;
import com.skycrew.repository.CrewMemberRepository;
import com.skycrew.repository.FlightRepository;
import com.skycrew.repository.RosterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the Smart Rostering Engine.
 * Uses Mockito to isolate business logic from the database layer.
 */
@ExtendWith(MockitoExtension.class)
class RosterServiceTest {

    @Mock
    private RosterRepository rosterRepository;

    @Mock
    private CrewMemberRepository crewMemberRepository;

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private AvailabilityRepository availabilityRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private RosterService rosterService;

    private CockpitCrew captain;
    private Flight flightA;
    private Flight flightB;

    @BeforeEach
    void setUp() {
        // Set configurable business rule
        ReflectionTestUtils.setField(rosterService, "minRestHours", 12);

        // Create test crew member
        captain = new CockpitCrew();
        captain.setCrewId(1L);
        captain.setName("Captain Smith");
        captain.setRole(CrewRole.CAPTAIN);
        captain.setBaseAirport("JFK");
        captain.setMaxMonthlyHours(100);
        captain.setLicenseNumber("CPL-12345");

        // Flight A: JFK → LAX, 10:00 - 16:00 (6 hours)
        flightA = new Flight();
        flightA.setFlightId(1L);
        flightA.setFlightNumber("SK101");
        flightA.setOrigin("JFK");
        flightA.setDestination("LAX");
        flightA.setDepartureTime(LocalDateTime.of(2026, 7, 1, 10, 0));
        flightA.setArrivalTime(LocalDateTime.of(2026, 7, 1, 16, 0));
        flightA.setRequiredPilots(2);
        flightA.setRequiredCabinCrew(4);

        // Flight B: LAX → SFO, next day 06:00 - 08:00 (2 hours)
        flightB = new Flight();
        flightB.setFlightId(2L);
        flightB.setFlightNumber("SK202");
        flightB.setOrigin("LAX");
        flightB.setDestination("SFO");
        flightB.setDepartureTime(LocalDateTime.of(2026, 7, 2, 6, 0));
        flightB.setArrivalTime(LocalDateTime.of(2026, 7, 2, 8, 0));
        flightB.setRequiredPilots(2);
        flightB.setRequiredCabinCrew(4);
    }

    @Nested
    @DisplayName("Successful Assignment")
    class SuccessfulAssignment {

        @Test
        @DisplayName("Should assign crew to flight when no conflicts exist")
        void shouldAssignCrewToFlight_WhenNoConflicts() {
            // Arrange
            when(crewMemberRepository.findById(1L)).thenReturn(Optional.of(captain));
            when(flightRepository.findById(1L)).thenReturn(Optional.of(flightA));
            when(rosterRepository.findConflictingRosters(anyLong(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(rosterRepository.findConfirmedRostersByCrewIdOrderByDeparture(1L))
                    .thenReturn(Collections.emptyList());
            when(rosterRepository.findConfirmedRostersInMonth(anyLong(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(rosterRepository.countConfirmedByFlightAndCrewType(anyLong(), any()))
                    .thenReturn(0L);
            when(availabilityRepository.findOverlappingAvailability(anyLong(), any(), any()))
                    .thenReturn(Collections.emptyList());

            Roster savedRoster = new Roster();
            savedRoster.setRosterId(1L);
            savedRoster.setCrewMember(captain);
            savedRoster.setFlight(flightA);
            savedRoster.setStatus(RosterStatus.CONFIRMED);
            when(rosterRepository.save(any(Roster.class))).thenReturn(savedRoster);

            // Act
            RosterAssignmentResult result = rosterService.assignCrewToFlight(1L, 1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getCrewId()).isEqualTo(1L);
            assertThat(result.getFlightId()).isEqualTo(1L);
            assertThat(result.getStatus()).isEqualTo(RosterStatus.CONFIRMED);
            verify(rosterRepository).save(any(Roster.class));
        }

        @Test
        @DisplayName("Should allow assignment when rest period is exactly sufficient (14h rest for 12h min)")
        void shouldAllowAssignment_WhenRestPeriodIsSufficient() {
            // Flight A arrives at 16:00 on Jul 1
            // Flight B departs at 06:00 on Jul 2 — 14 hours rest (> 12h minimum)
            Roster existingRoster = new Roster();
            existingRoster.setRosterId(10L);
            existingRoster.setCrewMember(captain);
            existingRoster.setFlight(flightA);
            existingRoster.setStatus(RosterStatus.CONFIRMED);

            when(crewMemberRepository.findById(1L)).thenReturn(Optional.of(captain));
            when(flightRepository.findById(2L)).thenReturn(Optional.of(flightB));
            when(rosterRepository.findConflictingRosters(anyLong(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(rosterRepository.findConfirmedRostersByCrewIdOrderByDeparture(1L))
                    .thenReturn(List.of(existingRoster));
            when(rosterRepository.findConfirmedRostersInMonth(anyLong(), any(), any()))
                    .thenReturn(List.of(existingRoster));
            when(rosterRepository.countConfirmedByFlightAndCrewType(anyLong(), any()))
                    .thenReturn(0L);
            when(availabilityRepository.findOverlappingAvailability(anyLong(), any(), any()))
                    .thenReturn(Collections.emptyList());

            Roster savedRoster = new Roster();
            savedRoster.setRosterId(2L);
            savedRoster.setCrewMember(captain);
            savedRoster.setFlight(flightB);
            savedRoster.setStatus(RosterStatus.CONFIRMED);
            when(rosterRepository.save(any(Roster.class))).thenReturn(savedRoster);

            // Act
            RosterAssignmentResult result = rosterService.assignCrewToFlight(1L, 2L);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            verify(rosterRepository).save(any(Roster.class));
        }
    }

    @Nested
    @DisplayName("Overlap Detection")
    class OverlapDetection {

        @Test
        @DisplayName("Should reject assignment when flights overlap")
        void shouldRejectAssignment_WhenFlightsOverlap() {
            // Create an overlapping roster
            Roster overlappingRoster = new Roster();
            overlappingRoster.setRosterId(10L);
            overlappingRoster.setFlight(flightB);

            when(crewMemberRepository.findById(1L)).thenReturn(Optional.of(captain));
            when(flightRepository.findById(1L)).thenReturn(Optional.of(flightA));
            when(rosterRepository.findConflictingRosters(anyLong(), any(), any()))
                    .thenReturn(List.of(overlappingRoster));
            when(rosterRepository.findConfirmedRostersByCrewIdOrderByDeparture(1L))
                    .thenReturn(Collections.emptyList());
            when(rosterRepository.findConfirmedRostersInMonth(anyLong(), any(), any()))
                    .thenReturn(Collections.emptyList());

            // Act & Assert
            assertThatThrownBy(() -> rosterService.assignCrewToFlight(1L, 1L))
                    .isInstanceOf(SchedulingConflictException.class)
                    .satisfies(ex -> {
                        SchedulingConflictException sce = (SchedulingConflictException) ex;
                        assertThat(sce.getConflicts()).isNotEmpty();
                        assertThat(sce.getConflicts().get(0).getConflictType())
                                .isEqualTo(ConflictReport.ConflictType.OVERLAP);
                    });

            // Verify roster was NOT saved
            verify(rosterRepository, never()).save(any(Roster.class));
        }
    }

    @Nested
    @DisplayName("Fatigue Management")
    class FatigueManagement {

        @Test
        @DisplayName("Should reject assignment when rest period is insufficient")
        void shouldRejectAssignment_WhenRestPeriodInsufficient() {
            // Flight A arrives at 16:00 on Jul 1
            // Flight C departs at 02:00 on Jul 2 — only 10 hours rest (< 12h minimum)
            Flight flightC = new Flight();
            flightC.setFlightId(3L);
            flightC.setFlightNumber("SK303");
            flightC.setOrigin("LAX");
            flightC.setDestination("ORD");
            flightC.setDepartureTime(LocalDateTime.of(2026, 7, 2, 2, 0));
            flightC.setArrivalTime(LocalDateTime.of(2026, 7, 2, 8, 0));

            Roster existingRoster = new Roster();
            existingRoster.setRosterId(10L);
            existingRoster.setCrewMember(captain);
            existingRoster.setFlight(flightA); // arrives at 16:00
            existingRoster.setStatus(RosterStatus.CONFIRMED);

            when(crewMemberRepository.findById(1L)).thenReturn(Optional.of(captain));
            when(flightRepository.findById(3L)).thenReturn(Optional.of(flightC));
            when(rosterRepository.findConflictingRosters(anyLong(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(rosterRepository.findConfirmedRostersByCrewIdOrderByDeparture(1L))
                    .thenReturn(List.of(existingRoster));
            when(rosterRepository.findConfirmedRostersInMonth(anyLong(), any(), any()))
                    .thenReturn(List.of(existingRoster));

            // Act & Assert
            assertThatThrownBy(() -> rosterService.assignCrewToFlight(1L, 3L))
                    .isInstanceOf(SchedulingConflictException.class)
                    .satisfies(ex -> {
                        SchedulingConflictException sce = (SchedulingConflictException) ex;
                        assertThat(sce.getConflicts()).isNotEmpty();
                        assertThat(sce.getConflicts()).anyMatch(c ->
                                c.getConflictType() == ConflictReport.ConflictType.FATIGUE);
                    });

            verify(rosterRepository, never()).save(any(Roster.class));
        }
    }

    @Nested
    @DisplayName("Monthly Hours Cap")
    class MonthlyHoursCap {

        @Test
        @DisplayName("Should reject assignment when monthly hours would be exceeded")
        void shouldRejectAssignment_WhenMonthlyHoursExceeded() {
            // Captain has 100 max monthly hours
            // Existing flights total 96 hours this month
            captain.setMaxMonthlyHours(100);

            // Create a long flight (say 96h worth of existing rosters - simulated)
            Flight longFlight = new Flight();
            longFlight.setFlightId(10L);
            longFlight.setFlightNumber("SK900");
            longFlight.setDepartureTime(LocalDateTime.of(2026, 7, 1, 0, 0));
            longFlight.setArrivalTime(LocalDateTime.of(2026, 7, 5, 0, 0)); // 96 hours

            Roster existingRoster = new Roster();
            existingRoster.setRosterId(10L);
            existingRoster.setCrewMember(captain);
            existingRoster.setFlight(longFlight);
            existingRoster.setStatus(RosterStatus.CONFIRMED);

            // New flight is 6 hours — total would be 102 > 100
            when(crewMemberRepository.findById(1L)).thenReturn(Optional.of(captain));
            when(flightRepository.findById(1L)).thenReturn(Optional.of(flightA)); // 6 hour flight
            when(rosterRepository.findConflictingRosters(anyLong(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(rosterRepository.findConfirmedRostersByCrewIdOrderByDeparture(1L))
                    .thenReturn(List.of(existingRoster));
            when(rosterRepository.findConfirmedRostersInMonth(anyLong(), any(), any()))
                    .thenReturn(List.of(existingRoster));

            // Act & Assert
            assertThatThrownBy(() -> rosterService.assignCrewToFlight(1L, 1L))
                    .isInstanceOf(SchedulingConflictException.class)
                    .satisfies(ex -> {
                        SchedulingConflictException sce = (SchedulingConflictException) ex;
                        assertThat(sce.getConflicts()).anyMatch(c ->
                                c.getConflictType() == ConflictReport.ConflictType.HOURS_EXCEEDED);
                    });

            verify(rosterRepository, never()).save(any(Roster.class));
        }
    }

    @Nested
    @DisplayName("Crew Complement")
    class CrewComplement {

        @Test
        @DisplayName("Should reject assignment when pilot slots are full")
        void shouldRejectAssignment_WhenPilotSlotsFull() {
            // Flight requires 2 pilots and already has 2 assigned
            when(crewMemberRepository.findById(1L)).thenReturn(Optional.of(captain));
            when(flightRepository.findById(1L)).thenReturn(Optional.of(flightA));
            when(rosterRepository.findConflictingRosters(anyLong(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(rosterRepository.findConfirmedRostersByCrewIdOrderByDeparture(1L))
                    .thenReturn(Collections.emptyList());
            when(rosterRepository.findConfirmedRostersInMonth(anyLong(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(rosterRepository.countConfirmedByFlightAndCrewType(1L, CockpitCrew.class))
                    .thenReturn(2L); // Already full (required = 2)

            // Act & Assert
            assertThatThrownBy(() -> rosterService.assignCrewToFlight(1L, 1L))
                    .isInstanceOf(SchedulingConflictException.class)
                    .satisfies(ex -> {
                        SchedulingConflictException sce = (SchedulingConflictException) ex;
                        assertThat(sce.getConflicts()).anyMatch(c ->
                                c.getConflictType() == ConflictReport.ConflictType.CREW_COMPLEMENT_EXCEEDED);
                    });

            verify(rosterRepository, never()).save(any(Roster.class));
        }

        @Test
        @DisplayName("Should reject assignment when cabin crew slots are full")
        void shouldRejectAssignment_WhenCabinSlotsFull() {
            // Create a cabin crew member
            CabinCrew attendant = new CabinCrew();
            attendant.setCrewId(2L);
            attendant.setName("FA Jones");
            attendant.setRole(CrewRole.FLIGHT_ATTENDANT);
            attendant.setBaseAirport("JFK");
            attendant.setMaxMonthlyHours(120);

            when(crewMemberRepository.findById(2L)).thenReturn(Optional.of(attendant));
            when(flightRepository.findById(1L)).thenReturn(Optional.of(flightA));
            when(rosterRepository.findConflictingRosters(anyLong(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(rosterRepository.findConfirmedRostersByCrewIdOrderByDeparture(2L))
                    .thenReturn(Collections.emptyList());
            when(rosterRepository.findConfirmedRostersInMonth(anyLong(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(rosterRepository.countConfirmedByFlightAndCrewType(1L, CabinCrew.class))
                    .thenReturn(4L); // Already full (required = 4)

            // Act & Assert
            assertThatThrownBy(() -> rosterService.assignCrewToFlight(2L, 1L))
                    .isInstanceOf(SchedulingConflictException.class)
                    .satisfies(ex -> {
                        SchedulingConflictException sce = (SchedulingConflictException) ex;
                        assertThat(sce.getConflicts()).anyMatch(c ->
                                c.getConflictType() == ConflictReport.ConflictType.CREW_COMPLEMENT_EXCEEDED);
                    });

            verify(rosterRepository, never()).save(any(Roster.class));
        }
    }

    @Nested
    @DisplayName("Conflict Detection Scan")
    class ConflictDetectionScan {

        @Test
        @DisplayName("Should detect multiple conflicts for a crew member")
        void shouldDetectMultipleConflictsForCrew() {
            // Two flights with only 8h rest between them (< 12h minimum)
            Flight earlyFlight = new Flight();
            earlyFlight.setFlightId(1L);
            earlyFlight.setFlightNumber("SK101");
            earlyFlight.setDepartureTime(LocalDateTime.of(2026, 7, 1, 10, 0));
            earlyFlight.setArrivalTime(LocalDateTime.of(2026, 7, 1, 16, 0));

            Flight lateFlight = new Flight();
            lateFlight.setFlightId(2L);
            lateFlight.setFlightNumber("SK102");
            lateFlight.setDepartureTime(LocalDateTime.of(2026, 7, 2, 0, 0)); // 8h after earlyFlight
            lateFlight.setArrivalTime(LocalDateTime.of(2026, 7, 2, 4, 0));

            Roster roster1 = new Roster();
            roster1.setCrewMember(captain);
            roster1.setFlight(earlyFlight);
            roster1.setStatus(RosterStatus.CONFIRMED);

            Roster roster2 = new Roster();
            roster2.setCrewMember(captain);
            roster2.setFlight(lateFlight);
            roster2.setStatus(RosterStatus.CONFIRMED);

            when(crewMemberRepository.findAll()).thenReturn(List.of(captain));
            when(rosterRepository.findConfirmedRostersByCrewIdOrderByDeparture(1L))
                    .thenReturn(List.of(roster1, roster2));

            // Act
            List<ConflictReport> conflicts = rosterService.detectAllConflicts();

            // Assert
            assertThat(conflicts).isNotEmpty();
            assertThat(conflicts).anyMatch(c ->
                    c.getConflictType() == ConflictReport.ConflictType.FATIGUE);
        }

        @Test
        @DisplayName("Should return empty list when no conflicts exist")
        void shouldReturnEmptyWhenNoConflicts() {
            when(crewMemberRepository.findAll()).thenReturn(List.of(captain));
            when(rosterRepository.findConfirmedRostersByCrewIdOrderByDeparture(1L))
                    .thenReturn(Collections.emptyList());

            List<ConflictReport> conflicts = rosterService.detectAllConflicts();

            assertThat(conflicts).isEmpty();
        }
    }

    @Nested
    @DisplayName("Rule 5: Availability Check")
    class AvailabilityCheck {

        @Test
        @DisplayName("Should reject assignment when crew is on approved leave")
        void shouldReject_WhenCrewIsOnLeave() {
            when(crewMemberRepository.findById(1L)).thenReturn(Optional.of(captain));
            when(flightRepository.findById(1L)).thenReturn(Optional.of(flightA));
            when(rosterRepository.findConflictingRosters(anyLong(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(rosterRepository.findConfirmedRostersByCrewIdOrderByDeparture(1L))
                    .thenReturn(Collections.emptyList());
            when(rosterRepository.findConfirmedRostersInMonth(anyLong(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(rosterRepository.countConfirmedByFlightAndCrewType(anyLong(), any()))
                    .thenReturn(0L);

            // Crew has approved leave overlapping with flight
            CrewAvailability leave = CrewAvailability.builder()
                    .id(1L)
                    .crewMember(captain)
                    .availabilityType(AvailabilityType.LEAVE)
                    .startDate(flightA.getDepartureTime().minusDays(1))
                    .endDate(flightA.getArrivalTime().plusDays(1))
                    .approved(true)
                    .build();

            when(availabilityRepository.findOverlappingAvailability(anyLong(), any(), any()))
                    .thenReturn(List.of(leave));

            assertThatThrownBy(() -> rosterService.assignCrewToFlight(1L, 1L))
                    .isInstanceOf(SchedulingConflictException.class)
                    .satisfies(ex -> {
                        List<ConflictReport> conflicts =
                                ((SchedulingConflictException) ex).getConflicts();
                        assertThat(conflicts).anyMatch(c ->
                                c.getConflictType() == ConflictReport.ConflictType.UNAVAILABLE);
                    });
        }
    }

    @Nested
    @DisplayName("Rule 6: Flight Duty Period (FDP)")
    class FlightDutyPeriod {

        @Test
        @DisplayName("Should reject assignment when FDP exceeds 13 hours")
        void shouldReject_WhenFdpExceeds13Hours() {
            // Create a very long flight: 12.5h flight + 1h report + 0.5h debrief = 14h total
            Flight longFlight = new Flight();
            longFlight.setFlightId(3L);
            longFlight.setFlightNumber("SK-LONG");
            longFlight.setOrigin("JFK");
            longFlight.setDestination("SIN");
            longFlight.setDepartureTime(LocalDateTime.of(2026, 12, 1, 10, 0));
            longFlight.setArrivalTime(LocalDateTime.of(2026, 12, 1, 22, 30)); // 12.5 hours
            longFlight.setReportTimeMinutes(60);
            longFlight.setDebriefTimeMinutes(30);
            longFlight.setRequiredPilots(2);
            longFlight.setRequiredCabinCrew(4);

            when(crewMemberRepository.findById(1L)).thenReturn(Optional.of(captain));
            when(flightRepository.findById(3L)).thenReturn(Optional.of(longFlight));
            when(rosterRepository.findConflictingRosters(anyLong(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(rosterRepository.findConfirmedRostersByCrewIdOrderByDeparture(1L))
                    .thenReturn(Collections.emptyList());
            when(rosterRepository.findConfirmedRostersInMonth(anyLong(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(rosterRepository.countConfirmedByFlightAndCrewType(anyLong(), any()))
                    .thenReturn(0L);
            when(availabilityRepository.findOverlappingAvailability(anyLong(), any(), any()))
                    .thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> rosterService.assignCrewToFlight(1L, 3L))
                    .isInstanceOf(SchedulingConflictException.class)
                    .satisfies(ex -> {
                        List<ConflictReport> conflicts =
                                ((SchedulingConflictException) ex).getConflicts();
                        assertThat(conflicts).anyMatch(c ->
                                c.getConflictType() == ConflictReport.ConflictType.FDP_EXCEEDED);
                    });
        }
    }
}

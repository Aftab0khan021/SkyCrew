package com.skycrew.service;

import com.skycrew.dto.ConflictReport;
import com.skycrew.dto.PagedResponse;
import com.skycrew.dto.RosterAssignmentResult;
import com.skycrew.exception.MonthlyHoursExceededException;
import com.skycrew.exception.ResourceNotFoundException;
import com.skycrew.exception.SchedulingConflictException;
import com.skycrew.model.*;
import com.skycrew.repository.AvailabilityRepository;
import com.skycrew.repository.CrewMemberRepository;
import com.skycrew.repository.FlightRepository;
import com.skycrew.repository.RosterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Smart Rostering Engine — the core business logic of SkyCrew.
 *
 * Enforces the following rules when assigning crew to flights:
 *  1. OVERLAP DETECTION — No crew member on two overlapping flights
 *  2. FATIGUE MANAGEMENT — Minimum rest period between flights
 *  3. MONTHLY HOURS CAP — Cannot exceed max flying hours per month
 *  4. CREW COMPLEMENT — Flight cannot exceed required crew count
 *  5. AVAILABILITY CHECK — Crew must not be on leave/training/medical
 *  6. FDP LIMIT — Maximum 13h continuous duty period
 *  7. CUMULATIVE DUTY — 60h in 7 days, 190h in 28 days
 */
@Service
@RequiredArgsConstructor
public class RosterService {

    private final RosterRepository rosterRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final FlightRepository flightRepository;
    private final AvailabilityRepository availabilityRepository;
    private final NotificationService notificationService;

    @Value("${skycrew.rules.min-rest-hours}")
    private int minRestHours;

    private static final int MAX_FDP_HOURS = 13; // Maximum Flight Duty Period
    private static final int MAX_7DAY_DUTY_HOURS = 60;
    private static final int MAX_28DAY_DUTY_HOURS = 190;

    /**
     * Assigns a crew member to a flight after running all business rule checks.
     */
    @Transactional
    public RosterAssignmentResult assignCrewToFlight(Long crewId, Long flightId) {
        // 1. Validate entities exist
        CrewMember crew = crewMemberRepository.findById(crewId)
                .orElseThrow(() -> new ResourceNotFoundException("CrewMember", "crewId", crewId));
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight", "flightId", flightId));

        List<ConflictReport> conflicts = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 2. Check for OVERLAP conflicts
        List<Roster> overlapping = rosterRepository.findConflictingRosters(
                crewId, flight.getDepartureTime(), flight.getArrivalTime());

        for (Roster overlap : overlapping) {
            conflicts.add(ConflictReport.builder()
                    .crewId(crewId)
                    .crewName(crew.getName())
                    .conflictType(ConflictReport.ConflictType.OVERLAP)
                    .flightAId(flightId)
                    .flightANumber(flight.getFlightNumber())
                    .flightBId(overlap.getFlight().getFlightId())
                    .flightBNumber(overlap.getFlight().getFlightNumber())
                    .message(String.format("Flight %s overlaps with already assigned flight %s",
                            flight.getFlightNumber(), overlap.getFlight().getFlightNumber()))
                    .build());
        }

        // 3. Check FATIGUE RULE (minimum rest period)
        List<Roster> allRosters = rosterRepository
                .findConfirmedRostersByCrewIdOrderByDeparture(crewId);

        for (Roster existing : allRosters) {
            Flight existingFlight = existing.getFlight();

            // Check rest after existing flight before new flight
            Duration restBefore = Duration.between(
                    existingFlight.getArrivalTime(), flight.getDepartureTime());
            if (restBefore.toHours() >= 0 && restBefore.toHours() < minRestHours) {
                conflicts.add(ConflictReport.builder()
                        .crewId(crewId)
                        .crewName(crew.getName())
                        .conflictType(ConflictReport.ConflictType.FATIGUE)
                        .flightAId(existingFlight.getFlightId())
                        .flightANumber(existingFlight.getFlightNumber())
                        .flightBId(flightId)
                        .flightBNumber(flight.getFlightNumber())
                        .message(String.format(
                                "Only %.1f hours rest between flight %s (arrives %s) and flight %s (departs %s). " +
                                "Minimum required: %d hours",
                                (double) restBefore.toMinutes() / 60,
                                existingFlight.getFlightNumber(), existingFlight.getArrivalTime(),
                                flight.getFlightNumber(), flight.getDepartureTime(),
                                minRestHours))
                        .build());
            }

            // Check rest after new flight before existing flight
            Duration restAfter = Duration.between(
                    flight.getArrivalTime(), existingFlight.getDepartureTime());
            if (restAfter.toHours() >= 0 && restAfter.toHours() < minRestHours) {
                conflicts.add(ConflictReport.builder()
                        .crewId(crewId)
                        .crewName(crew.getName())
                        .conflictType(ConflictReport.ConflictType.FATIGUE)
                        .flightAId(flightId)
                        .flightANumber(flight.getFlightNumber())
                        .flightBId(existingFlight.getFlightId())
                        .flightBNumber(existingFlight.getFlightNumber())
                        .message(String.format(
                                "Only %.1f hours rest between flight %s (arrives %s) and flight %s (departs %s). " +
                                "Minimum required: %d hours",
                                (double) restAfter.toMinutes() / 60,
                                flight.getFlightNumber(), flight.getArrivalTime(),
                                existingFlight.getFlightNumber(), existingFlight.getDepartureTime(),
                                minRestHours))
                        .build());
            }
        }

        // 4. Check MONTHLY HOURS CAP
        LocalDateTime monthStart = flight.getDepartureTime().withDayOfMonth(1)
                .truncatedTo(ChronoUnit.DAYS);
        LocalDateTime monthEnd = monthStart.plusMonths(1);

        List<Roster> monthlyRosters = rosterRepository
                .findConfirmedRostersInMonth(crewId, monthStart, monthEnd);

        double monthlyHours = monthlyRosters.stream()
                .mapToDouble(r -> Duration.between(
                        r.getFlight().getDepartureTime(),
                        r.getFlight().getArrivalTime()).toMinutes() / 60.0)
                .sum();

        double newFlightHours = Duration.between(
                flight.getDepartureTime(), flight.getArrivalTime()).toMinutes() / 60.0;

        double totalHoursAfterAssignment = monthlyHours + newFlightHours;

        if (totalHoursAfterAssignment > crew.getMaxMonthlyHours()) {
            conflicts.add(ConflictReport.builder()
                    .crewId(crewId)
                    .crewName(crew.getName())
                    .conflictType(ConflictReport.ConflictType.HOURS_EXCEEDED)
                    .flightAId(flightId)
                    .flightANumber(flight.getFlightNumber())
                    .message(String.format(
                            "Assigning flight %s (%.1f hrs) would bring monthly total to %.1f hrs, " +
                            "exceeding the %d hr limit for crew member %s",
                            flight.getFlightNumber(), newFlightHours,
                            totalHoursAfterAssignment, crew.getMaxMonthlyHours(), crew.getName()))
                    .build());
        } else if (totalHoursAfterAssignment > crew.getMaxMonthlyHours() * 0.9) {
            // Warning at 90% threshold
            warnings.add(String.format(
                    "Crew member %s is at %.0f%% of monthly flying hours (%.1f / %d hrs)",
                    crew.getName(), (totalHoursAfterAssignment / crew.getMaxMonthlyHours()) * 100,
                    totalHoursAfterAssignment, crew.getMaxMonthlyHours()));
        }

        // 5. Check CREW COMPLEMENT (flight cannot exceed required crew count)
        if (crew instanceof CockpitCrew) {
            long currentPilots = rosterRepository.countConfirmedByFlightAndCrewType(
                    flightId, CockpitCrew.class);
            if (currentPilots >= flight.getRequiredPilots()) {
                conflicts.add(ConflictReport.builder()
                        .crewId(crewId)
                        .crewName(crew.getName())
                        .conflictType(ConflictReport.ConflictType.CREW_COMPLEMENT_EXCEEDED)
                        .flightAId(flightId)
                        .flightANumber(flight.getFlightNumber())
                        .message(String.format(
                                "Flight %s already has %d/%d pilots assigned. Cannot add crew member %s",
                                flight.getFlightNumber(), currentPilots,
                                flight.getRequiredPilots(), crew.getName()))
                        .build());
            }
        } else if (crew instanceof CabinCrew) {
            long currentCabin = rosterRepository.countConfirmedByFlightAndCrewType(
                    flightId, CabinCrew.class);
            if (currentCabin >= flight.getRequiredCabinCrew()) {
                conflicts.add(ConflictReport.builder()
                        .crewId(crewId)
                        .crewName(crew.getName())
                        .conflictType(ConflictReport.ConflictType.CREW_COMPLEMENT_EXCEEDED)
                        .flightAId(flightId)
                        .flightANumber(flight.getFlightNumber())
                        .message(String.format(
                                "Flight %s already has %d/%d cabin crew assigned. Cannot add crew member %s",
                                flight.getFlightNumber(), currentCabin,
                                flight.getRequiredCabinCrew(), crew.getName()))
                        .build());
            }
        }

        // 6. Check AVAILABILITY (leave, training, medical blocks)
        List<CrewAvailability> unavailableBlocks = availabilityRepository
                .findOverlappingAvailability(crewId, flight.getDepartureTime(), flight.getArrivalTime());

        for (CrewAvailability block : unavailableBlocks) {
            conflicts.add(ConflictReport.builder()
                    .crewId(crewId)
                    .crewName(crew.getName())
                    .conflictType(ConflictReport.ConflictType.UNAVAILABLE)
                    .flightAId(flightId)
                    .flightANumber(flight.getFlightNumber())
                    .message(String.format(
                            "Crew member %s is unavailable (%s) from %s to %s",
                            crew.getName(), block.getAvailabilityType(),
                            block.getStartDate(), block.getEndDate()))
                    .build());
        }

        // 7. Check FLIGHT DUTY PERIOD (FDP) — max 13h continuous duty
        double dutyHours = (flight.getReportTimeMinutes() / 60.0)
                + Duration.between(flight.getDepartureTime(), flight.getArrivalTime()).toMinutes() / 60.0
                + (flight.getDebriefTimeMinutes() / 60.0);

        if (dutyHours > MAX_FDP_HOURS) {
            conflicts.add(ConflictReport.builder()
                    .crewId(crewId)
                    .crewName(crew.getName())
                    .conflictType(ConflictReport.ConflictType.FDP_EXCEEDED)
                    .flightAId(flightId)
                    .flightANumber(flight.getFlightNumber())
                    .message(String.format(
                            "Flight %s duty period is %.1f hours (report: %dmin + flight + debrief: %dmin). " +
                            "Maximum FDP: %d hours",
                            flight.getFlightNumber(), dutyHours,
                            flight.getReportTimeMinutes(), flight.getDebriefTimeMinutes(),
                            MAX_FDP_HOURS))
                    .build());
        }

        // 8. Check CUMULATIVE DUTY (60h in 7 days, 190h in 28 days)
        LocalDateTime sevenDaysAgo = flight.getDepartureTime().minusDays(7);
        LocalDateTime twentyEightDaysAgo = flight.getDepartureTime().minusDays(28);
        LocalDateTime sevenDaysAhead = flight.getDepartureTime().plusDays(7);
        LocalDateTime twentyEightDaysAhead = flight.getDepartureTime().plusDays(28);

        List<Roster> last7DaysRosters = rosterRepository
                .findConfirmedRostersInMonth(crewId, sevenDaysAgo, sevenDaysAhead);
        double last7DaysDuty = last7DaysRosters.stream()
                .mapToDouble(r -> Duration.between(
                        r.getFlight().getDepartureTime(),
                        r.getFlight().getArrivalTime()).toMinutes() / 60.0)
                .sum() + newFlightHours;

        if (last7DaysDuty > MAX_7DAY_DUTY_HOURS) {
            conflicts.add(ConflictReport.builder()
                    .crewId(crewId)
                    .crewName(crew.getName())
                    .conflictType(ConflictReport.ConflictType.CUMULATIVE_DUTY_EXCEEDED)
                    .flightAId(flightId)
                    .flightANumber(flight.getFlightNumber())
                    .message(String.format(
                            "Crew %s would reach %.1f duty hours in 7-day window. Maximum: %d hours",
                            crew.getName(), last7DaysDuty, MAX_7DAY_DUTY_HOURS))
                    .build());
        }

        List<Roster> last28DaysRosters = rosterRepository
                .findConfirmedRostersInMonth(crewId, twentyEightDaysAgo, twentyEightDaysAhead);
        double last28DaysDuty = last28DaysRosters.stream()
                .mapToDouble(r -> Duration.between(
                        r.getFlight().getDepartureTime(),
                        r.getFlight().getArrivalTime()).toMinutes() / 60.0)
                .sum() + newFlightHours;

        if (last28DaysDuty > MAX_28DAY_DUTY_HOURS) {
            conflicts.add(ConflictReport.builder()
                    .crewId(crewId)
                    .crewName(crew.getName())
                    .conflictType(ConflictReport.ConflictType.CUMULATIVE_DUTY_EXCEEDED)
                    .flightAId(flightId)
                    .flightANumber(flight.getFlightNumber())
                    .message(String.format(
                            "Crew %s would reach %.1f duty hours in 28-day window. Maximum: %d hours",
                            crew.getName(), last28DaysDuty, MAX_28DAY_DUTY_HOURS))
                    .build());
        }

        // If any conflicts found, reject the assignment
        if (!conflicts.isEmpty()) {
            throw new SchedulingConflictException(
                    "Cannot assign crew member to flight — " + conflicts.size() + " conflict(s) detected",
                    conflicts);
        }

        // All clear — create the roster entry
        Roster roster = new Roster();
        roster.setCrewMember(crew);
        roster.setFlight(flight);
        roster.setStatus(RosterStatus.CONFIRMED);

        Roster saved = rosterRepository.save(roster);

        // Trigger notification
        try {
            notificationService.notifyScheduleChange(saved, "ASSIGNED");
        } catch (Exception e) {
            // Don't fail the assignment if notification fails
        }

        return RosterAssignmentResult.builder()
                .rosterId(saved.getRosterId())
                .crewId(crewId)
                .flightId(flightId)
                .status(saved.getStatus())
                .success(true)
                .message("Crew member " + crew.getName() + " assigned to flight " +
                        flight.getFlightNumber() + " successfully")
                .warnings(warnings)
                .createdAt(saved.getCreatedAt())
                .createdBy(saved.getCreatedBy())
                .build();
    }

    /**
     * Scans all confirmed rosters and returns any detected scheduling violations.
     */
    @Transactional(readOnly = true)
    public List<ConflictReport> detectAllConflicts() {
        List<ConflictReport> allConflicts = new ArrayList<>();
        List<CrewMember> allCrew = crewMemberRepository.findAll();

        for (CrewMember crew : allCrew) {
            List<Roster> rosters = rosterRepository
                    .findConfirmedRostersByCrewIdOrderByDeparture(crew.getCrewId());

            if (rosters.size() < 2) continue;

            for (int i = 0; i < rosters.size() - 1; i++) {
                Flight current = rosters.get(i).getFlight();
                Flight next = rosters.get(i + 1).getFlight();

                // Check overlap
                if (current.getArrivalTime().isAfter(next.getDepartureTime())) {
                    allConflicts.add(ConflictReport.builder()
                            .crewId(crew.getCrewId())
                            .crewName(crew.getName())
                            .conflictType(ConflictReport.ConflictType.OVERLAP)
                            .flightAId(current.getFlightId())
                            .flightANumber(current.getFlightNumber())
                            .flightBId(next.getFlightId())
                            .flightBNumber(next.getFlightNumber())
                            .message(String.format("Flight %s overlaps with flight %s for crew %s",
                                    current.getFlightNumber(), next.getFlightNumber(), crew.getName()))
                            .build());
                }

                // Check fatigue
                Duration rest = Duration.between(current.getArrivalTime(), next.getDepartureTime());
                if (rest.toHours() >= 0 && rest.toHours() < minRestHours) {
                    allConflicts.add(ConflictReport.builder()
                            .crewId(crew.getCrewId())
                            .crewName(crew.getName())
                            .conflictType(ConflictReport.ConflictType.FATIGUE)
                            .flightAId(current.getFlightId())
                            .flightANumber(current.getFlightNumber())
                            .flightBId(next.getFlightId())
                            .flightBNumber(next.getFlightNumber())
                            .message(String.format(
                                    "Insufficient rest (%.1f hrs) between flights %s and %s for crew %s. " +
                                    "Minimum: %d hrs",
                                    (double) rest.toMinutes() / 60,
                                    current.getFlightNumber(), next.getFlightNumber(),
                                    crew.getName(), minRestHours))
                            .build());
                }
            }
        }

        return allConflicts;
    }

    @Transactional(readOnly = true)
    public PagedResponse<RosterAssignmentResult> getAllRosters(Pageable pageable) {
        Page<Roster> page = rosterRepository.findAll(pageable);

        var results = page.getContent().stream()
                .map(r -> RosterAssignmentResult.builder()
                        .rosterId(r.getRosterId())
                        .crewId(r.getCrewMember().getCrewId())
                        .flightId(r.getFlight().getFlightId())
                        .status(r.getStatus())
                        .success(true)
                        .createdAt(r.getCreatedAt())
                        .createdBy(r.getCreatedBy())
                        .build())
                .toList();

        return PagedResponse.<RosterAssignmentResult>builder()
                .content(results)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional
    public void removeRoster(Long rosterId) {
        Roster roster = rosterRepository.findById(rosterId)
                .orElseThrow(() -> new ResourceNotFoundException("Roster", "rosterId", rosterId));

        rosterRepository.delete(roster);

        // Trigger notification
        try {
            notificationService.notifyScheduleChange(roster, "REMOVED");
        } catch (Exception e) {
            // Don't fail the deletion if notification fails
        }
    }
}

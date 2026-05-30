package com.skycrew.service;

import com.skycrew.dto.*;
import com.skycrew.exception.ResourceNotFoundException;
import com.skycrew.model.*;
import com.skycrew.repository.CrewMemberRepository;
import com.skycrew.repository.RosterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing crew member CRUD operations and schedule retrieval.
 */
@Service
@RequiredArgsConstructor
public class CrewService {

    private final CrewMemberRepository crewMemberRepository;
    private final RosterRepository rosterRepository;

    @Transactional
    public CrewMemberResponse createCrewMember(CrewMemberRequest request) {
        CrewMember crewMember;

        if ("COCKPIT".equalsIgnoreCase(request.getCrewType())) {
            CockpitCrew cockpit = new CockpitCrew();
            cockpit.setLicenseNumber(request.getLicenseNumber());
            cockpit.setTypeRatings(request.getTypeRatings());
            crewMember = cockpit;
        } else if ("CABIN".equalsIgnoreCase(request.getCrewType())) {
            CabinCrew cabin = new CabinCrew();
            cabin.setLanguagesSpoken(request.getLanguagesSpoken());
            cabin.setSafetyTrainingExpiry(request.getSafetyTrainingExpiry());
            crewMember = cabin;
        } else {
            throw new IllegalArgumentException(
                    "Invalid crew type: " + request.getCrewType() + ". Must be COCKPIT or CABIN.");
        }

        crewMember.setName(request.getName());
        crewMember.setRole(request.getRole());
        crewMember.setBaseAirport(request.getBaseAirport().toUpperCase());
        crewMember.setMaxMonthlyHours(request.getMaxMonthlyHours());

        CrewMember saved = crewMemberRepository.save(crewMember);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public CrewMemberResponse getCrewMemberById(Long crewId) {
        CrewMember crew = crewMemberRepository.findById(crewId)
                .orElseThrow(() -> new ResourceNotFoundException("CrewMember", "crewId", crewId));
        return mapToResponse(crew);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CrewMemberResponse> getAllCrewMembers(Pageable pageable) {
        Page<CrewMember> page = crewMemberRepository.findAll(pageable);
        return buildPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ScheduleResponse> getCrewSchedule(Long crewId, Pageable pageable) {
        // Verify crew member exists
        if (!crewMemberRepository.existsById(crewId)) {
            throw new ResourceNotFoundException("CrewMember", "crewId", crewId);
        }

        Page<Roster> rosterPage = rosterRepository.findByCrewMember_CrewId(crewId, pageable);

        var scheduleItems = rosterPage.getContent().stream()
                .map(roster -> ScheduleResponse.builder()
                        .rosterId(roster.getRosterId())
                        .status(roster.getStatus())
                        .flightId(roster.getFlight().getFlightId())
                        .flightNumber(roster.getFlight().getFlightNumber())
                        .origin(roster.getFlight().getOrigin())
                        .destination(roster.getFlight().getDestination())
                        .departureTime(roster.getFlight().getDepartureTime())
                        .arrivalTime(roster.getFlight().getArrivalTime())
                        .assignedAt(roster.getCreatedAt())
                        .assignedBy(roster.getCreatedBy())
                        .build())
                .toList();

        return PagedResponse.<ScheduleResponse>builder()
                .content(scheduleItems)
                .page(rosterPage.getNumber())
                .size(rosterPage.getSize())
                .totalElements(rosterPage.getTotalElements())
                .totalPages(rosterPage.getTotalPages())
                .last(rosterPage.isLast())
                .build();
    }

    @Transactional
    public void deleteCrewMember(Long crewId) {
        if (!crewMemberRepository.existsById(crewId)) {
            throw new ResourceNotFoundException("CrewMember", "crewId", crewId);
        }
        crewMemberRepository.deleteById(crewId);
    }

    // --- Private helpers ---

    private CrewMemberResponse mapToResponse(CrewMember crew) {
        CrewMemberResponse.CrewMemberResponseBuilder builder = CrewMemberResponse.builder()
                .crewId(crew.getCrewId())
                .name(crew.getName())
                .role(crew.getRole())
                .baseAirport(crew.getBaseAirport())
                .maxMonthlyHours(crew.getMaxMonthlyHours())
                .createdAt(crew.getCreatedAt())
                .updatedAt(crew.getUpdatedAt())
                .createdBy(crew.getCreatedBy());

        if (crew instanceof CockpitCrew cockpit) {
            builder.crewType("COCKPIT")
                    .licenseNumber(cockpit.getLicenseNumber())
                    .typeRatings(cockpit.getTypeRatings());
        } else if (crew instanceof CabinCrew cabin) {
            builder.crewType("CABIN")
                    .languagesSpoken(cabin.getLanguagesSpoken())
                    .safetyTrainingExpiry(cabin.getSafetyTrainingExpiry());
        }

        return builder.build();
    }

    private PagedResponse<CrewMemberResponse> buildPagedResponse(Page<CrewMember> page) {
        return PagedResponse.<CrewMemberResponse>builder()
                .content(page.getContent().stream().map(this::mapToResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}

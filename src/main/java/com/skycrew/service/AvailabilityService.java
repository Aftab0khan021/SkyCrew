package com.skycrew.service;

import com.skycrew.dto.AvailabilityRequest;
import com.skycrew.dto.AvailabilityResponse;
import com.skycrew.exception.ResourceNotFoundException;
import com.skycrew.model.CrewAvailability;
import com.skycrew.model.CrewMember;
import com.skycrew.repository.AvailabilityRepository;
import com.skycrew.repository.CrewMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing crew availability calendar.
 * Tracks leave, training, medical, and vacation blocks.
 */
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;
    private final CrewMemberRepository crewMemberRepository;

    /**
     * Adds an availability block for a crew member.
     */
    @Transactional
    public AvailabilityResponse addAvailability(Long crewId, AvailabilityRequest request) {
        CrewMember crew = crewMemberRepository.findById(crewId)
                .orElseThrow(() -> new ResourceNotFoundException("CrewMember", "crewId", crewId));

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        CrewAvailability availability = CrewAvailability.builder()
                .crewMember(crew)
                .availabilityType(request.getAvailabilityType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .notes(request.getNotes())
                .approved(request.isApproved())
                .build();

        CrewAvailability saved = availabilityRepository.save(availability);
        return toResponse(saved);
    }

    /**
     * Gets all availability blocks for a crew member.
     */
    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getAvailability(Long crewId) {
        if (!crewMemberRepository.existsById(crewId)) {
            throw new ResourceNotFoundException("CrewMember", "crewId", crewId);
        }
        return availabilityRepository.findByCrewMemberCrewIdOrderByStartDateAsc(crewId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Gets availability blocks for a crew member within a date range.
     */
    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getAvailabilityInRange(
            Long crewId, LocalDateTime start, LocalDateTime end) {
        return availabilityRepository.findByCrewAndDateRange(crewId, start, end)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns unavailable periods for a crew member in a given month.
     * Convenience wrapper around getAvailabilityInRange for calendar-month queries.
     */
    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getUnavailablePeriods(Long crewId, java.time.LocalDate month) {
        LocalDateTime monthStart = month.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = month.withDayOfMonth(month.lengthOfMonth())
                .atTime(23, 59, 59);
        return getAvailabilityInRange(crewId, monthStart, monthEnd);
    }

    /**
     * Updates an availability block (e.g., approve or modify dates).
     */
    @Transactional
    public AvailabilityResponse updateAvailability(Long crewId, Long availabilityId,
                                                    AvailabilityRequest request) {
        CrewAvailability availability = availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CrewAvailability", "id", availabilityId));

        if (!availability.getCrewMember().getCrewId().equals(crewId)) {
            throw new IllegalArgumentException(
                    "Availability block does not belong to crew member " + crewId);
        }

        availability.setAvailabilityType(request.getAvailabilityType());
        availability.setStartDate(request.getStartDate());
        availability.setEndDate(request.getEndDate());
        availability.setNotes(request.getNotes());
        availability.setApproved(request.isApproved());

        return toResponse(availabilityRepository.save(availability));
    }

    /**
     * Deletes an availability block.
     */
    @Transactional
    public void deleteAvailability(Long crewId, Long availabilityId) {
        CrewAvailability availability = availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CrewAvailability", "id", availabilityId));

        if (!availability.getCrewMember().getCrewId().equals(crewId)) {
            throw new IllegalArgumentException(
                    "Availability block does not belong to crew member " + crewId);
        }

        availabilityRepository.delete(availability);
    }

    /**
     * Checks if a crew member is available during a given time window.
     * Returns true if NO approved leave/training/medical blocks overlap.
     */
    @Transactional(readOnly = true)
    public boolean isCrewAvailable(Long crewId, LocalDateTime start, LocalDateTime end) {
        List<CrewAvailability> blocks = availabilityRepository
                .findOverlappingAvailability(crewId, start, end);
        return blocks.isEmpty();
    }

    private AvailabilityResponse toResponse(CrewAvailability a) {
        return AvailabilityResponse.builder()
                .id(a.getId())
                .crewId(a.getCrewMember().getCrewId())
                .crewName(a.getCrewMember().getName())
                .availabilityType(a.getAvailabilityType())
                .startDate(a.getStartDate())
                .endDate(a.getEndDate())
                .notes(a.getNotes())
                .approved(a.isApproved())
                .createdAt(a.getCreatedAt())
                .createdBy(a.getCreatedBy())
                .build();
    }
}

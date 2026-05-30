package com.skycrew.repository;

import com.skycrew.model.CrewAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AvailabilityRepository extends JpaRepository<CrewAvailability, Long> {

    /**
     * Finds all availability blocks for a crew member.
     */
    List<CrewAvailability> findByCrewMemberCrewIdOrderByStartDateAsc(Long crewId);

    /**
     * Finds approved availability blocks that overlap with a given time window.
     * Used by the rostering engine to check if crew is available.
     */
    @Query("SELECT a FROM CrewAvailability a WHERE a.crewMember.crewId = :crewId " +
           "AND a.approved = true " +
           "AND a.startDate < :endTime AND a.endDate > :startTime")
    List<CrewAvailability> findOverlappingAvailability(
            @Param("crewId") Long crewId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Finds all availability blocks for a crew member within a date range (month view).
     */
    @Query("SELECT a FROM CrewAvailability a WHERE a.crewMember.crewId = :crewId " +
           "AND a.startDate < :endDate AND a.endDate > :startDate " +
           "ORDER BY a.startDate ASC")
    List<CrewAvailability> findByCrewAndDateRange(
            @Param("crewId") Long crewId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}

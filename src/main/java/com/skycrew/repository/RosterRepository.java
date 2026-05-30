package com.skycrew.repository;

import com.skycrew.model.Roster;
import com.skycrew.model.RosterStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RosterRepository extends JpaRepository<Roster, Long> {

    Page<Roster> findAll(Pageable pageable);

    List<Roster> findByCrewMember_CrewId(Long crewId);

    Page<Roster> findByCrewMember_CrewId(Long crewId, Pageable pageable);

    List<Roster> findByFlight_FlightId(Long flightId);

    List<Roster> findByStatus(RosterStatus status);

    /**
     * Find all confirmed roster entries for a crew member whose flights overlap
     * with a given time window. Used for conflict detection.
     */
    @Query("SELECT r FROM Roster r JOIN FETCH r.flight f " +
           "WHERE r.crewMember.crewId = :crewId " +
           "AND r.status = 'CONFIRMED' " +
           "AND f.departureTime < :endTime " +
           "AND f.arrivalTime > :startTime")
    List<Roster> findConflictingRosters(
            @Param("crewId") Long crewId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Find all confirmed roster entries for a given crew member, ordered by departure time.
     * Used for fatigue rule and monthly hours checking.
     */
    @Query("SELECT r FROM Roster r JOIN FETCH r.flight f " +
           "WHERE r.crewMember.crewId = :crewId " +
           "AND r.status = 'CONFIRMED' " +
           "ORDER BY f.departureTime ASC")
    List<Roster> findConfirmedRostersByCrewIdOrderByDeparture(@Param("crewId") Long crewId);

    /**
     * Find confirmed rosters for a crew member within a specific month for hours calculation.
     */
    @Query("SELECT r FROM Roster r JOIN FETCH r.flight f " +
           "WHERE r.crewMember.crewId = :crewId " +
           "AND r.status = 'CONFIRMED' " +
           "AND f.departureTime >= :monthStart " +
           "AND f.departureTime < :monthEnd")
    List<Roster> findConfirmedRostersInMonth(
            @Param("crewId") Long crewId,
            @Param("monthStart") LocalDateTime monthStart,
            @Param("monthEnd") LocalDateTime monthEnd);

    /**
     * Count confirmed assignments for a specific flight, filtered by crew role type.
     */
    @Query("SELECT COUNT(r) FROM Roster r " +
           "WHERE r.flight.flightId = :flightId " +
           "AND r.status = 'CONFIRMED' " +
           "AND TYPE(r.crewMember) = :crewType")
    long countConfirmedByFlightAndCrewType(
            @Param("flightId") Long flightId,
            @Param("crewType") Class<?> crewType);

    // Multi-tenancy aware queries
    @Query("SELECT r FROM Roster r WHERE r.tenantId = :tenantId")
    Page<Roster> findAllByTenantId(@Param("tenantId") String tenantId, Pageable pageable);

    @Query("SELECT r FROM Roster r WHERE r.crewMember.crewId = :crewId AND r.tenantId = :tenantId")
    List<Roster> findByCrewIdAndTenantId(
            @Param("crewId") Long crewId,
            @Param("tenantId") String tenantId);
}

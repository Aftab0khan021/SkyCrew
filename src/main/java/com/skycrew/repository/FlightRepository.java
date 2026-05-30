package com.skycrew.repository;

import com.skycrew.model.Flight;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {

    Page<Flight> findAll(Pageable pageable);

    Optional<Flight> findByFlightNumber(String flightNumber);

    List<Flight> findByDepartureTimeBetween(LocalDateTime start, LocalDateTime end);

    // Multi-tenancy aware queries
    @Query("SELECT f FROM Flight f WHERE f.tenantId = :tenantId")
    Page<Flight> findAllByTenantId(@Param("tenantId") String tenantId, Pageable pageable);

    @Query("SELECT f FROM Flight f WHERE f.flightNumber = :flightNumber AND f.tenantId = :tenantId")
    Optional<Flight> findByFlightNumberAndTenantId(
            @Param("flightNumber") String flightNumber,
            @Param("tenantId") String tenantId);

    @Query("SELECT f FROM Flight f WHERE f.departureTime BETWEEN :start AND :end AND f.tenantId = :tenantId")
    List<Flight> findByDepartureTimeBetweenAndTenantId(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("tenantId") String tenantId);
}

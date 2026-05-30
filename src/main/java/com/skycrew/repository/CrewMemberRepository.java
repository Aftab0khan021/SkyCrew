package com.skycrew.repository;

import com.skycrew.model.CrewMember;
import com.skycrew.model.CrewRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CrewMemberRepository extends JpaRepository<CrewMember, Long> {

    Page<CrewMember> findAll(Pageable pageable);

    List<CrewMember> findByBaseAirport(String baseAirport);

    List<CrewMember> findByRole(CrewRole role);

    // Multi-tenancy aware queries
    @Query("SELECT c FROM CrewMember c WHERE c.tenantId = :tenantId")
    List<CrewMember> findAllByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT c FROM CrewMember c WHERE c.tenantId = :tenantId")
    Page<CrewMember> findAllByTenantId(@Param("tenantId") String tenantId, Pageable pageable);

    @Query("SELECT c FROM CrewMember c WHERE c.baseAirport = :airport AND c.tenantId = :tenantId")
    List<CrewMember> findByBaseAirportAndTenantId(
            @Param("airport") String baseAirport,
            @Param("tenantId") String tenantId);
}

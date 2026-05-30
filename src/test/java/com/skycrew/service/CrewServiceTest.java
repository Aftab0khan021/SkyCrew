package com.skycrew.service;

import com.skycrew.dto.CrewMemberRequest;
import com.skycrew.dto.CrewMemberResponse;
import com.skycrew.dto.PagedResponse;
import com.skycrew.exception.ResourceNotFoundException;
import com.skycrew.model.*;
import com.skycrew.repository.CrewMemberRepository;
import com.skycrew.repository.RosterRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrewServiceTest {

    @Mock
    private CrewMemberRepository crewMemberRepository;

    @Mock
    private RosterRepository rosterRepository;

    @InjectMocks
    private CrewService crewService;

    private CrewMemberRequest cockpitRequest;
    private CockpitCrew savedCaptain;

    @BeforeEach
    void setUp() {
        cockpitRequest = CrewMemberRequest.builder()
                .name("Captain Smith")
                .role(CrewRole.CAPTAIN)
                .baseAirport("JFK")
                .maxMonthlyHours(100)
                .crewType("COCKPIT")
                .licenseNumber("CPL-12345")
                .typeRatings("B737,A320")
                .build();

        savedCaptain = new CockpitCrew();
        savedCaptain.setCrewId(1L);
        savedCaptain.setName("Captain Smith");
        savedCaptain.setRole(CrewRole.CAPTAIN);
        savedCaptain.setBaseAirport("JFK");
        savedCaptain.setMaxMonthlyHours(100);
        savedCaptain.setLicenseNumber("CPL-12345");
        savedCaptain.setTypeRatings("B737,A320");
    }

    @Test
    @DisplayName("Should create cockpit crew member successfully")
    void shouldCreateCockpitCrew() {
        when(crewMemberRepository.save(any(CrewMember.class))).thenReturn(savedCaptain);

        CrewMemberResponse response = crewService.createCrewMember(cockpitRequest);

        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Captain Smith");
        assertThat(response.getCrewType()).isEqualTo("COCKPIT");
        assertThat(response.getLicenseNumber()).isEqualTo("CPL-12345");
        verify(crewMemberRepository).save(any(CockpitCrew.class));
    }

    @Test
    @DisplayName("Should create cabin crew member successfully")
    void shouldCreateCabinCrew() {
        CrewMemberRequest cabinRequest = CrewMemberRequest.builder()
                .name("Flight Attendant Jones")
                .role(CrewRole.FLIGHT_ATTENDANT)
                .baseAirport("LAX")
                .maxMonthlyHours(120)
                .crewType("CABIN")
                .languagesSpoken("English,French")
                .build();

        CabinCrew savedCabin = new CabinCrew();
        savedCabin.setCrewId(2L);
        savedCabin.setName("Flight Attendant Jones");
        savedCabin.setRole(CrewRole.FLIGHT_ATTENDANT);
        savedCabin.setBaseAirport("LAX");
        savedCabin.setMaxMonthlyHours(120);
        savedCabin.setLanguagesSpoken("English,French");

        when(crewMemberRepository.save(any(CrewMember.class))).thenReturn(savedCabin);

        CrewMemberResponse response = crewService.createCrewMember(cabinRequest);

        assertThat(response.getCrewType()).isEqualTo("CABIN");
        assertThat(response.getLanguagesSpoken()).isEqualTo("English,French");
    }

    @Test
    @DisplayName("Should throw error for invalid crew type")
    void shouldThrowForInvalidCrewType() {
        CrewMemberRequest badRequest = CrewMemberRequest.builder()
                .name("Test")
                .role(CrewRole.CAPTAIN)
                .baseAirport("JFK")
                .maxMonthlyHours(100)
                .crewType("INVALID")
                .build();

        assertThatThrownBy(() -> crewService.createCrewMember(badRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid crew type");
    }

    @Test
    @DisplayName("Should throw 404 when crew member not found")
    void shouldThrow404_WhenCrewNotFound() {
        when(crewMemberRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> crewService.getCrewMemberById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should return paginated crew members")
    void shouldReturnPaginatedCrew() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<CrewMember> page = new PageImpl<>(List.of(savedCaptain), pageable, 1);
        when(crewMemberRepository.findAll(pageable)).thenReturn(page);

        PagedResponse<CrewMemberResponse> response = crewService.getAllCrewMembers(pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getName()).isEqualTo("Captain Smith");
    }
}

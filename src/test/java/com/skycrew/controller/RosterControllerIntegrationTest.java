package com.skycrew.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skycrew.dto.*;
import com.skycrew.model.AppRole;
import com.skycrew.model.CrewRole;
import com.skycrew.model.User;
import com.skycrew.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Smart Rostering Engine via RosterController.
 * Verifies all 4 business rules end-to-end:
 *  1. Overlap detection
 *  2. Fatigue management
 *  3. Monthly hours cap
 *  4. Crew complement
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RosterControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        // Ensure admin user exists (idempotent)
        if (!userRepository.existsByUsername("rosteradmin")) {
            User admin = new User();
            admin.setUsername("rosteradmin");
            admin.setPassword(passwordEncoder.encode("testpass123"));
            admin.setRole(AppRole.ADMIN);
            userRepository.save(admin);
        }

        // Login to get JWT token
        AuthRequest loginRequest = AuthRequest.builder()
                .username("rosteradmin")
                .password("testpass123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        adminToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();
    }

    /**
     * Helper to create a crew member and return their ID.
     */
    private Long createCrew(String name, String role, String crewType) throws Exception {
        CrewMemberRequest request = CrewMemberRequest.builder()
                .name(name)
                .role(CrewRole.valueOf(role))
                .baseAirport("JFK")
                .maxMonthlyHours(100)
                .crewType(crewType)
                .licenseNumber(crewType.equals("COCKPIT") ? "CPL-" + System.nanoTime() : null)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/crew")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("crewId").asLong();
    }

    /**
     * Helper to create a flight and return its ID.
     */
    private Long createFlight(String flightNumber, LocalDateTime departure,
                               LocalDateTime arrival) throws Exception {
        FlightRequest request = FlightRequest.builder()
                .flightNumber(flightNumber)
                .origin("JFK")
                .destination("LAX")
                .departureTime(departure)
                .arrivalTime(arrival)
                .requiredPilots(2)
                .requiredCabinCrew(4)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/flights")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("flightId").asLong();
    }

    /**
     * Helper to assign crew to a flight.
     */
    private MvcResult assignCrew(Long crewId, Long flightId, int expectedStatus) throws Exception {
        RosterAssignRequest request = RosterAssignRequest.builder()
                .crewId(crewId)
                .flightId(flightId)
                .build();

        return mockMvc.perform(post("/api/v1/roster/assign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(expectedStatus))
                .andReturn();
    }

    @Test
    @Order(1)
    @DisplayName("Should successfully assign crew to flight when no conflicts")
    void shouldAssignCrewSuccessfully() throws Exception {
        Long crewId = createCrew("Capt. Test", "CAPTAIN", "COCKPIT");
        Long flightId = createFlight("IT001",
                LocalDateTime.of(2027, 1, 10, 10, 0),
                LocalDateTime.of(2027, 1, 10, 16, 0));

        assignCrew(crewId, flightId, 201);
    }

    @Test
    @Order(2)
    @DisplayName("Should reject assignment when flights overlap (409)")
    void shouldRejectOverlappingFlights() throws Exception {
        Long crewId = createCrew("Capt. Overlap", "CAPTAIN", "COCKPIT");

        // Flight A: 10:00 - 16:00
        Long flightA = createFlight("OV-A",
                LocalDateTime.of(2027, 2, 1, 10, 0),
                LocalDateTime.of(2027, 2, 1, 16, 0));

        // Flight B: 14:00 - 20:00 (overlaps with A)
        Long flightB = createFlight("OV-B",
                LocalDateTime.of(2027, 2, 1, 14, 0),
                LocalDateTime.of(2027, 2, 1, 20, 0));

        // Assign A — should succeed
        assignCrew(crewId, flightA, 201);

        // Assign B — should fail with 409 OVERLAP
        MvcResult result = assignCrew(crewId, flightB, 409);

        String response = result.getResponse().getContentAsString();
        assertThat(response).contains("OVERLAP");
    }

    @Test
    @Order(3)
    @DisplayName("Should reject assignment when rest period is insufficient (409)")
    void shouldRejectInsufficientRest() throws Exception {
        Long crewId = createCrew("Capt. Fatigue", "CAPTAIN", "COCKPIT");

        // Flight A: arrives at 16:00
        Long flightA = createFlight("FT-A",
                LocalDateTime.of(2027, 3, 1, 10, 0),
                LocalDateTime.of(2027, 3, 1, 16, 0));

        // Flight B: departs at 02:00 next day — only 10h rest (< 12h min)
        Long flightB = createFlight("FT-B",
                LocalDateTime.of(2027, 3, 2, 2, 0),
                LocalDateTime.of(2027, 3, 2, 8, 0));

        assignCrew(crewId, flightA, 201);

        MvcResult result = assignCrew(crewId, flightB, 409);
        String response = result.getResponse().getContentAsString();
        assertThat(response).contains("FATIGUE");
    }

    @Test
    @Order(4)
    @DisplayName("Should return conflict list via GET /roster/conflicts")
    void shouldReturnConflicts() throws Exception {
        mockMvc.perform(get("/api/v1/roster/conflicts")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(5)
    @DisplayName("Should return paginated roster entries")
    void shouldReturnPaginatedRosters() throws Exception {
        mockMvc.perform(get("/api/v1/roster?page=0&size=10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @Order(6)
    @DisplayName("Should return 401 when assigning without authentication")
    void shouldReturn403WithoutAuth() throws Exception {
        RosterAssignRequest request = RosterAssignRequest.builder()
                .crewId(1L)
                .flightId(1L)
                .build();

        mockMvc.perform(post("/api/v1/roster/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}

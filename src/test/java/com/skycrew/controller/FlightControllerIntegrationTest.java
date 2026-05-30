package com.skycrew.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skycrew.dto.AuthRequest;
import com.skycrew.dto.FlightRequest;
import com.skycrew.model.AppRole;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for FlightController.
 * Boots the full Spring context with H2, creates an admin user programmatically,
 * and tests the full request→controller→service→repository→database flow.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlightControllerIntegrationTest {

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
        if (!userRepository.existsByUsername("testadmin")) {
            User admin = new User();
            admin.setUsername("testadmin");
            admin.setPassword(passwordEncoder.encode("testpass123"));
            admin.setRole(AppRole.ADMIN);
            userRepository.save(admin);
        }

        // Login to get JWT token
        AuthRequest loginRequest = AuthRequest.builder()
                .username("testadmin")
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

    @Test
    @Order(1)
    @DisplayName("Should create a flight with valid data")
    void shouldCreateFlight() throws Exception {
        FlightRequest request = FlightRequest.builder()
                .flightNumber("SK101")
                .origin("JFK")
                .destination("LAX")
                .departureTime(LocalDateTime.of(2026, 12, 1, 10, 0))
                .arrivalTime(LocalDateTime.of(2026, 12, 1, 16, 0))
                .requiredPilots(2)
                .requiredCabinCrew(4)
                .build();

        mockMvc.perform(post("/api/v1/flights")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flightNumber").value("SK101"))
                .andExpect(jsonPath("$.origin").value("JFK"))
                .andExpect(jsonPath("$.destination").value("LAX"))
                .andExpect(jsonPath("$.flightId").isNumber());
    }

    @Test
    @Order(2)
    @DisplayName("Should return 400 for invalid flight data (missing required fields)")
    void shouldReturn400ForInvalidData() throws Exception {
        FlightRequest badRequest = FlightRequest.builder()
                .flightNumber("")  // blank
                .origin("J")       // too short
                .build();

        mockMvc.perform(post("/api/v1/flights")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    @Order(3)
    @DisplayName("Should return paginated flights")
    void shouldReturnPaginatedFlights() throws Exception {
        mockMvc.perform(get("/api/v1/flights?page=0&size=10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @Order(4)
    @DisplayName("Should return 401 for unauthenticated request")
    void shouldReturn403WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/flights"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    @DisplayName("Should return 404 for non-existent flight")
    void shouldReturn404ForNonExistentFlight() throws Exception {
        mockMvc.perform(get("/api/v1/flights/9999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(6)
    @DisplayName("Should delete a flight successfully")
    void shouldDeleteFlight() throws Exception {
        // First create a flight to delete
        FlightRequest request = FlightRequest.builder()
                .flightNumber("SK999")
                .origin("ORD")
                .destination("SFO")
                .departureTime(LocalDateTime.of(2026, 12, 15, 8, 0))
                .arrivalTime(LocalDateTime.of(2026, 12, 15, 12, 0))
                .requiredPilots(2)
                .requiredCabinCrew(3)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/flights")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long flightId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("flightId").asLong();

        // Now delete it
        mockMvc.perform(delete("/api/v1/flights/" + flightId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Verify it's gone
        mockMvc.perform(get("/api/v1/flights/" + flightId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }
}

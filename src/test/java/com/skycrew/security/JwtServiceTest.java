package com.skycrew.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JWT token generation, parsing, and validation.
 */
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails adminUser;
    private UserDetails crewUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        // Generate a valid 256-bit (32 byte) Base64-encoded secret
        String testSecret = Base64.getEncoder().encodeToString(
                "ThisIsATestSecretKeyWith32Bytes!".getBytes());
        ReflectionTestUtils.setField(jwtService, "secretKey", testSecret);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000L); // 1 hour

        adminUser = new User("admin", "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        crewUser = new User("crew_member", "password",
                List.of(new SimpleGrantedAuthority("ROLE_CREW")));
    }

    @Test
    @DisplayName("Should generate a valid JWT token")
    void shouldGenerateToken() {
        String token = jwtService.generateToken(adminUser);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    @DisplayName("Should extract username from token")
    void shouldExtractUsername() {
        String token = jwtService.generateToken(adminUser);
        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("admin");
    }

    @Test
    @DisplayName("Should extract username for crew user")
    void shouldExtractUsernameForCrewUser() {
        String token = jwtService.generateToken(crewUser);
        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("crew_member");
    }

    @Test
    @DisplayName("Should validate token for correct user")
    void shouldValidateTokenForCorrectUser() {
        String token = jwtService.generateToken(adminUser);
        boolean isValid = jwtService.isTokenValid(token, adminUser);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject token for wrong user")
    void shouldRejectTokenForWrongUser() {
        String token = jwtService.generateToken(adminUser);
        boolean isValid = jwtService.isTokenValid(token, crewUser);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should extract non-null expiration date")
    void shouldExtractExpiration() {
        String token = jwtService.generateToken(adminUser);

        assertThat(jwtService.extractExpiration(token)).isNotNull();
        assertThat(jwtService.extractExpiration(token)).isInTheFuture();
    }

    @Test
    @DisplayName("Should reject expired token")
    void shouldRejectExpiredToken() {
        // Set expiry to 0ms (instantly expired)
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 0L);
        String token = jwtService.generateToken(adminUser);

        // Token should be expired immediately
        assertThatThrownBy(() -> jwtService.isTokenValid(token, adminUser))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void shouldGenerateDifferentTokens() {
        String adminToken = jwtService.generateToken(adminUser);
        String crewToken = jwtService.generateToken(crewUser);

        assertThat(adminToken).isNotEqualTo(crewToken);
    }
}

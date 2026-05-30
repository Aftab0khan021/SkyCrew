package com.skycrew.controller;

import com.skycrew.dto.AuthRequest;
import com.skycrew.dto.AuthResponse;
import com.skycrew.model.AppRole;
import com.skycrew.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration and JWT token authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate and get JWT token",
               description = "Returns a JWT token on successful authentication. " +
                       "Use this token in the Authorization header as 'Bearer <token>'.")
    @ApiResponse(responseCode = "200", description = "Login successful")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user",
               description = "Creates a new application user. Requires ADMIN role. " +
                       "Default role for new users is CREW.")
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    @ApiResponse(responseCode = "400", description = "Username already taken or invalid data")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody AuthRequest request,
            @RequestParam(defaultValue = "CREW") AppRole role) {
        AuthResponse response = authService.register(request, role);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}

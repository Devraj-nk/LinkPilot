package com.example.linkpilot.controller;

import com.example.linkpilot.dto.AuthResponse;
import com.example.linkpilot.dto.LoginRequest;
import com.example.linkpilot.dto.RefreshRequest;
import com.example.linkpilot.dto.RegisterRequest;
import com.example.linkpilot.exception.RateLimitExceededException;
import com.example.linkpilot.security.RateLimiterService;
import com.example.linkpilot.service.AuthService;
import com.example.linkpilot.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // Keyed by IP, not by account - there's no authenticated user yet on either of these
    // routes, and IP is exactly the axis credential-stuffing / registration-spam abuse
    // happens on.
    private static final int LOGIN_LIMIT = 10;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(1);
    private static final int REGISTER_LIMIT = 5;
    private static final Duration REGISTER_WINDOW = Duration.ofHours(1);

    private final AuthService authService;
    private final RateLimiterService rateLimiter;

    public AuthController(AuthService authService, RateLimiterService rateLimiter) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String ip = ClientIpResolver.resolve(httpRequest);
        if (!rateLimiter.allow("ratelimit:register:" + ip, REGISTER_LIMIT, REGISTER_WINDOW)) {
            throw new RateLimitExceededException("Too many accounts created from this address - try again later");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = ClientIpResolver.resolve(httpRequest);
        if (!rateLimiter.allow("ratelimit:login:" + ip, LOGIN_LIMIT, LOGIN_WINDOW)) {
            throw new RateLimitExceededException("Too many login attempts - try again in a minute");
        }
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}

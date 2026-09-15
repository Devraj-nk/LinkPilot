package com.example.linkpilot.service;

import com.example.linkpilot.dto.AuthResponse;
import com.example.linkpilot.dto.LoginRequest;
import com.example.linkpilot.dto.RegisterRequest;
import com.example.linkpilot.exception.DuplicateResourceException;
import com.example.linkpilot.model.RefreshToken;
import com.example.linkpilot.model.User;
import com.example.linkpilot.model.UserRole;
import com.example.linkpilot.repository.UserRepository;
import com.example.linkpilot.security.JwtService;
import com.example.linkpilot.security.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, refreshTokenService);
        // save() just echoes back whatever entity was passed to it, matching how JPA's
        // save() returns the (now-managed) same instance.
        lenient().when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("fake-access-token");
        lenient().when(refreshTokenService.issue(any(User.class))).thenReturn("fake-refresh-token");
    }

    @Test
    void register_createsUserAndIssuesTokens() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");

        AuthResponse response = authService.register(new RegisterRequest("new@example.com", "password123", "New User"));

        assertThat(response.accessToken()).isEqualTo("fake-access-token");
        assertThat(response.refreshToken()).isEqualTo("fake-refresh-token");
        assertThat(response.user().email()).isEqualTo("new@example.com");
        assertThat(response.user().name()).isEqualTo("New User");
        assertThat(response.user().role()).isEqualTo(UserRole.USER);

        verify(userRepository).save(argThat(u ->
                u.getEmail().equals("new@example.com") && u.getPasswordHash().equals("hashed-password")
        ));
    }

    @Test
    void register_rejectsDuplicateEmail() {
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () ->
                authService.register(new RegisterRequest("taken@example.com", "password123", "Someone"))
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_succeedsWithCorrectPassword() {
        User user = existingUser("user@example.com", "hashed-password");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);

        AuthResponse response = authService.login(new LoginRequest("user@example.com", "password123"));

        assertThat(response.accessToken()).isEqualTo("fake-access-token");
        assertThat(response.user().email()).isEqualTo("user@example.com");
    }

    @Test
    void login_rejectsWrongPassword() {
        User user = existingUser("user@example.com", "hashed-password");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () ->
                authService.login(new LoginRequest("user@example.com", "wrong-password"))
        );
    }

    @Test
    void login_rejectsUnknownEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () ->
                authService.login(new LoginRequest("nobody@example.com", "password123"))
        );
    }

    @Test
    void refresh_rotatesToken_revokingOldAndIssuingNew() {
        User user = existingUser("user@example.com", "hashed-password");
        RefreshToken oldToken = new RefreshToken();
        oldToken.setUser(user);
        when(refreshTokenService.validate("old-raw-token")).thenReturn(oldToken);

        AuthResponse response = authService.refresh("old-raw-token");

        verify(refreshTokenService).revoke(oldToken);
        verify(refreshTokenService).issue(user);
        assertThat(response.accessToken()).isEqualTo("fake-access-token");
        assertThat(response.refreshToken()).isEqualTo("fake-refresh-token");
    }

    @Test
    void logout_revokesThePresentedToken() {
        authService.logout("some-raw-token");

        verify(refreshTokenService).revoke("some-raw-token");
    }

    private User existingUser(String email, String passwordHash) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setName("Existing User");
        user.setRole(UserRole.USER);
        return user;
    }
}

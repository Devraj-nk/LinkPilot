package com.example.linkpilot.service;

import com.example.linkpilot.dto.AuthResponse;
import com.example.linkpilot.dto.LoginRequest;
import com.example.linkpilot.dto.RegisterRequest;
import com.example.linkpilot.dto.UserResponse;
import com.example.linkpilot.exception.DuplicateResourceException;
import com.example.linkpilot.model.RefreshToken;
import com.example.linkpilot.model.User;
import com.example.linkpilot.model.UserRole;
import com.example.linkpilot.repository.UserRepository;
import com.example.linkpilot.security.JwtService;
import com.example.linkpilot.security.RefreshTokenService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setName(request.name());
        user.setRole(UserRole.USER);
        user = userRepository.save(user);

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken token = refreshTokenService.validate(rawRefreshToken);
        User user = token.getUser();
        refreshTokenService.revoke(token);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = refreshTokenService.issue(user);
        return new AuthResponse(accessToken, refreshToken, UserResponse.from(user));
    }
}

package com.example.linkpilot.security;

import com.example.linkpilot.exception.ForbiddenException;
import com.example.linkpilot.model.RefreshToken;
import com.example.linkpilot.model.User;
import com.example.linkpilot.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHasher tokenHasher;
    private final long refreshTokenExpirationDays;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            TokenHasher tokenHasher,
            @Value("${jwt.refresh-token-expiration-days}") long refreshTokenExpirationDays
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHasher = tokenHasher;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    public String issue(User user) {
        String rawToken = tokenHasher.generateRawToken();

        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(tokenHasher.hash(rawToken));
        entity.setExpiresAt(OffsetDateTime.now().plusDays(refreshTokenExpirationDays));
        refreshTokenRepository.save(entity);

        return rawToken;
    }

    public RefreshToken validate(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHasher.hash(rawToken))
                .orElseThrow(() -> new ForbiddenException("Invalid refresh token"));
        if (token.getRevokedAt() != null || token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ForbiddenException("Refresh token expired or revoked");
        }
        return token;
    }

    public void revoke(RefreshToken token) {
        token.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(token);
    }

    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(tokenHasher.hash(rawToken)).ifPresent(this::revoke);
    }
}

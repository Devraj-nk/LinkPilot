package com.example.linkpilot.dto;

import com.example.linkpilot.model.APIKey;

import java.time.OffsetDateTime;
import java.util.UUID;

public record APIKeyResponse(
        UUID id,
        String name,
        OffsetDateTime lastUsedAt,
        OffsetDateTime expiresAt,
        OffsetDateTime revokedAt,
        OffsetDateTime createdAt
) {
    public static APIKeyResponse from(APIKey apiKey) {
        return new APIKeyResponse(
                apiKey.getId(),
                apiKey.getName(),
                apiKey.getLastUsedAt(),
                apiKey.getExpiresAt(),
                apiKey.getRevokedAt(),
                apiKey.getCreatedAt()
        );
    }
}

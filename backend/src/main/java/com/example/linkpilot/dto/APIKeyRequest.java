package com.example.linkpilot.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;

public record APIKeyRequest(
        @NotBlank String name,
        OffsetDateTime expiresAt
) {
}

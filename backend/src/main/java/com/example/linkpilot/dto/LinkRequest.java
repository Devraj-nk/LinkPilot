package com.example.linkpilot.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LinkRequest(
        @NotBlank String originalUrl,
        String title,
        UUID campaignId,
        UUID domainId,
        String customCode,
        OffsetDateTime expiresAt
) {
}

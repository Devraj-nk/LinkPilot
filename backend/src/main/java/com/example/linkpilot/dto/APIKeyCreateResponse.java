package com.example.linkpilot.dto;

import java.util.UUID;

public record APIKeyCreateResponse(
        UUID id,
        String name,
        String rawKey
) {
}

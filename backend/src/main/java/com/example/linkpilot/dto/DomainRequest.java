package com.example.linkpilot.dto;

import jakarta.validation.constraints.NotBlank;

public record DomainRequest(
        @NotBlank String domain
) {
}

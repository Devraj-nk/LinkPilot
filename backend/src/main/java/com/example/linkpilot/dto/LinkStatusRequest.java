package com.example.linkpilot.dto;

import com.example.linkpilot.model.LinkStatus;
import jakarta.validation.constraints.NotNull;

public record LinkStatusRequest(
        @NotNull LinkStatus status
) {
}

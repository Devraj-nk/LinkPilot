package com.example.linkpilot.dto;

import com.example.linkpilot.model.CampaignStatus;
import jakarta.validation.constraints.NotBlank;

public record CampaignRequest(
        @NotBlank String name,
        String description,
        CampaignStatus status
) {
}

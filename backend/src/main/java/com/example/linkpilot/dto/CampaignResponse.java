package com.example.linkpilot.dto;

import com.example.linkpilot.model.Campaign;
import com.example.linkpilot.model.CampaignStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CampaignResponse(
        UUID id,
        String name,
        String description,
        CampaignStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static CampaignResponse from(Campaign campaign) {
        return new CampaignResponse(
                campaign.getId(),
                campaign.getName(),
                campaign.getDescription(),
                campaign.getStatus(),
                campaign.getCreatedAt(),
                campaign.getUpdatedAt()
        );
    }
}

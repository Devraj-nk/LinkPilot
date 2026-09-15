package com.example.linkpilot.dto;

import com.example.linkpilot.model.Link;
import com.example.linkpilot.model.LinkStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LinkResponse(
        UUID id,
        String shortCode,
        String originalUrl,
        String title,
        LinkStatus status,
        UUID campaignId,
        UUID domainId,
        String shortUrl,
        int clickCount,
        OffsetDateTime expiresAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static LinkResponse from(Link link, String defaultPublicBaseUrl) {
        String host = link.getDomain() != null ? link.getDomain().getDomain() : defaultPublicBaseUrl;
        String shortUrl = host.startsWith("http") ? host + "/" + link.getShortCode() : "https://" + host + "/" + link.getShortCode();
        return new LinkResponse(
                link.getId(),
                link.getShortCode(),
                link.getOriginalUrl(),
                link.getTitle(),
                link.getStatus(),
                link.getCampaign() != null ? link.getCampaign().getId() : null,
                link.getDomain() != null ? link.getDomain().getId() : null,
                shortUrl,
                link.getClickCount(),
                link.getExpiresAt(),
                link.getCreatedAt(),
                link.getUpdatedAt()
        );
    }
}

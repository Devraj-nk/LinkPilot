package com.example.linkpilot.dto;

import com.example.linkpilot.model.Domain;
import com.example.linkpilot.model.DomainVerificationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DomainResponse(
        UUID id,
        String domain,
        DomainVerificationStatus verificationStatus,
        OffsetDateTime verifiedAt,
        OffsetDateTime createdAt
) {
    public static DomainResponse from(Domain domain) {
        return new DomainResponse(
                domain.getId(),
                domain.getDomain(),
                domain.getVerificationStatus(),
                domain.getVerifiedAt(),
                domain.getCreatedAt()
        );
    }
}

package com.example.linkpilot.dto;

import com.example.linkpilot.model.User;
import com.example.linkpilot.model.UserRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String name,
        UserRole role,
        boolean verified,
        OffsetDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.isVerified(),
                user.getCreatedAt()
        );
    }
}

package com.example.linkpilot.security;

import com.example.linkpilot.model.UserRole;

import java.util.UUID;

public record AuthenticatedUser(UUID id, String email, UserRole role) {
}

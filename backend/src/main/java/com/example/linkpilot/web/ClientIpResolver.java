package com.example.linkpilot.web;

import jakarta.servlet.http.HttpServletRequest;

/** Shared by anything keying a per-IP limit or metric off the request (rate limiting, click analytics). */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

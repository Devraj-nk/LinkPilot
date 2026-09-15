package com.example.linkpilot.controller;

import com.example.linkpilot.dto.LinkAnalyticsResponse;
import com.example.linkpilot.security.AuthenticatedUser;
import com.example.linkpilot.service.AnalyticsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/api/links/{id}/analytics")
    public LinkAnalyticsResponse getAnalytics(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID id) {
        return analyticsService.getForLink(principal.id(), id);
    }
}

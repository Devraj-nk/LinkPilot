package com.example.linkpilot.dto;

import java.util.List;

public record LinkAnalyticsResponse(
        int totalClicks,
        List<DailyClickPoint> dailyClicks,
        List<NamedCount> deviceBreakdown,
        List<NamedCount> topReferrers,
        boolean analyticsAvailable
) {
}

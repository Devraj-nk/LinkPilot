package com.example.linkpilot.service;

import com.example.linkpilot.dto.DailyClickPoint;
import com.example.linkpilot.dto.LinkAnalyticsResponse;
import com.example.linkpilot.dto.NamedCount;
import com.example.linkpilot.model.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Reads query the raw click_events table directly rather than the pre-aggregated
 * materialized views in aggregators.sql: those views' uniqExact(ip_hash) is only exact
 * within a single INSERT batch, and ClickEventService always inserts one row at a time,
 * so summing unique_visitors across batches would just equal the view count - not
 * actually unique. Querying click_events directly is both correct and fast enough at
 * this project's scale; the materialized views are still created (they're the right
 * tool once you're batching inserts at real volume) but aren't what the dashboard reads.
 */
@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private static final String DAILY_SQL = """
            SELECT toString(date) AS day, count() AS views, uniqExact(ip_hash) AS uniqueVisitors
            FROM click_events
            WHERE link_id = ? AND date >= today() - 13
            GROUP BY day
            ORDER BY day
            """;

    private static final String DEVICE_SQL = """
            SELECT device_type AS name, count() AS cnt
            FROM click_events
            WHERE link_id = ?
            GROUP BY device_type
            ORDER BY cnt DESC
            LIMIT 10
            """;

    private static final String REFERRER_SQL = """
            SELECT referrer AS name, count() AS cnt
            FROM click_events
            WHERE link_id = ? AND referrer != ''
            GROUP BY referrer
            ORDER BY cnt DESC
            LIMIT 10
            """;

    private final LinkService linkService;
    private final JdbcTemplate clickHouseJdbcTemplate;

    public AnalyticsService(LinkService linkService, JdbcTemplate clickHouseJdbcTemplate) {
        this.linkService = linkService;
        this.clickHouseJdbcTemplate = clickHouseJdbcTemplate;
    }

    public LinkAnalyticsResponse getForLink(UUID userId, UUID linkId) {
        Link link = linkService.getForUser(userId, linkId);
        String id = link.getId().toString();

        try {
            List<DailyClickPoint> daily = clickHouseJdbcTemplate.query(
                    DAILY_SQL,
                    (rs, i) -> new DailyClickPoint(rs.getString("day"), rs.getLong("views"), rs.getLong("uniqueVisitors")),
                    id
            );
            List<NamedCount> devices = clickHouseJdbcTemplate.query(
                    DEVICE_SQL,
                    (rs, i) -> new NamedCount(rs.getString("name"), rs.getLong("cnt")),
                    id
            );
            List<NamedCount> referrers = clickHouseJdbcTemplate.query(
                    REFERRER_SQL,
                    (rs, i) -> new NamedCount(rs.getString("name"), rs.getLong("cnt")),
                    id
            );
            return new LinkAnalyticsResponse(link.getClickCount(), daily, devices, referrers, true);
        } catch (Exception e) {
            log.warn("ClickHouse query failed for link {}: {}", linkId, e.getMessage());
            return new LinkAnalyticsResponse(link.getClickCount(), List.of(), List.of(), List.of(), false);
        }
    }
}

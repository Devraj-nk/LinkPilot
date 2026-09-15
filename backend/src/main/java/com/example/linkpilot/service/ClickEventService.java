package com.example.linkpilot.service;

import com.example.linkpilot.analytics.UserAgentParser;
import com.example.linkpilot.model.Link;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Records one click_events row in ClickHouse per redirect. Runs off the request thread
 * (see AsyncConfig) and swallows every failure internally - analytics must never be able
 * to break a redirect, which is why nothing here ever propagates an exception.
 */
@Service
public class ClickEventService {

    private static final Logger log = LoggerFactory.getLogger(ClickEventService.class);

    private static final String INSERT_SQL = """
            INSERT INTO click_events
                (link_id, timestamp, ip_hash, country, region, city, device_type, browser, operating_system, referrer, user_agent)
            VALUES (?, now64(3), ?, '', '', '', ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate clickHouseJdbcTemplate;

    public ClickEventService(JdbcTemplate clickHouseJdbcTemplate) {
        this.clickHouseJdbcTemplate = clickHouseJdbcTemplate;
    }

    @Async("analyticsExecutor")
    public void recordAsync(Link link, HttpServletRequest request) {
        try {
            String userAgent = request.getHeader("User-Agent");
            UserAgentParser.Result ua = UserAgentParser.parse(userAgent);
            String referrerHost = extractHost(request.getHeader("Referer"));
            long ipHash = hashIp(clientIp(request));

            clickHouseJdbcTemplate.update(
                    INSERT_SQL,
                    link.getId().toString(),
                    ipHash,
                    ua.deviceType(),
                    ua.browser(),
                    ua.operatingSystem(),
                    referrerHost,
                    userAgent == null ? "" : userAgent
            );
        } catch (Exception e) {
            log.warn("Could not record click event for link {}: {}", link.getId(), e.getMessage());
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractHost(String referer) {
        if (referer == null || referer.isBlank()) {
            return "";
        }
        try {
            String host = URI.create(referer).getHost();
            return host != null ? host : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * SHA-256(ip + today's UTC date), truncated to 63 bits (sign bit cleared so it binds
     * cleanly as a plain positive `long` while still fitting ClickHouse's UInt64 column).
     * Salting by the current day means the same visitor hashes to the same value within a
     * day (accurate `uniqExact` daily-unique-visitor counts) but to a different value the
     * next day - a visitor can never be correlated across days from this hash.
     */
    private long hashIp(String ip) {
        try {
            String salted = ip + ":" + LocalDate.now(ZoneOffset.UTC);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(salted.getBytes(StandardCharsets.UTF_8));
            long value = 0;
            for (int i = 0; i < 8; i++) {
                value = (value << 8) | (digest[i] & 0xFF);
            }
            return value & Long.MAX_VALUE;
        } catch (NoSuchAlgorithmException e) {
            return 0L;
        }
    }
}

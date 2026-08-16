-- daily link clicks aggregation

CREATE MATERIALIZED VIEW link_clicks_daily
ENGINE = AggregatingMergeTree()
ORDER BY (link_id, date)
AS SELECT
    link_id,
    date,
    count() AS views,
    uniqExact(ip_hash) AS unique_visitors,
    countIf(device_type = 'mobile') AS mobile_views,
    countIf(device_type = 'desktop') AS desktop_views,
    argMax(country, timestamp) AS top_country  -- Simplified; use topK for real implementation
FROM click_events
GROUP BY link_id, date;

-- Hourly Aggregates Materialized View

CREATE MATERIALIZED VIEW link_clicks_hourly
ENGINE = AggregatingMergeTree()
ORDER BY (link_id, date, hour)
AS SELECT
    link_id,
    date,
    hour,
    count() AS views,
    uniqExact(ip_hash) AS unique_visitors
FROM click_events
GROUP BY link_id, date, hour;

-- Referrer Aggregates Materialized View

CREATE MATERIALIZED VIEW link_clicks_referral
ENGINE = AggregatingMergeTree()
ORDER BY (link_id, referrer)
AS SELECT
    link_id,
    referrer,
    count() AS views,
    uniqExact(ip_hash) AS unique_visitors
FROM click_events
WHERE referrer != '' AND referrer IS NOT NULL
GROUP BY link_id, referrer;

-- 
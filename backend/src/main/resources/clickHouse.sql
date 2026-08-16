CREATE TABLE click_events
(
    event_id UUID DEFAULT generateUUIDv4(),
    link_id UUID NOT NULL,
    timestamp DateTime64(3, 'UTC') NOT NULL,
    ip_hash UInt64 NOT NULL,          -- Anonymized IP for privacy
    country LowCardinality(String),   -- ISO country code
    region LowCardinality(String),    -- ISO subdivision code
    city LowCardinality(String),      -- City name
    device_type LowCardinality(String), -- mobile, desktop, tablet
    browser LowCardinality(String),   -- chrome, firefox, safari, etc.
    operating_system LowCardinality(String), -- windows, macos, linux, ios, android
    referrer LowCardinality(String),  -- Referring domain
    user_agent String,                -- Full user agent (for debugging)
    
    -- Materialized columns for common aggregations
    date Date MATERIALIZED toDate(timestamp),
    hour UInt8 MATERIALIZED toHour(timestamp),
    day_of_week UInt8 MATERIALIZED dayOfWeek(timestamp)
)
ENGINE = MergeTree()
PARTITION BY toYYYYMM(timestamp)        -- Monthly partitions for easy retention
ORDER BY (link_id, timestamp)           -- Optimized for link-based queries
TTL timestamp + INTERVAL 24 MONTH DELETE -- Retain 2 years of click data
SETTINGS index_granularity = 8192;
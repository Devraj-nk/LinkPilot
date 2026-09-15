-- Runs after Hibernate's ddl-auto=update has created the tables (see
-- SqliteSchemaInitializer). Indexes marked UNIQUE exist because SQLite doesn't support
-- "ALTER TABLE ... ADD CONSTRAINT", so Hibernate's own generated unique constraints
-- silently fail against it (logged as a warning, not an error) - a unique index is
-- the actual enforcement mechanism for those columns.

-- Links table indexes (most critical for performance)
CREATE UNIQUE INDEX IF NOT EXISTS idx_links_short_code ON links(short_code);  -- also the uniqueness backstop
CREATE INDEX IF NOT EXISTS idx_links_user_id ON links(user_id);        -- User's links
CREATE INDEX IF NOT EXISTS idx_links_campaign_id ON links(campaign_id); -- Campaign links
CREATE INDEX IF NOT EXISTS idx_links_domain_id ON links(domain_id);     -- Domain-scoped redirect lookup
CREATE INDEX IF NOT EXISTS idx_links_expires_at ON links(expires_at);   -- Expiration cleanup
CREATE INDEX IF NOT EXISTS idx_links_status ON links(status);           -- Active link filtering

-- Users table indexes
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users(email);      -- Login by email, also the uniqueness backstop
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);               -- Admin queries

-- Refresh tokens table indexes
CREATE UNIQUE INDEX IF NOT EXISTS idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- API keys table indexes
CREATE INDEX IF NOT EXISTS idx_api_keys_user_id ON api_keys(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_api_keys_key_hash ON api_keys(key_hash); -- Key validation, also the uniqueness backstop

-- Campaigns table indexes
CREATE INDEX IF NOT EXISTS idx_campaigns_user_id ON campaigns(user_id);
CREATE INDEX IF NOT EXISTS idx_campaigns_status ON campaigns(status);

-- QR codes table indexes
CREATE INDEX IF NOT EXISTS idx_qr_codes_link_id ON qr_codes(link_id);

-- Domains table indexes
CREATE INDEX IF NOT EXISTS idx_domains_user_id ON domains(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_domains_domain ON domains(domain); -- also the uniqueness backstop
CREATE INDEX IF NOT EXISTS idx_domains_verification_status ON domains(verification_status);

-- Links table indexes (most critical for performance)
CREATE INDEX idx_links_short_code ON links(short_code);  -- Ultra-fast redirect lookup
CREATE INDEX idx_links_user_id ON links(user_id);        -- User's links
CREATE INDEX idx_links_campaign_id ON links(campaign_id); -- Campaign links
CREATE INDEX idx_links_expires_at ON links(expires_at);   -- Expiration cleanup
CREATE INDEX idx_links_status ON links(status);           -- Active link filtering

-- Users table indexes
CREATE INDEX idx_users_email ON users(email);             -- Login by email
CREATE INDEX idx_users_role ON users(role);               -- Admin queries

-- Refresh tokens table indexes
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- API keys table indexes
CREATE INDEX idx_api_keys_user_id ON api_keys(user_id);
CREATE INDEX idx_api_keys_key_hash ON api_keys(key_hash); -- Key validation

-- Campaigns table indexes
CREATE INDEX idx_campaigns_user_id ON campaigns(user_id);
CREATE INDEX idx_campaigns_status ON campaigns(status);

-- QR codes table indexes
CREATE INDEX idx_qr_codes_link_id ON qr_codes(link_id);

-- Domains table indexes (optional)
CREATE INDEX idx_domains_user_id ON domains(user_id);
CREATE INDEX idx_domains_domain ON domains(domain);
CREATE INDEX idx_domains_verification_status ON domains(verification_status);
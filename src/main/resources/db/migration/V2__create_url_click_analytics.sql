
CREATE TABLE url_click_analytics (
    short_code VARCHAR(20) PRIMARY KEY,
    total_clicks BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

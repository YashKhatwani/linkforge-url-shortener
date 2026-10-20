
CREATE TABLE processed_click_events (
    event_id VARCHAR(36) PRIMARY KEY,
    short_code VARCHAR(20) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
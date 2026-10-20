CREATE TABLE url_mapping (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    short_code VARCHAR(20) NOT NULL,
    original_url TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ,

    CONSTRAINT uk_url_mapping_short_code
        UNIQUE (short_code),

    CONSTRAINT chk_url_mapping_expiration
        CHECK (expires_at IS NULL OR expires_at > created_at)
);

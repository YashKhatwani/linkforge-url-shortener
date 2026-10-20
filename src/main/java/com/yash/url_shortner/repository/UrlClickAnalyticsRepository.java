
package com.yash.url_shortner.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class UrlClickAnalyticsRepository {

    private final JdbcTemplate jdbcTemplate;

    public UrlClickAnalyticsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ClickAnalytics findAnalyticsByShortCode(String shortCode) {
        String sql = """
        SELECT short_code, total_clicks, updated_at
        FROM url_click_analytics
        WHERE short_code = ?
        """;

        return jdbcTemplate.query(
                sql,
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }

                    return new ClickAnalytics(
                            rs.getString("short_code"),
                            rs.getLong("total_clicks"),
                            rs.getTimestamp("updated_at").toInstant()
                    );
                },
                shortCode
        );
    }

    public record ClickAnalytics(
            String shortCode,
            long totalClicks,
            java.time.Instant updatedAt
    ) {}

    @Transactional
    public boolean processClickEvent(String eventId, String shortCode) {

        String insertEventSql = """
            INSERT INTO processed_click_events (event_id, short_code)
            VALUES (?, ?)
            ON CONFLICT (event_id) DO NOTHING
            """;

        int inserted = jdbcTemplate.update(
                insertEventSql, eventId, shortCode
        );

        if (inserted == 0) {
            // This event was already processed.
            return false;
        }

        String incrementSql = """
            INSERT INTO url_click_analytics
                (short_code, total_clicks, updated_at)
            VALUES (?, 1, CURRENT_TIMESTAMP)
            ON CONFLICT (short_code)
            DO UPDATE SET
                total_clicks = url_click_analytics.total_clicks + 1,
                updated_at = CURRENT_TIMESTAMP
            """;

        jdbcTemplate.update(incrementSql, shortCode);

        return true;
    }

    public java.util.Map<String, Long> findClickCounts(
            java.util.List<String> shortCodes) {

        if (shortCodes.isEmpty()) {
            return java.util.Map.of();
        }

        String placeholders = String.join(
                ",",
                java.util.Collections.nCopies(shortCodes.size(), "?")
        );

        String sql = """
            SELECT short_code, total_clicks
            FROM url_click_analytics
            WHERE short_code IN (%s)
            """.formatted(placeholders);

        return jdbcTemplate.query(sql, rs -> {
            java.util.Map<String, Long> counts = new java.util.HashMap<>();

            while (rs.next()) {
                counts.put(
                        rs.getString("short_code"),
                        rs.getLong("total_clicks")
                );
            }

            return counts;
        }, shortCodes.toArray());
    }
}

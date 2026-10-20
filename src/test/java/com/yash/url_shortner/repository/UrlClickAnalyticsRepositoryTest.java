
package com.yash.url_shortner.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class UrlClickAnalyticsRepositoryTest {

    @Autowired
    private UrlClickAnalyticsRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void duplicateEventShouldIncrementClicksOnlyOnce() {
        String eventId = "test-event-12345";
        String shortCode = "test123";

        // Clean up data from a previous test run.
        jdbcTemplate.update(
                "DELETE FROM processed_click_events WHERE event_id = ?",
                eventId
        );
        jdbcTemplate.update(
                "DELETE FROM url_click_analytics WHERE short_code = ?",
                shortCode
        );

        // Process the same event twice.
        boolean firstResult =
                repository.processClickEvent(eventId, shortCode);

        boolean secondResult =
                repository.processClickEvent(eventId, shortCode);

        // First processing succeeds; duplicate is ignored.
        assertEquals(true, firstResult);
        assertEquals(false, secondResult);

        // The click count should be exactly one.
        Integer totalClicks = jdbcTemplate.queryForObject(
                "SELECT total_clicks FROM url_click_analytics WHERE short_code = ?",
                Integer.class,
                shortCode
        );

        assertEquals(1, totalClicks);
    }
}

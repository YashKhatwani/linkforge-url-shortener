package com.yash.url_shortner.event;

import java.time.Instant;

public record UrlClickEvent(
        String eventId,
        String shortCode,
        Instant clickedAt
) {}

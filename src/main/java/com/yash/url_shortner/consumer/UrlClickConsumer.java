
package com.yash.url_shortner.consumer;

import com.yash.url_shortner.event.UrlClickEvent;
import com.yash.url_shortner.repository.UrlClickAnalyticsRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UrlClickConsumer {

    private final UrlClickAnalyticsRepository analyticsRepository;

    public UrlClickConsumer(
            UrlClickAnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    @KafkaListener(topics = "url-click-events")
    public void consume(UrlClickEvent event) {
        boolean processed = analyticsRepository.processClickEvent(
                event.eventId(),
                event.shortCode()
        );

        if (processed) {
            System.out.println(
                    "Processed click event: " + event.eventId()
            );
        } else {
            System.out.println(
                    "Skipped duplicate event: " + event.eventId()
            );
        }
    }
}

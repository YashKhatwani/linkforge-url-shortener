package com.yash.url_shortner.producer;

import com.yash.url_shortner.event.UrlClickEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class UrlClickProducer {

    private static final String TOPIC = "url-click-events";

    private final KafkaTemplate<String, UrlClickEvent> kafkaTemplate;

    public UrlClickProducer(
            KafkaTemplate<String, UrlClickEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }


    public void publishClick(UrlClickEvent event) {
        try {
            kafkaTemplate.send("url-click-events", event.shortCode(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            System.err.println("Kafka send failed:");
                            ex.printStackTrace();
                        } else {
                            System.out.println(
                                    "Kafka event sent: topic="
                                            + result.getRecordMetadata().topic()
                                            + ", partition="
                                            + result.getRecordMetadata().partition()
                            );
                        }
                    });
        } catch (Exception ex) {
            System.err.println("Kafka producer initialization failed:");
            ex.printStackTrace();
        }
    }

}
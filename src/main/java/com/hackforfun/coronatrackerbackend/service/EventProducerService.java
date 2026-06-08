'package com.hackforfun.coronatrackerbackend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class EventProducerService {

    private static final Logger log = LoggerFactory.getLogger(EventProducerService.class);
    private static final String TOPIC = "corona-events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public EventProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishEvent(String eventType, String payload) {
        String message = String.format(
            "{\"type\":\"%s\",\"payload\":%s,\"source\":\"corona-backend\"}",
            eventType, payload);
        kafkaTemplate.send(TOPIC, message);
        log.info("Published event to {}: {}", TOPIC, message);
    }
}

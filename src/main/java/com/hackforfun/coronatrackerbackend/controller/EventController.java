package com.hackforfun.coronatrackerbackend.controller;

import com.hackforfun.coronatrackerbackend.service.EventProducerService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/coronatracker")
@CrossOrigin(origins = "*")
public class EventController {

    private final EventProducerService eventProducer;
    private final StringRedisTemplate redis;

    public EventController(EventProducerService eventProducer,
                           StringRedisTemplate redis) {
        this.eventProducer = eventProducer;
        this.redis = redis;
    }

    @PostMapping("/events/publish")
    public Map<String, Object> publishEvent(@RequestBody Map<String, String> body) {
        String type = body.getOrDefault("type", "case-reported");
        String payload = String.format(
            "{\"data\":\"%s\",\"timestamp\":\"%s\"}",
            body.getOrDefault("data", "test"), Instant.now());

        eventProducer.publishEvent(type, payload);

        redis.opsForValue().increment("corona-backend:events:published");
        redis.opsForList().leftPush("corona-backend:recent-events",
            type + ":" + Instant.now());
        redis.opsForList().trim("corona-backend:recent-events", 0, 99);

        return Map.of(
            "status", "published",
            "topic", "corona-events",
            "type", type,
            "timestamp", Instant.now().toString()
        );
    }

    @GetMapping("/events/stats")
    public Map<String, Object> getStats() {
        String published = redis.opsForValue()
            .get("corona-backend:events:published");
        return Map.of(
            "service", "corona-backend",
            "eventsPublished", published != null ? published : "0",
            "topic", "corona-events"
        );
    }
}

package com.ingoboka_api.v1.ussd.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "ingoboka.ussd.session-storage", havingValue = "redis", matchIfMissing = true)
public class RedisUssdSessionStore implements UssdSessionStore {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ingoboka.ussd.session-ttl-seconds:120}")
    private long sessionTtlSeconds;

    public RedisUssdSessionStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<UssdSession> find(String sessionId) {
        String json = redisTemplate.opsForValue().get(key(sessionId));
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, UssdSession.class));
        } catch (JsonProcessingException ex) {
            log.warn("Corrupt USSD session {}: {}", sessionId, ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void save(UssdSession session) {
        try {
            redisTemplate
                    .opsForValue()
                    .set(
                            key(session.getSessionId()),
                            objectMapper.writeValueAsString(session),
                            Duration.ofSeconds(sessionTtlSeconds));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize USSD session", ex);
        }
    }

    @Override
    public void delete(String sessionId) {
        redisTemplate.delete(key(sessionId));
    }

    private static String key(String sessionId) {
        return "ussd:session:" + sessionId;
    }
}

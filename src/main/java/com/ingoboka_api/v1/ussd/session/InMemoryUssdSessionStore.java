package com.ingoboka_api.v1.ussd.session;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ingoboka.ussd.session-storage", havingValue = "memory")
public class InMemoryUssdSessionStore implements UssdSessionStore {

    private final Map<String, UssdSession> sessions = new ConcurrentHashMap<>();

    @Override
    public Optional<UssdSession> find(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public void save(UssdSession session) {
        sessions.put(session.getSessionId(), session);
    }

    @Override
    public void delete(String sessionId) {
        sessions.remove(sessionId);
    }
}

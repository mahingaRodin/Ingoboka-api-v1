package com.ingoboka_api.v1.ussd.session;

import java.util.Optional;

public interface UssdSessionStore {

    Optional<UssdSession> find(String sessionId);

    void save(UssdSession session);

    void delete(String sessionId);
}

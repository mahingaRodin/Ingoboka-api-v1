package com.ingoboka_api.v1.identity.services;

import com.ingoboka_api.v1.common.enums.VerificationTokenType;

public interface NotificationService {
    void sendVerificationToken(String email, String token, VerificationTokenType type);
}
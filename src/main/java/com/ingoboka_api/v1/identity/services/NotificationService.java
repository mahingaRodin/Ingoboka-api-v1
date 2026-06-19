package com.ingoboka_api.v1.identity.services;

import com.ingoboka_api.v1.common.enums.VerificationTokenType;
import com.ingoboka_api.v1.identity.models.User;
import java.util.Map;

public interface NotificationService {

    void sendVerificationToken(String email, String token, VerificationTokenType type);

    void sendTemplatedEmail(String email, String templateName, Map<String, String> variables);

    void sendStaffWelcomeEmail(User user, String organizationName, String temporaryPassword);

    void sendOtpEmail(String email, String otp, int expirationMinutes);
}
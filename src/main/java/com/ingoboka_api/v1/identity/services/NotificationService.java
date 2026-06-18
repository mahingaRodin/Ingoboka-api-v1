package com.ingoboka_api.v1.identity.service;

import com.ingoboka_api.v1.common.enums.VerificationTokenType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;

    public void sendVerificationToken(String email, String token, VerificationTokenType type) {
        String subject = switch (type) {
            case EMAIL_VERIFICATION -> "Verify your Ingoboka email";
            case PASSWORD_RESET -> "Reset your Ingoboka password";
            case ACCOUNT_ACTIVATION -> "Activate your Ingoboka account";
        };

        String body = switch (type) {
            case EMAIL_VERIFICATION -> "Use this token to verify your email: " + token;
            case PASSWORD_RESET -> "Use this token to reset your password: " + token;
            case ACCOUNT_ACTIVATION -> "Use this token to activate your account and set a password: " + token;
        };

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Failed to send {} email to {}. Token logged for development.", type, email);
            log.info("DEV TOKEN [{}] for {}: {}", type, email, token);
        }
    }
}

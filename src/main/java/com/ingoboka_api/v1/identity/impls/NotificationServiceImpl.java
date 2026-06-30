package com.ingoboka_api.v1.identity.impls;

import com.ingoboka_api.v1.common.config.PlatformProperties;
import com.ingoboka_api.v1.common.config.SecurityProperties;
import com.ingoboka_api.v1.common.enums.VerificationTokenType;
import com.ingoboka_api.v1.identity.models.User;
import com.ingoboka_api.v1.identity.services.NotificationService;
import com.ingoboka_api.v1.messaging.services.EmailTemplateService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final JavaMailSender mailSender;
    private final EmailTemplateService emailTemplateService;
    private final PlatformProperties platformProperties;
    private final SecurityProperties securityProperties;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Override
    public void sendVerificationToken(String email, String token, VerificationTokenType type) {
        if (type == VerificationTokenType.EMAIL_VERIFICATION) {
            sendTemplatedEmail(
                    email,
                    "email-verification",
                    Map.of(
                            "fullName", email,
                            "token", token,
                            "verifyUrl", platformProperties.getFrontendVerifyEmailUrl() + "?token=" + token,
                            "hours", String.valueOf(securityProperties.getVerificationTokenExpirationHours())));
            return;
        }

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

        sendPlainEmail(email, subject, body, type.name(), token);
    }

    @Override
    public void sendTemplatedEmail(String email, String templateName, Map<String, String> variables) {
        EmailTemplateService.RenderedEmail rendered = emailTemplateService.render(templateName, variables);
        sendPlainEmail(email, rendered.subject(), rendered.body(), templateName, null);
    }

    @Override
    public void sendStaffWelcomeEmail(User user, String organizationName, String temporaryPassword) {
        sendTemplatedEmail(
                user.getEmail(),
                "staff-welcome",
                Map.of(
                        "fullName", user.getFirstName() + " " + user.getLastName(),
                        "organizationName", organizationName,
                        "email", user.getEmail(),
                        "temporaryPassword", temporaryPassword,
                        "loginUrl", platformProperties.getFrontendLoginUrl()));
    }

    @Override
    public void sendOtpEmail(String email, String otp, int expirationMinutes) {
        sendTemplatedEmail(
                email, "otp-verification", Map.of("otp", otp, "minutes", String.valueOf(expirationMinutes)));
    }

    private void sendPlainEmail(String email, String subject, String body, String context, String devToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (mailFrom != null && !mailFrom.isBlank()) {
                message.setFrom(mailFrom.trim());
            }
            message.setTo(email);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Failed to send {} email to {}: {}", context, email, ex.getMessage());
            if (devToken != null) {
                log.info("DEV TOKEN [{}] for {}: {}", context, email, devToken);
            } else {
                log.info("DEV EMAIL [{}] to {}: {}", context, email, body);
            }
        }
    }
}

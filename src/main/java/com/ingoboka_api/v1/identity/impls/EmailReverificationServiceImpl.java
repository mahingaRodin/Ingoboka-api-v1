package com.ingoboka_api.v1.identity.impls;

import com.ingoboka_api.v1.common.enums.UserStatus;
import com.ingoboka_api.v1.common.enums.VerificationTokenType;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.identity.models.User;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import com.ingoboka_api.v1.identity.repositories.VerificationTokenRepository;
import com.ingoboka_api.v1.identity.services.EmailReverificationService;
import com.ingoboka_api.v1.identity.services.NotificationService;
import com.ingoboka_api.v1.identity.services.OtpService;
import com.ingoboka_api.v1.common.config.SecurityProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailReverificationServiceImpl implements EmailReverificationService {

    public static final String OTP_PURPOSE_EMAIL_CHANGE = "EMAIL_CHANGE";

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final NotificationService notificationService;
    private final OtpService otpService;
    private final SecurityProperties securityProperties;

    @Value("${ingoboka.security.otp.expiration-minutes:10}")
    private int otpExpirationMinutes;

    @Override
    @Transactional
    public void applyEmailChange(User user, String newEmail) {
        String normalized = newEmail.trim().toLowerCase();
        if (normalized.equalsIgnoreCase(user.getEmail())) {
            return;
        }
        if (userRepository.existsByEmailIgnoreCase(normalized)) {
            throw new BusinessException(
                    "Email is already registered",
                    "EMAIL_ALREADY_REGISTERED",
                    java.util.Map.of("email", "Email is already registered"));
        }

        user.setEmail(normalized);
        user.setEmailVerified(false);
        user.setStatus(UserStatus.PENDING_EMAIL_VERIFICATION);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        sendEmailVerificationOtp(user);
        issueVerificationToken(user);
    }

    @Override
    @Transactional
    public void sendEmailVerificationOtp(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new BusinessException("No email address on file");
        }
        String otp = otpService.generateAndStore(OTP_PURPOSE_EMAIL_CHANGE, user.getEmail());
        notificationService.sendOtpEmail(user.getEmail(), otp, otpExpirationMinutes);
        log.debug("Email verification OTP issued for user {}", user.getId());
    }

    private void issueVerificationToken(User user) {
        String rawToken = UUID.randomUUID().toString();
        com.ingoboka_api.v1.identity.models.VerificationToken token =
                new com.ingoboka_api.v1.identity.models.VerificationToken();
        token.setId(UUID.randomUUID());
        token.setUser(user);
        token.setTokenHash(hashToken(rawToken));
        token.setType(VerificationTokenType.EMAIL_VERIFICATION);
        token.setExpiresAt(
                Instant.now().plusSeconds(securityProperties.getVerificationTokenExpirationHours() * 3600L));
        token.setCreatedAt(Instant.now());
        verificationTokenRepository.save(token);
        notificationService.sendVerificationToken(user.getEmail(), rawToken, VerificationTokenType.EMAIL_VERIFICATION);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}

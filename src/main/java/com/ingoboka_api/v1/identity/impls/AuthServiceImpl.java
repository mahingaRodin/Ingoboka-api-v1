package com.ingoboka_api.v1.identity.impls;

import com.ingoboka_api.v1.common.config.JwtProperties;
import com.ingoboka_api.v1.common.config.SecurityProperties;
import com.ingoboka_api.v1.common.enums.NotificationChannel;
import com.ingoboka_api.v1.common.enums.UserStatus;
import com.ingoboka_api.v1.common.enums.VerificationTokenType;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.requests.*;
import com.ingoboka_api.v1.common.responses.AuthTokensResponse;
import com.ingoboka_api.v1.common.security.IngobokaUserDetails;
import com.ingoboka_api.v1.common.security.JwtService;
import com.ingoboka_api.v1.identity.models.*;
import com.ingoboka_api.v1.identity.repositories.RefreshTokenRepository;
import com.ingoboka_api.v1.identity.repositories.RoleRepository;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import com.ingoboka_api.v1.identity.repositories.VerificationTokenRepository;
import com.ingoboka_api.v1.identity.services.AuthService;
import com.ingoboka_api.v1.identity.services.NotificationService;
import com.ingoboka_api.v1.identity.services.OtpService;
import com.ingoboka_api.v1.messaging.services.NotificationTemplateService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final SecurityProperties securityProperties;
    private final NotificationService notificationService;
    private final OtpService otpService;
    private final NotificationTemplateService notificationTemplateService;

    private static final String OTP_PURPOSE_SIGNUP = "SIGNUP";

    @Value("${ingoboka.security.otp.expiration-minutes:10}")
    private int otpExpirationMinutes;

    @Override
    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BusinessException("Email is already registered");
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BusinessException("Phone number is already registered");
        }

        Role citizenRole = roleRepository
                .findByCode(RoleCodes.CITIZEN)
                .orElseThrow(() -> new BusinessException("Citizen role is not configured"));

        Instant now = Instant.now();
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.PENDING_EMAIL_VERIFICATION);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.getRoles().add(citizenRole);

        userRepository.save(user);
        sendSignupOtp(user);
    }

    private void sendSignupOtp(User user) {
        String otp = otpService.generateAndStore(OTP_PURPOSE_SIGNUP, user.getPhoneNumber());
        notificationTemplateService.sendTemplated(
                user.getId(),
                null,
                "OTP_VERIFICATION",
                NotificationChannel.EMAIL,
                user.getEmail(),
                Map.of("otp", otp, "minutes", String.valueOf(otpExpirationMinutes)));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthTokensResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.getEmail().trim().toLowerCase(), request.getPassword()));

        User user = userRepository
                .findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new BusinessException("Invalid credentials"));

        if (user.getStatus() == UserStatus.PENDING_EMAIL_VERIFICATION || !user.isPhoneVerified()) {
            throw new BusinessException("Please verify your account with OTP before logging in");
        }
        if (user.getStatus() == UserStatus.PENDING_ACTIVATION) {
            throw new BusinessException("Please activate your account before logging in");
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException("Account is disabled");
        }

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public void requestEmailVerification(EmailRequest request) {
        userRepository.findByEmailIgnoreCase(request.getEmail()).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                issueVerificationToken(user, VerificationTokenType.EMAIL_VERIFICATION);
            }
        });
    }

    @Override
    @Transactional
    public void confirmEmailVerification(VerifyEmailConfirmRequest request) {
        VerificationToken token = loadActiveToken(request.getToken(), VerificationTokenType.EMAIL_VERIFICATION);
        User user = token.getUser();
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setUpdatedAt(Instant.now());
        consumeToken(token);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {
        otpService.verify(OTP_PURPOSE_SIGNUP, request.getPhoneNumber(), request.getOtp());
        User user = userRepository
                .findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new BusinessException("Account not found"));
        user.setPhoneVerified(true);
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void resendOtp(ResendOtpRequest request) {
        userRepository.findByPhoneNumber(request.getPhoneNumber()).ifPresent(user -> {
            if (user.getStatus() == UserStatus.PENDING_EMAIL_VERIFICATION) {
                sendSignupOtp(user);
            }
        });
    }

    @Override
    @Transactional
    public AuthTokensResponse refresh(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository
                .findByTokenHashAndRevokedFalse(hashToken(request.getRefreshToken()))
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Refresh token expired");
        }
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        return buildAuthResponse(stored.getUser());
    }

    @Override
    @Transactional
    public void requestPasswordReset(EmailRequest request) {
        userRepository.findByEmailIgnoreCase(request.getEmail()).ifPresent(user -> {
            if (user.getStatus() == UserStatus.ACTIVE) {
                issueVerificationToken(user, VerificationTokenType.PASSWORD_RESET);
            }
        });
    }

    @Override
    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        VerificationToken token = loadActiveToken(request.getToken(), VerificationTokenType.PASSWORD_RESET);
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(Instant.now());
        consumeToken(token);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void activateAccount(ActivateAccountRequest request) {
        VerificationToken token = loadActiveToken(request.getToken(), VerificationTokenType.ACCOUNT_ACTIVATION);
        User user = token.getUser();
        if (user.getStatus() != UserStatus.PENDING_ACTIVATION) {
            throw new BusinessException("Account is not pending activation");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setUpdatedAt(Instant.now());
        consumeToken(token);
        userRepository.save(user);
    }

    private AuthTokensResponse buildAuthResponse(User user) {
        IngobokaUserDetails userDetails = new IngobokaUserDetails(user);
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = createRefreshToken(user);

        return AuthTokensResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInMinutes(jwtProperties.getAccessTokenExpirationMinutes())
                .user(AuthTokensResponse.UserSummaryResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .status(user.getStatus().name())
                        .roles(user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet()))
                        .organizationId(
                                user.getOrganization() != null ? user.getOrganization().getId() : null)
                        .build())
                .build();
    }

    private String createRefreshToken(User user) {
        String rawToken = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setExpiresAt(Instant.now().plusSeconds(jwtProperties.getRefreshTokenExpirationDays() * 86400L));
        refreshToken.setCreatedAt(Instant.now());
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    private void issueVerificationToken(User user, VerificationTokenType type) {
        String rawToken = UUID.randomUUID().toString();
        VerificationToken token = new VerificationToken();
        token.setId(UUID.randomUUID());
        token.setUser(user);
        token.setTokenHash(hashToken(rawToken));
        token.setType(type);
        token.setExpiresAt(Instant.now().plusSeconds(expirationHours(type) * 3600L));
        token.setCreatedAt(Instant.now());
        verificationTokenRepository.save(token);
        notificationService.sendVerificationToken(user.getEmail(), rawToken, type);
    }

    private long expirationHours(VerificationTokenType type) {
        return switch (type) {
            case EMAIL_VERIFICATION -> securityProperties.getVerificationTokenExpirationHours();
            case PASSWORD_RESET -> securityProperties.getPasswordResetTokenExpirationHours();
            case ACCOUNT_ACTIVATION -> securityProperties.getActivationTokenExpirationHours();
        };
    }

    private VerificationToken loadActiveToken(String rawToken, VerificationTokenType type) {
        VerificationToken token = verificationTokenRepository
                .findByTokenHashAndTypeAndConsumedAtIsNull(hashToken(rawToken), type)
                .orElseThrow(() -> new BusinessException("Invalid or expired token"));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Invalid or expired token");
        }
        return token;
    }

    private void consumeToken(VerificationToken token) {
        token.setConsumedAt(Instant.now());
        verificationTokenRepository.save(token);
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

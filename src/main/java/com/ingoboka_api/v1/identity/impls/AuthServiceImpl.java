package com.ingoboka_api.v1.identity.impls;

import com.ingoboka_api.v1.common.config.OtpDeliveryProperties;
import com.ingoboka_api.v1.common.enums.OtpDeliveryChannel;
import com.ingoboka_api.v1.common.config.JwtProperties;
import com.ingoboka_api.v1.common.config.SecurityProperties;
import com.ingoboka_api.v1.common.enums.ConsentType;
import com.ingoboka_api.v1.common.enums.KycStatus;
import com.ingoboka_api.v1.common.enums.NotificationChannel;
import com.ingoboka_api.v1.common.enums.UserStatus;
import com.ingoboka_api.v1.common.enums.VerificationTokenType;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.requests.*;
import com.ingoboka_api.v1.common.responses.AuthTokensResponse;
import com.ingoboka_api.v1.common.security.IngobokaUserDetails;
import com.ingoboka_api.v1.common.security.JwtService;
import com.ingoboka_api.v1.common.util.HashUtils;
import com.ingoboka_api.v1.customer.models.CitizenProfile;
import com.ingoboka_api.v1.customer.repositories.CitizenProfileRepository;
import com.ingoboka_api.v1.customer.repositories.ConsentRepository;
import com.ingoboka_api.v1.identity.repositories.RefreshTokenRepository;
import com.ingoboka_api.v1.identity.repositories.RoleRepository;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import com.ingoboka_api.v1.identity.repositories.VerificationTokenRepository;
import com.ingoboka_api.v1.identity.services.AuthService;
import com.ingoboka_api.v1.identity.services.NotificationService;
import com.ingoboka_api.v1.identity.services.OtpService;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.identity.models.RefreshToken;
import com.ingoboka_api.v1.identity.models.Role;
import com.ingoboka_api.v1.identity.models.RoleCodes;
import com.ingoboka_api.v1.identity.models.User;
import com.ingoboka_api.v1.identity.models.VerificationToken;
import com.ingoboka_api.v1.messaging.services.NotificationTemplateService;
import com.ingoboka_api.v1.messaging.services.SmsDeliveryService;
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
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
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
    private final CitizenProfileRepository citizenProfileRepository;
    private final ConsentRepository consentRepository;
    private final SmsDeliveryService smsDeliveryService;
    private final OtpDeliveryProperties otpDeliveryProperties;

    private static final String OTP_PURPOSE_SIGNUP = "SIGNUP";
    private static final String SYNTHETIC_EMAIL_DOMAIN = "@phone.ingoboka.rw";

    @Value("${ingoboka.security.otp.expiration-minutes:10}")
    private int otpExpirationMinutes;

    @Override
    @Transactional
    public void signup(SignupRequest request) {
        throw new BusinessException(
                "Staff and partner accounts are provisioned by administrators. Citizens must use POST /api/v1/auth/register");
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        String phone = normalizePhone(request.getPhone());
        if (userRepository.existsByPhoneNumber(phone)) {
            throw new BusinessException("Phone number is already registered");
        }

        String email = resolveEmail(request.getEmail(), phone);
        if (otpDeliveryProperties.getDeliveryChannel() == OtpDeliveryChannel.EMAIL
                && !StringUtils.hasText(request.getEmail())) {
            throw new BusinessException(
                    "Email is required for verification while SMS is unavailable. Provide a real email address.");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Email is already registered");
        }

        String nationalIdHash = HashUtils.sha256(request.getNationalId());
        if (nationalIdHash != null && citizenProfileRepository.existsByNationalIdHash(nationalIdHash)) {
            throw new BusinessException("National ID is already registered");
        }

        String[] nameParts = splitFullName(request.getFullName());
        Role citizenRole = roleRepository
                .findByCode(RoleCodes.CITIZEN)
                .orElseThrow(() -> new BusinessException("Citizen role is not configured"));

        Instant now = Instant.now();
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setFirstName(nameParts[0]);
        user.setLastName(nameParts[1]);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.PENDING_EMAIL_VERIFICATION);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.getRoles().add(citizenRole);
        userRepository.save(user);

        CitizenProfile profile = new CitizenProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(user.getId());
        profile.setNationalIdHash(nationalIdHash);
        profile.setDateOfBirth(LocalDate.now().minusYears(25));
        profile.setCountry("RW");
        profile.setPreferredLanguage("en");
        profile.setKycStatus(KycStatus.PENDING);
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        citizenProfileRepository.save(profile);

        sendSignupOtp(user);
    }

    private String normalizePhone(String phone) {
        return phone != null ? phone.trim() : "";
    }

    private String resolveEmail(String email, String phone) {
        if (email != null && !email.isBlank()) {
            return email.trim().toLowerCase();
        }
        String digits = phone.replaceAll("\\D", "");
        return digits + "@phone.ingoboka.rw";
    }

    private String[] splitFullName(String fullName) {
        String trimmed = fullName.trim();
        int space = trimmed.indexOf(' ');
        if (space < 0) {
            return new String[] {trimmed, trimmed};
        }
        return new String[] {trimmed.substring(0, space), trimmed.substring(space + 1).trim()};
    }

    private void sendSignupOtp(User user) {
        String otp = otpService.generateAndStore(OTP_PURPOSE_SIGNUP, user.getPhoneNumber());
        Map<String, String> variables =
                Map.of("otp", otp, "minutes", String.valueOf(otpExpirationMinutes));

        switch (otpDeliveryProperties.getDeliveryChannel()) {
            case EMAIL -> notificationService.sendOtpEmail(user.getEmail(), otp, otpExpirationMinutes);
            case SMS -> {
                String message = "Your Ingoboka verification code is "
                        + otp
                        + ". Valid for "
                        + otpExpirationMinutes
                        + " minutes.";
                smsDeliveryService.send(user.getPhoneNumber(), message);
                notificationTemplateService.sendTemplated(
                        user.getId(),
                        null,
                        "OTP_VERIFICATION",
                        NotificationChannel.SMS,
                        user.getPhoneNumber(),
                        variables);
            }
            case LOG -> log.info(
                    "DEV OTP for phone {} (email {}): {}",
                    user.getPhoneNumber(),
                    user.getEmail(),
                    otp);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AuthTokensResponse login(LoginRequest request) {
        String identifier = request.resolvedIdentifier();
        String principal = identifier.contains("@") ? identifier.toLowerCase() : identifier;
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(principal, request.getPassword()));

        User user = identifier.contains("@")
                ? userRepository
                        .findByEmailIgnoreCase(identifier)
                        .orElseThrow(() -> new BusinessException("Invalid credentials"))
                : userRepository
                        .findByPhoneNumber(identifier)
                        .orElseThrow(() -> new BusinessException("Invalid credentials"));

        boolean isCitizen = user.getRoles().stream().anyMatch(role -> RoleCodes.CITIZEN.equals(role.getCode()));

        if (isCitizen) {
            if (user.getStatus() == UserStatus.PENDING_EMAIL_VERIFICATION || !user.isPhoneVerified()) {
                throw new BusinessException("Please verify your account with OTP before logging in");
            }
        } else {
            if (user.getStatus() == UserStatus.PENDING_ACTIVATION) {
                throw new BusinessException("Please activate your account before logging in");
            }
        }

        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException("Account is disabled");
        }
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new BusinessException("Account is locked");
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
    public AuthTokensResponse verifyOtp(VerifyOtpRequest request) {
        otpService.verify(OTP_PURPOSE_SIGNUP, request.getPhoneNumber(), request.getOtp());
        User user = userRepository
                .findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new BusinessException("Account not found"));
        user.setPhoneVerified(true);
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        return buildAuthResponse(user);
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

    @Transactional
    @Override
    public void logout(LogoutRequest request) {
        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            return;
        }
        refreshTokenRepository
                .findByTokenHashAndRevokedFalse(hashToken(request.getRefreshToken()))
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
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

    @Override
    @Transactional
    public AuthTokensResponse changePassword(ChangePasswordRequest request) {
        User user = userRepository
                .findWithDetailsById(SecurityUtils.currentUser().getUserId())
                .orElseThrow(() -> new BusinessException("User not found"));

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        user.setStatus(UserStatus.PENDING_EMAIL_VERIFICATION);
        user.setEmailVerified(false);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        notificationService.sendTemplatedEmail(
                user.getEmail(),
                "password-changed",
                Map.of("fullName", user.getFirstName() + " " + user.getLastName()));
        issueVerificationToken(user, VerificationTokenType.EMAIL_VERIFICATION);

        return buildAuthResponse(user);
    }

    private AuthTokensResponse buildAuthResponse(User user) {
        IngobokaUserDetails userDetails = new IngobokaUserDetails(user);
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = createRefreshToken(user);
        long expiresMinutes = jwtProperties.getAccessTokenExpirationMinutes();
        String primaryRole = user.getRoles().stream().findFirst().map(Role::getCode).orElse(RoleCodes.CITIZEN);
        boolean consentGiven = consentRepository
                .findByUserIdAndConsentTypeAndGrantedTrueAndRevokedAtIsNull(
                        user.getId(), ConsentType.DATA_PROCESSING)
                .isPresent();
        boolean isCitizen = user.getRoles().stream().anyMatch(role -> RoleCodes.CITIZEN.equals(role.getCode()));
        boolean requiresEmailVerification = !isCitizen && !user.isEmailVerified();
        boolean accountActive = user.getStatus() == UserStatus.ACTIVE
                && !user.isMustChangePassword()
                && (isCitizen ? user.isPhoneVerified() : user.isEmailVerified());

        return AuthTokensResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInMinutes(expiresMinutes)
                .expiresIn(expiresMinutes * 60)
                .user(AuthTokensResponse.UserSummaryResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .fullName(user.getFirstName() + " " + user.getLastName())
                        .phone(user.getPhoneNumber())
                        .status(user.getStatus().name())
                        .roles(user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet()))
                        .role(primaryRole)
                        .organizationId(
                                user.getOrganization() != null ? user.getOrganization().getId() : null)
                        .verified(isCitizen ? user.isPhoneVerified() : user.isEmailVerified())
                        .consentGiven(consentGiven)
                        .mustChangePassword(user.isMustChangePassword())
                        .emailVerified(user.isEmailVerified())
                        .requiresEmailVerification(requiresEmailVerification)
                        .accountActive(accountActive)
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

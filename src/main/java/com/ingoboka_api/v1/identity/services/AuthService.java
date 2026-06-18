package com.ingoboka_api.v1.identity.service;

import com.ingoboka_api.v1.common.config.JwtProperties;
import com.ingoboka_api.v1.common.config.SecurityProperties;
import com.ingoboka_api.v1.common.enums.UserStatus;
import com.ingoboka_api.v1.common.enums.VerificationTokenType;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.security.IngobokaUserDetails;
import com.ingoboka_api.v1.common.security.JwtService;
import com.ingoboka_api.v1.identity.domain.RefreshToken;
import com.ingoboka_api.v1.identity.domain.Role;
import com.ingoboka_api.v1.identity.domain.RoleCodes;
import com.ingoboka_api.v1.identity.domain.User;
import com.ingoboka_api.v1.identity.domain.VerificationToken;
import com.ingoboka_api.v1.identity.dto.ActivateAccountRequest;
import com.ingoboka_api.v1.identity.dto.AuthTokensResponse;
import com.ingoboka_api.v1.identity.dto.EmailRequest;
import com.ingoboka_api.v1.identity.dto.LoginRequest;
import com.ingoboka_api.v1.identity.dto.PasswordResetRequest;
import com.ingoboka_api.v1.identity.dto.SignupRequest;
import com.ingoboka_api.v1.identity.dto.VerifyEmailConfirmRequest;
import com.ingoboka_api.v1.identity.repository.RefreshTokenRepository;
import com.ingoboka_api.v1.identity.repository.RoleRepository;
import com.ingoboka_api.v1.identity.repository.UserRepository;
import com.ingoboka_api.v1.identity.repository.VerificationTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

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
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.getRoles().add(citizenRole);

        userRepository.save(user);
        issueVerificationToken(user, VerificationTokenType.EMAIL_VERIFICATION);
    }

    @Transactional(readOnly = true)
    public AuthTokensResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.getEmail().trim().toLowerCase(), request.getPassword()));

        User user = userRepository
                .findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new BusinessException("Invalid credentials"));

        if (user.getStatus() == UserStatus.PENDING_EMAIL_VERIFICATION) {
            throw new BusinessException("Please verify your email before logging in");
        }
        if (user.getStatus() == UserStatus.PENDING_ACTIVATION) {
            throw new BusinessException("Please activate your account before logging in");
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException("Account is disabled");
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public void requestEmailVerification(EmailRequest request) {
        userRepository.findByEmailIgnoreCase(request.getEmail()).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                issueVerificationToken(user, VerificationTokenType.EMAIL_VERIFICATION);
            }
        });
    }

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

    @Transactional
    public void requestPasswordReset(EmailRequest request) {
        userRepository.findByEmailIgnoreCase(request.getEmail()).ifPresent(user -> {
            if (user.getStatus() == UserStatus.ACTIVE) {
                issueVerificationToken(user, VerificationTokenType.PASSWORD_RESET);
            }
        });
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        VerificationToken token = loadActiveToken(request.getToken(), VerificationTokenType.PASSWORD_RESET);
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(Instant.now());
        consumeToken(token);
        userRepository.save(user);
    }

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

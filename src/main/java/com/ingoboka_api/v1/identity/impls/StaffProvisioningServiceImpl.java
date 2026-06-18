package com.ingoboka_api.v1.identity.impls;

import com.ingoboka_api.v1.common.config.SecurityProperties;
import com.ingoboka_api.v1.common.enums.RoleScope;
import com.ingoboka_api.v1.common.enums.UserStatus;
import com.ingoboka_api.v1.common.enums.VerificationTokenType;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.responses.StaffCreatedResponse;
import com.ingoboka_api.v1.identity.models.Organization;
import com.ingoboka_api.v1.identity.models.Role;
import com.ingoboka_api.v1.identity.models.User;
import com.ingoboka_api.v1.identity.models.VerificationToken;
import com.ingoboka_api.v1.identity.repositories.OrganizationRepository;
import com.ingoboka_api.v1.identity.repositories.RoleRepository;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import com.ingoboka_api.v1.identity.repositories.VerificationTokenRepository;
import com.ingoboka_api.v1.identity.services.NotificationService;
import com.ingoboka_api.v1.identity.services.StaffProvisioningService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffProvisioningServiceImpl implements StaffProvisioningService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final NotificationService notificationService;
    private final SecurityProperties securityProperties;

    @Override
    @Transactional
    public StaffCreatedResponse createStaffMember(
            UUID organizationId,
            String email,
            String phoneNumber,
            String firstName,
            String lastName,
            String roleCode) {

        Organization organization = organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new BusinessException("Organization not found"));

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Email is already registered");
        }

        Role role = roleRepository
                .findByCode(roleCode)
                .orElseThrow(() -> new BusinessException("Role not found: " + roleCode));

        if (role.getScope() != RoleScope.TENANT) {
            throw new BusinessException("Only tenant-scoped roles can be assigned to staff");
        }

        Instant now = Instant.now();
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setOrganization(organization);
        user.setEmail(email.trim().toLowerCase());
        user.setPhoneNumber(phoneNumber);
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setPasswordHash(null);
        user.setStatus(UserStatus.PENDING_ACTIVATION);
        user.setEmailVerified(false);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.getRoles().add(role);
        userRepository.save(user);

        String rawToken = issueActivationToken(user);

        return StaffCreatedResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .roleCode(roleCode)
                .organizationId(organizationId)
                .activationRequired(true)
                .build();
    }

    private String issueActivationToken(User user) {
        String rawToken = UUID.randomUUID().toString();
        VerificationToken token = new VerificationToken();
        token.setId(UUID.randomUUID());
        token.setUser(user);
        token.setTokenHash(hashToken(rawToken));
        token.setType(VerificationTokenType.ACCOUNT_ACTIVATION);
        token.setExpiresAt(Instant.now().plusSeconds(securityProperties.getActivationTokenExpirationHours() * 3600L));
        token.setCreatedAt(Instant.now());
        verificationTokenRepository.save(token);
        notificationService.sendVerificationToken(user.getEmail(), rawToken, VerificationTokenType.ACCOUNT_ACTIVATION);
        return rawToken;
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

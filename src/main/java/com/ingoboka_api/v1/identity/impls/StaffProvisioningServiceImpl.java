package com.ingoboka_api.v1.identity.impls;

import com.ingoboka_api.v1.common.config.SecurityProperties;
import com.ingoboka_api.v1.common.enums.RoleScope;
import com.ingoboka_api.v1.common.enums.UserStatus;
import com.ingoboka_api.v1.common.enums.VerificationTokenType;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.responses.StaffCreatedResponse;
import com.ingoboka_api.v1.common.util.HashUtils;
import com.ingoboka_api.v1.common.util.TemporaryPasswordGenerator;
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
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class StaffProvisioningServiceImpl implements StaffProvisioningService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenRepository verificationTokenRepository;
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
        return createStaffMemberWithDefaultPassword(
                organizationId, email, phoneNumber, firstName, lastName, roleCode, null);
    }

    @Override
    @Transactional
    public StaffCreatedResponse createStaffMemberWithDefaultPassword(
            UUID organizationId,
            String email,
            String phoneNumber,
            String firstName,
            String lastName,
            String roleCode,
            String defaultPassword) {

        Organization organization = organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new BusinessException("Organization not found"));

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Email is already registered");
        }

        Role role = requireStaffRole(roleCode);

        String temporaryPassword =
                StringUtils.hasText(defaultPassword) ? defaultPassword : TemporaryPasswordGenerator.generate(12);

        Instant now = Instant.now();
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setOrganization(organization);
        user.setEmail(email.trim().toLowerCase());
        user.setPhoneNumber(phoneNumber);
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setStatus(UserStatus.PENDING_PASSWORD_CHANGE);
        user.setMustChangePassword(true);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.getRoles().add(role);
        userRepository.save(user);

        notificationService.sendStaffWelcomeEmail(user, organization.getName(), temporaryPassword);

        return StaffCreatedResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .roleCode(roleCode)
                .organizationId(organizationId)
                .activationRequired(false)
                .mustChangePassword(true)
                .inviteSent(false)
                .enrollmentStatus("PENDING")
                .build();
    }

    @Override
    @Transactional
    public StaffCreatedResponse createStaffMemberWithInvite(
            UUID organizationId,
            String email,
            String phoneNumber,
            String firstName,
            String lastName,
            String roleCode,
            String inviterName) {

        Organization organization = organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new BusinessException("Organization not found"));

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Email is already registered");
        }

        Role role = requireStaffRole(roleCode);

        Instant now = Instant.now();
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setOrganization(organization);
        user.setEmail(email.trim().toLowerCase());
        user.setPhoneNumber(phoneNumber);
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setStatus(UserStatus.PENDING_ACTIVATION);
        user.setMustChangePassword(false);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.getRoles().add(role);
        userRepository.save(user);

        String activationToken = issueActivationToken(user);
        notificationService.sendStaffInviteEmail(user, organization.getName(), inviterName, activationToken);

        return StaffCreatedResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .roleCode(roleCode)
                .organizationId(organizationId)
                .activationRequired(true)
                .mustChangePassword(false)
                .inviteSent(true)
                .enrollmentStatus("PENDING")
                .build();
    }

    @Override
    public String issueActivationToken(User user) {
        String rawToken = UUID.randomUUID().toString();
        VerificationToken token = new VerificationToken();
        token.setId(UUID.randomUUID());
        token.setUser(user);
        token.setTokenHash(HashUtils.sha256(rawToken));
        token.setType(VerificationTokenType.ACCOUNT_ACTIVATION);
        token.setExpiresAt(
                Instant.now().plusSeconds(securityProperties.getActivationTokenExpirationHours() * 3600L));
        token.setCreatedAt(Instant.now());
        verificationTokenRepository.save(token);
        return rawToken;
    }

    private Role requireStaffRole(String roleCode) {
        Role role = roleRepository
                .findByCode(roleCode)
                .orElseThrow(() -> new BusinessException("Role not found: " + roleCode));

        if (role.getScope() == RoleScope.CUSTOMER) {
            throw new BusinessException("Citizen roles cannot be provisioned as staff");
        }
        return role;
    }
}

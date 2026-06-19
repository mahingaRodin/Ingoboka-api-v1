package com.ingoboka_api.v1.identity.impls;

import com.ingoboka_api.v1.common.enums.RoleScope;
import com.ingoboka_api.v1.common.enums.UserStatus;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.responses.StaffCreatedResponse;
import com.ingoboka_api.v1.common.util.TemporaryPasswordGenerator;
import com.ingoboka_api.v1.identity.models.Organization;
import com.ingoboka_api.v1.identity.models.Role;
import com.ingoboka_api.v1.identity.models.User;
import com.ingoboka_api.v1.identity.repositories.OrganizationRepository;
import com.ingoboka_api.v1.identity.repositories.RoleRepository;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
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

        Role role = roleRepository
                .findByCode(roleCode)
                .orElseThrow(() -> new BusinessException("Role not found: " + roleCode));

        if (role.getScope() == RoleScope.CUSTOMER) {
            throw new BusinessException("Citizen roles cannot be provisioned as staff");
        }

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
                .build();
    }
}

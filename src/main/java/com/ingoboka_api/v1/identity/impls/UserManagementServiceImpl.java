package com.ingoboka_api.v1.identity.impls;

import com.ingoboka_api.v1.common.enums.RoleScope;
import com.ingoboka_api.v1.common.enums.UserStatus;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.requests.CreateManagedUserRequest;
import com.ingoboka_api.v1.common.requests.ResetManagedUserPasswordRequest;
import com.ingoboka_api.v1.common.requests.UpdateManagedUserRequest;
import com.ingoboka_api.v1.common.requests.UpdateManagedUserRolesRequest;
import com.ingoboka_api.v1.common.requests.UpdateStaffStatusRequest;
import com.ingoboka_api.v1.common.responses.ManagedUserResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.StaffCreatedResponse;
import com.ingoboka_api.v1.common.security.IngobokaUserDetails;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.common.util.PaginationUtils;
import com.ingoboka_api.v1.common.util.TemporaryPasswordGenerator;
import com.ingoboka_api.v1.identity.models.Organization;
import com.ingoboka_api.v1.identity.models.Role;
import com.ingoboka_api.v1.identity.models.RoleCodes;
import com.ingoboka_api.v1.identity.models.User;
import com.ingoboka_api.v1.identity.repositories.OrganizationRepository;
import com.ingoboka_api.v1.identity.repositories.RoleRepository;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import com.ingoboka_api.v1.identity.services.NotificationService;
import com.ingoboka_api.v1.identity.services.StaffProvisioningService;
import com.ingoboka_api.v1.identity.services.UserManagementService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private static final Set<UserStatus> ALLOWED_STATUS_UPDATES =
            Set.of(UserStatus.ACTIVE, UserStatus.DISABLED, UserStatus.LOCKED, UserStatus.PENDING_PASSWORD_CHANGE);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final StaffProvisioningService staffProvisioningService;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ManagedUserResponse> listUsers(UUID organizationId, UserStatus status, int page, int size) {
        assertPlatformAdmin();
        Page<User> result = organizationId == null
                ? userRepository.findAllByOrderByCreatedAtDesc(PaginationUtils.toPageable(page, size))
                : userRepository.findByOrganizationIdOrderByCreatedAtDesc(
                        organizationId, PaginationUtils.toPageable(page, size));
        Page<ManagedUserResponse> mapped = result.map(this::toResponse);
        if (status == null) {
            return PageResponse.from(mapped);
        }
        List<ManagedUserResponse> filtered = mapped.getContent().stream()
                .filter(user -> user.getStatus() == status)
                .toList();
        return PageResponse.<ManagedUserResponse>builder()
                .content(filtered)
                .page(mapped.getNumber())
                .size(mapped.getSize())
                .totalElements(filtered.size())
                .totalPages(mapped.getTotalPages())
                .first(mapped.isFirst())
                .last(mapped.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ManagedUserResponse getUser(UUID userId) {
        assertPlatformAdmin();
        return toResponse(requireUser(userId));
    }

    @Override
    @Transactional
    public ManagedUserResponse createUser(CreateManagedUserRequest request) {
        assertPlatformAdmin();
        rejectCitizenRole(request.getRoleCode());

        Role role = requireRole(request.getRoleCode());
        UUID organizationId = resolveOrganizationId(role, request.getOrganizationId());

        StaffCreatedResponse created = staffProvisioningService.createStaffMemberWithDefaultPassword(
                organizationId,
                request.getEmail(),
                request.getPhoneNumber(),
                request.getFirstName(),
                request.getLastName(),
                request.getRoleCode(),
                request.getDefaultPassword());

        return getUser(created.getUserId());
    }

    @Override
    @Transactional
    public ManagedUserResponse updateUser(UUID userId, UpdateManagedUserRequest request) {
        assertPlatformAdmin();
        User user = requireUser(userId);
        rejectCitizenAccount(user);

        if (request.getEmail() != null
                && !request.getEmail().equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BusinessException("Email is already registered");
        }

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(request.getEmail().trim().toLowerCase());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        user.setUpdatedAt(Instant.now());
        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public ManagedUserResponse updateUserRoles(UUID userId, UpdateManagedUserRolesRequest request) {
        assertPlatformAdmin();
        User user = requireUser(userId);
        rejectCitizenAccount(user);
        rejectCitizenRole(request.getRoleCode());

        Role role = requireRole(request.getRoleCode());
        user.getRoles().clear();
        user.getRoles().add(role);
        user.setUpdatedAt(Instant.now());
        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public ManagedUserResponse updateUserStatus(UUID userId, UpdateStaffStatusRequest request) {
        assertPlatformAdmin();
        if (!ALLOWED_STATUS_UPDATES.contains(request.getStatus())) {
            throw new BusinessException("Invalid user status");
        }
        User user = requireUser(userId);
        rejectCitizenAccount(user);
        user.setStatus(request.getStatus());
        user.setUpdatedAt(Instant.now());
        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public ManagedUserResponse resetUserPassword(UUID userId, ResetManagedUserPasswordRequest request) {
        assertPlatformAdmin();
        User user = requireUser(userId);
        rejectCitizenAccount(user);

        String temporaryPassword = StringUtils.hasText(request.getDefaultPassword())
                ? request.getDefaultPassword()
                : TemporaryPasswordGenerator.generate(12);

        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        user.setStatus(UserStatus.PENDING_PASSWORD_CHANGE);
        user.setEmailVerified(false);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        String organizationName = user.getOrganization() != null ? user.getOrganization().getName() : "Ingoboka";
        notificationService.sendStaffWelcomeEmail(user, organizationName, temporaryPassword);
        return toResponse(user);
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        assertPlatformAdmin();
        User user = requireUser(userId);
        rejectCitizenAccount(user);
        user.setStatus(UserStatus.DISABLED);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    private User requireUser(UUID userId) {
        return userRepository
                .findWithDetailsById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
    }

    private Role requireRole(String roleCode) {
        return roleRepository
                .findByCode(roleCode)
                .orElseThrow(() -> new BusinessException("Role not found: " + roleCode));
    }

    private UUID resolveOrganizationId(Role role, UUID organizationId) {
        if (role.getScope() == RoleScope.PLATFORM) {
            Organization platformOrg = organizationRepository
                    .findByCode("INGOBOKA_PLATFORM")
                    .orElseThrow(() -> new BusinessException("Platform organization is not configured"));
            return platformOrg.getId();
        }
        if (organizationId == null) {
            throw new BusinessException("Organization is required for tenant roles");
        }
        organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new BusinessException("Organization not found"));
        return organizationId;
    }

    private void assertPlatformAdmin() {
        IngobokaUserDetails user = SecurityUtils.currentUser();
        if (!user.hasRole(RoleCodes.PLATFORM_ADMIN)) {
            throw new BusinessException("Only platform administrators can manage users globally");
        }
    }

    private void rejectCitizenRole(String roleCode) {
        if (RoleCodes.CITIZEN.equals(roleCode) || RoleCodes.BENEFICIARY.equals(roleCode)) {
            throw new BusinessException("Citizens must self-register via /api/v1/auth/register");
        }
    }

    private void rejectCitizenAccount(User user) {
        if (user.getRoles().stream().anyMatch(role -> RoleCodes.CITIZEN.equals(role.getCode()))) {
            throw new BusinessException("Citizen accounts are managed through the customer profile flow");
        }
    }

    private ManagedUserResponse toResponse(User user) {
        return ManagedUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .status(user.getStatus())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .mustChangePassword(user.isMustChangePassword())
                .organizationId(user.getOrganization() != null ? user.getOrganization().getId() : null)
                .organizationName(user.getOrganization() != null ? user.getOrganization().getName() : null)
                .roles(user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

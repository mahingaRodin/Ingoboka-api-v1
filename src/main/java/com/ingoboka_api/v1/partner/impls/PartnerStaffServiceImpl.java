package com.ingoboka_api.v1.partner.impls;

import com.ingoboka_api.v1.common.enums.UserStatus;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.requests.CreateStaffRequest;
import com.ingoboka_api.v1.common.requests.ResetManagedUserPasswordRequest;
import com.ingoboka_api.v1.common.requests.UpdateStaffRequest;
import com.ingoboka_api.v1.common.requests.UpdateStaffStatusRequest;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.PartnerStaffOverviewResponse;
import com.ingoboka_api.v1.common.responses.StaffCreatedResponse;
import com.ingoboka_api.v1.common.responses.StaffResponse;
import com.ingoboka_api.v1.common.security.IngobokaUserDetails;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.common.util.PaginationUtils;
import com.ingoboka_api.v1.common.util.TemporaryPasswordGenerator;
import com.ingoboka_api.v1.identity.models.Role;
import com.ingoboka_api.v1.identity.models.RoleCodes;
import com.ingoboka_api.v1.identity.models.User;
import com.ingoboka_api.v1.identity.repositories.RoleRepository;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import com.ingoboka_api.v1.identity.services.NotificationService;
import com.ingoboka_api.v1.identity.services.StaffProvisioningService;
import com.ingoboka_api.v1.partner.services.PartnerStaffService;
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
public class PartnerStaffServiceImpl implements PartnerStaffService {

    private static final Set<UserStatus> ALLOWED_STAFF_STATUS_UPDATES =
            Set.of(UserStatus.ACTIVE, UserStatus.DISABLED, UserStatus.LOCKED);

    private final StaffProvisioningService staffProvisioningService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public StaffCreatedResponse createStaff(UUID partnerId, CreateStaffRequest request) {
        assertCanManageStaff(partnerId);

        if (RoleCodes.PLATFORM_ADMIN.equals(request.getRoleCode())
                || RoleCodes.CITIZEN.equals(request.getRoleCode())
                || RoleCodes.BENEFICIARY.equals(request.getRoleCode())) {
            throw new BusinessException("Invalid role for staff member");
        }

        return staffProvisioningService.createStaffMemberWithDefaultPassword(
                partnerId,
                request.getEmail(),
                request.getPhoneNumber(),
                request.getFirstName(),
                request.getLastName(),
                request.getRoleCode(),
                request.getDefaultPassword());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StaffResponse> listStaff(UUID partnerId, int page, int size) {
        assertCanManageStaff(partnerId);
        Page<User> result = userRepository.findByOrganizationIdOrderByCreatedAtDesc(
                partnerId, PaginationUtils.toPageable(page, size));
        return PageResponse.from(result.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public StaffResponse getStaff(UUID partnerId, UUID userId) {
        assertCanManageStaff(partnerId);
        return toResponse(requireStaffMember(partnerId, userId));
    }

    @Override
    @Transactional
    public StaffResponse updateStaff(UUID partnerId, UUID userId, UpdateStaffRequest request) {
        assertCanManageStaff(partnerId);
        User user = requireStaffMember(partnerId, userId);

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
        if (StringUtils.hasText(request.getRoleCode())) {
            if (RoleCodes.PLATFORM_ADMIN.equals(request.getRoleCode())
                    || RoleCodes.CITIZEN.equals(request.getRoleCode())
                    || RoleCodes.BENEFICIARY.equals(request.getRoleCode())) {
                throw new BusinessException("Invalid role for staff member");
            }
            Role role = roleRepository
                    .findByCode(request.getRoleCode())
                    .orElseThrow(() -> new BusinessException("Role not found"));
            user.getRoles().clear();
            user.getRoles().add(role);
        }
        user.setUpdatedAt(Instant.now());
        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public StaffResponse resetStaffCredentials(UUID partnerId, UUID userId, ResetManagedUserPasswordRequest request) {
        assertCanManageStaff(partnerId);
        User user = requireStaffMember(partnerId, userId);

        String temporaryPassword = StringUtils.hasText(request.getDefaultPassword())
                ? request.getDefaultPassword()
                : TemporaryPasswordGenerator.generate(12);

        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        user.setStatus(UserStatus.PENDING_PASSWORD_CHANGE);
        user.setEmailVerified(false);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        String organizationName =
                user.getOrganization() != null ? user.getOrganization().getName() : "Partner";
        notificationService.sendStaffWelcomeEmail(user, organizationName, temporaryPassword);
        return toResponse(user);
    }

    @Override
    @Transactional
    public void deleteStaff(UUID partnerId, UUID userId) {
        assertCanManageStaff(partnerId);
        User user = requireStaffMember(partnerId, userId);
        if (user.getRoles().stream().anyMatch(r -> RoleCodes.PARTNER_ADMIN.equals(r.getCode()))
                && !SecurityUtils.isPlatformAdmin()) {
            throw new BusinessException("Partner administrators cannot remove other partner administrators");
        }
        user.setStatus(UserStatus.DISABLED);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    @Override
    @Transactional
    public StaffResponse updateStaffStatus(UUID partnerId, UUID userId, UpdateStaffStatusRequest request) {
        assertCanManageStaff(partnerId);

        if (!ALLOWED_STAFF_STATUS_UPDATES.contains(request.getStatus())) {
            throw new BusinessException("Invalid staff status");
        }

        User user = userRepository
                .findByIdAndOrganizationId(userId, partnerId)
                .orElseThrow(() -> new BusinessException("Staff member not found"));

        if (user.getRoles().stream().anyMatch(r -> RoleCodes.PARTNER_ADMIN.equals(r.getCode()))
                && request.getStatus() == UserStatus.DISABLED
                && !SecurityUtils.isPlatformAdmin()) {
            throw new BusinessException("Partner administrators cannot disable other partner administrators");
        }

        user.setStatus(request.getStatus());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        return toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PartnerStaffOverviewResponse getStaffOverview(UUID partnerId) {
        assertCanManageStaff(partnerId);
        PageResponse<StaffResponse> page = listStaff(partnerId, 0, 500);
        List<StaffResponse> staff = page.getContent();
        long pendingPassword = staff.stream()
                .filter(member -> member.isMustChangePassword()
                        || member.getStatus() == UserStatus.PENDING_PASSWORD_CHANGE)
                .count();
        long pendingEmail = staff.stream()
                .filter(member -> !member.isEmailVerified()
                        || member.getStatus() == UserStatus.PENDING_EMAIL_VERIFICATION)
                .count();
        long active = staff.stream().filter(member -> member.getStatus() == UserStatus.ACTIVE).count();
        long disabled = staff.stream()
                .filter(member ->
                        member.getStatus() == UserStatus.DISABLED || member.getStatus() == UserStatus.LOCKED)
                .count();

        return PartnerStaffOverviewResponse.builder()
                .totalStaff(staff.size())
                .pendingPasswordChange(pendingPassword)
                .pendingEmailVerification(pendingEmail)
                .activeStaff(active)
                .disabledOrLocked(disabled)
                .staff(staff)
                .build();
    }

    private StaffResponse toResponse(User user) {
        return StaffResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .status(user.getStatus())
                .emailVerified(user.isEmailVerified())
                .mustChangePassword(user.isMustChangePassword())
                .roles(user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .build();
    }

    private User requireStaffMember(UUID partnerId, UUID userId) {
        return userRepository
                .findByIdAndOrganizationId(userId, partnerId)
                .orElseThrow(() -> new BusinessException("Staff member not found"));
    }

    private void assertCanManageStaff(UUID partnerId) {
        IngobokaUserDetails user = SecurityUtils.currentUser();
        if (user.hasRole(RoleCodes.PLATFORM_ADMIN)) {
            return;
        }
        if (user.hasRole(RoleCodes.PARTNER_ADMIN) && partnerId.equals(user.getOrganizationId())) {
            return;
        }
        throw new BusinessException("Access denied to manage staff for this partner");
    }
}

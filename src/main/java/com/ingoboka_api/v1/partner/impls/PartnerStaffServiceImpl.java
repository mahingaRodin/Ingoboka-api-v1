package com.ingoboka_api.v1.partner.impls;

import com.ingoboka_api.v1.common.enums.UserStatus;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.requests.CreateStaffRequest;
import com.ingoboka_api.v1.common.requests.UpdateStaffStatusRequest;
import com.ingoboka_api.v1.common.responses.StaffCreatedResponse;
import com.ingoboka_api.v1.common.responses.StaffResponse;
import com.ingoboka_api.v1.common.security.IngobokaUserDetails;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.identity.models.Role;
import com.ingoboka_api.v1.identity.models.RoleCodes;
import com.ingoboka_api.v1.identity.models.User;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import com.ingoboka_api.v1.identity.services.StaffProvisioningService;
import com.ingoboka_api.v1.partner.services.PartnerStaffService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PartnerStaffServiceImpl implements PartnerStaffService {

    private static final Set<UserStatus> ALLOWED_STAFF_STATUS_UPDATES =
            Set.of(UserStatus.ACTIVE, UserStatus.DISABLED, UserStatus.LOCKED);

    private final StaffProvisioningService staffProvisioningService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public StaffCreatedResponse createStaff(UUID partnerId, CreateStaffRequest request) {
        assertCanManageStaff(partnerId);

        if (RoleCodes.PLATFORM_ADMIN.equals(request.getRoleCode())
                || RoleCodes.CITIZEN.equals(request.getRoleCode())
                || RoleCodes.BENEFICIARY.equals(request.getRoleCode())) {
            throw new BusinessException("Invalid role for staff member");
        }

        return staffProvisioningService.createStaffMember(
                partnerId,
                request.getEmail(),
                request.getPhoneNumber(),
                request.getFirstName(),
                request.getLastName(),
                request.getRoleCode());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StaffResponse> listStaff(UUID partnerId) {
        assertCanManageStaff(partnerId);
        return userRepository.findByOrganizationIdOrderByCreatedAtDesc(partnerId).stream()
                .map(this::toResponse)
                .toList();
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

    private StaffResponse toResponse(User user) {
        return StaffResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .status(user.getStatus())
                .roles(user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .build();
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

package com.ingoboka_api.v1.identity.controllers;

import com.ingoboka_api.v1.audit.services.AuditComplianceService;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.requests.UpdateStaffProfileRequest;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.StaffProfileResponse;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.identity.models.Role;
import com.ingoboka_api.v1.identity.models.User;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
@Tag(name = "Staff profile", description = "Personal profile for insurer and partner staff")
@SecurityRequirement(name = "bearerAuth")
public class StaffProfileController {

    private final UserRepository userRepository;
    private final AuditComplianceService auditComplianceService;

    @GetMapping("/me")
    @PreAuthorize(
            "hasAnyRole('PARTNER_ADMIN', 'CLAIMS_OFFICER', 'CLAIMS_SUPERVISOR', 'UNDERWRITER', "
                    + "'INSURER_PRODUCT_MANAGER', 'FINANCE_OFFICER', 'CUSTOMER_SUPPORT', 'AGENT', 'PLATFORM_ADMIN')")
    @Operation(summary = "Get my staff profile")
    public ApiResponse<StaffProfileResponse> getMyProfile() {
        return ApiResponse.ok("Profile retrieved", toResponse(requireCurrentStaff()));
    }

    @PutMapping("/me")
    @PreAuthorize(
            "hasAnyRole('PARTNER_ADMIN', 'CLAIMS_OFFICER', 'CLAIMS_SUPERVISOR', 'UNDERWRITER', "
                    + "'INSURER_PRODUCT_MANAGER', 'FINANCE_OFFICER', 'CUSTOMER_SUPPORT', 'AGENT', 'PLATFORM_ADMIN')")
    @Operation(summary = "Update my staff profile")
    public ApiResponse<StaffProfileResponse> updateMyProfile(@Valid @RequestBody UpdateStaffProfileRequest request) {
        User user = requireCurrentStaff();

        if (StringUtils.hasText(request.getFirstName())) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (StringUtils.hasText(request.getLastName())) {
            user.setLastName(request.getLastName().trim());
        }
        if (StringUtils.hasText(request.getEmail())
                && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
                throw new BusinessException("Email is already registered");
            }
            user.setEmail(request.getEmail().trim().toLowerCase());
            user.setEmailVerified(false);
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        user.setUpdatedAt(Instant.now());
        User saved = userRepository.save(user);

        auditComplianceService.log("STAFF_PROFILE_UPDATED", "USER", saved.getId(), "Updated staff profile");
        return ApiResponse.ok("Profile updated", toResponse(saved));
    }

    private User requireCurrentStaff() {
        return userRepository
                .findWithDetailsById(SecurityUtils.currentUser().getUserId())
                .orElseThrow(() -> new BusinessException("User not found"));
    }

    private StaffProfileResponse toResponse(User user) {
        return StaffProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .status(user.getStatus().name())
                .emailVerified(user.isEmailVerified())
                .roles(user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet()))
                .organizationId(user.getOrganization() != null ? user.getOrganization().getId() : null)
                .organizationName(user.getOrganization() != null ? user.getOrganization().getName() : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

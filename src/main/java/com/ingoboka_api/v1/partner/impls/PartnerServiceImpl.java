package com.ingoboka_api.v1.partner.impls;

import com.ingoboka_api.v1.common.enums.OrganizationType;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.requests.OnboardPartnerRequest;
import com.ingoboka_api.v1.common.requests.UpdatePartnerRequest;
import com.ingoboka_api.v1.common.requests.UpdatePartnerStatusRequest;
import com.ingoboka_api.v1.common.responses.OnboardPartnerResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.PartnerResponse;
import com.ingoboka_api.v1.common.security.IngobokaUserDetails;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.identity.models.Organization;
import com.ingoboka_api.v1.identity.models.RoleCodes;
import com.ingoboka_api.v1.identity.services.OrganizationManagementService;
import com.ingoboka_api.v1.identity.services.StaffProvisioningService;
import com.ingoboka_api.v1.partner.models.PartnerProfile;
import com.ingoboka_api.v1.partner.repositories.PartnerProfileRepository;
import com.ingoboka_api.v1.partner.services.PartnerService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PartnerServiceImpl implements PartnerService {

    private final OrganizationManagementService organizationManagementService;
    private final StaffProvisioningService staffProvisioningService;
    private final PartnerProfileRepository partnerProfileRepository;

    @Override
    @Transactional
    public OnboardPartnerResponse onboardPartner(OnboardPartnerRequest request) {
        if (request.getType() != OrganizationType.INSURER && request.getType() != OrganizationType.PARTNER) {
            throw new BusinessException("Partner type must be INSURER or PARTNER");
        }

        Organization organization = organizationManagementService.createOrganization(
                request.getName(), request.getCode(), request.getType());

        PartnerProfile profile = createProfile(organization.getId(), request);
        partnerProfileRepository.save(profile);

        var partnerAdmin = staffProvisioningService.createStaffMemberWithDefaultPassword(
                organization.getId(),
                request.getAdminEmail(),
                request.getAdminPhone(),
                request.getAdminFirstName(),
                request.getAdminLastName(),
                RoleCodes.PARTNER_ADMIN,
                request.getAdminDefaultPassword());

        return OnboardPartnerResponse.builder()
                .partner(toResponse(organization, profile))
                .partnerAdmin(partnerAdmin)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PartnerResponse> listPartners(int page, int size) {
        Page<Organization> result =
                organizationManagementService.listTenantOrganizations(page, size);
        return PageResponse.from(result.map(this::toResponseWithProfile));
    }

    @Override
    @Transactional(readOnly = true)
    public PartnerResponse getPartner(UUID partnerId) {
        assertCanAccessPartner(partnerId);
        return toResponseWithProfile(requireTenantOrganization(partnerId));
    }

    @Override
    @Transactional(readOnly = true)
    public PartnerResponse getMyPartner() {
        UUID orgId = requirePartnerOrganizationId();
        return toResponseWithProfile(requireTenantOrganization(orgId));
    }

    @Override
    @Transactional
    public PartnerResponse updatePartner(UUID partnerId, UpdatePartnerRequest request) {
        assertCanAccessPartner(partnerId);
        Organization organization = requireTenantOrganization(partnerId);

        if (request.getName() != null && !request.getName().isBlank()) {
            organization = organizationManagementService.updateOrganization(partnerId, request.getName());
        }

        PartnerProfile profile = partnerProfileRepository
                .findByOrganizationId(partnerId)
                .orElseGet(() -> createEmptyProfile(partnerId));

        applyProfileUpdates(profile, request);
        profile.setUpdatedAt(Instant.now());
        partnerProfileRepository.save(profile);

        return toResponse(organization, profile);
    }

    @Override
    @Transactional
    public PartnerResponse updatePartnerStatus(UUID partnerId, UpdatePartnerStatusRequest request) {
        if (!SecurityUtils.isPlatformAdmin()) {
            throw new BusinessException("Only platform administrators can change partner status");
        }

        Organization organization =
                organizationManagementService.updateStatus(partnerId, request.getStatus());
        PartnerProfile profile = partnerProfileRepository
                .findByOrganizationId(partnerId)
                .orElseGet(() -> createEmptyProfile(partnerId));

        return toResponse(organization, profile);
    }

    private PartnerProfile createProfile(UUID organizationId, OnboardPartnerRequest request) {
        Instant now = Instant.now();
        PartnerProfile profile = new PartnerProfile();
        profile.setId(UUID.randomUUID());
        profile.setOrganizationId(organizationId);
        profile.setRegistrationNumber(request.getRegistrationNumber());
        profile.setContactEmail(request.getContactEmail());
        profile.setContactPhone(request.getContactPhone());
        profile.setAddressLine(request.getAddressLine());
        profile.setDistrict(request.getDistrict());
        profile.setCountry(request.getCountry() != null ? request.getCountry() : "RW");
        profile.setWebsite(request.getWebsite());
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        return profile;
    }

    private PartnerProfile createEmptyProfile(UUID organizationId) {
        Instant now = Instant.now();
        PartnerProfile profile = new PartnerProfile();
        profile.setId(UUID.randomUUID());
        profile.setOrganizationId(organizationId);
        profile.setCountry("RW");
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        return profile;
    }

    private void applyProfileUpdates(PartnerProfile profile, UpdatePartnerRequest request) {
        if (request.getRegistrationNumber() != null) {
            profile.setRegistrationNumber(request.getRegistrationNumber());
        }
        if (request.getContactEmail() != null) {
            profile.setContactEmail(request.getContactEmail());
        }
        if (request.getContactPhone() != null) {
            profile.setContactPhone(request.getContactPhone());
        }
        if (request.getAddressLine() != null) {
            profile.setAddressLine(request.getAddressLine());
        }
        if (request.getDistrict() != null) {
            profile.setDistrict(request.getDistrict());
        }
        if (request.getCountry() != null) {
            profile.setCountry(request.getCountry());
        }
        if (request.getWebsite() != null) {
            profile.setWebsite(request.getWebsite());
        }
    }

    private PartnerResponse toResponseWithProfile(Organization organization) {
        PartnerProfile profile = partnerProfileRepository
                .findByOrganizationId(organization.getId())
                .orElseGet(() -> createEmptyProfile(organization.getId()));
        return toResponse(organization, profile);
    }

    private PartnerResponse toResponse(Organization organization, PartnerProfile profile) {
        return PartnerResponse.builder()
                .id(organization.getId())
                .name(organization.getName())
                .code(organization.getCode())
                .type(organization.getType())
                .status(organization.getStatus())
                .registrationNumber(profile.getRegistrationNumber())
                .contactEmail(profile.getContactEmail())
                .contactPhone(profile.getContactPhone())
                .addressLine(profile.getAddressLine())
                .district(profile.getDistrict())
                .country(profile.getCountry())
                .website(profile.getWebsite())
                .createdAt(organization.getCreatedAt())
                .updatedAt(organization.getUpdatedAt())
                .build();
    }

    private Organization requireTenantOrganization(UUID partnerId) {
        Organization organization = organizationManagementService
                .findById(partnerId)
                .orElseThrow(() -> new BusinessException("Partner not found"));

        if (organization.getType() != OrganizationType.INSURER
                && organization.getType() != OrganizationType.PARTNER) {
            throw new BusinessException("Organization is not a partner tenant");
        }
        return organization;
    }

    private void assertCanAccessPartner(UUID partnerId) {
        IngobokaUserDetails user = SecurityUtils.currentUser();
        if (user.hasRole(RoleCodes.PLATFORM_ADMIN)) {
            return;
        }
        if (user.hasRole(RoleCodes.PARTNER_ADMIN) && partnerId.equals(user.getOrganizationId())) {
            return;
        }
        throw new BusinessException("Access denied to this partner");
    }

    private UUID requirePartnerOrganizationId() {
        IngobokaUserDetails user = SecurityUtils.currentUser();
        if (user.getOrganizationId() == null) {
            throw new BusinessException("No organization associated with this account");
        }
        if (!user.hasRole(RoleCodes.PARTNER_ADMIN)) {
            throw new BusinessException("Only partner administrators can access this resource");
        }
        return user.getOrganizationId();
    }
}

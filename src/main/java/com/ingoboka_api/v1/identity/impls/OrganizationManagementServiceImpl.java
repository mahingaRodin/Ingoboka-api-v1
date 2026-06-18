package com.ingoboka_api.v1.identity.impls;

import com.ingoboka_api.v1.common.enums.OrganizationStatus;
import com.ingoboka_api.v1.common.enums.OrganizationType;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.identity.models.Organization;
import com.ingoboka_api.v1.identity.repositories.OrganizationRepository;
import com.ingoboka_api.v1.identity.services.OrganizationManagementService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationManagementServiceImpl implements OrganizationManagementService {

    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional
    public Organization createOrganization(String name, String code, OrganizationType type) {
        if (type == OrganizationType.PLATFORM) {
            throw new BusinessException("Cannot create platform organizations through this API");
        }
        if (organizationRepository.existsByCode(code)) {
            throw new BusinessException("Organization code already exists");
        }

        Instant now = Instant.now();
        Organization organization = new Organization();
        organization.setId(UUID.randomUUID());
        organization.setName(name.trim());
        organization.setCode(code.trim().toUpperCase());
        organization.setType(type);
        organization.setStatus(OrganizationStatus.ACTIVE);
        organization.setCreatedAt(now);
        organization.setUpdatedAt(now);
        return organizationRepository.save(organization);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Organization> findById(UUID id) {
        return organizationRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Organization> listTenantOrganizations() {
        return organizationRepository.findByTypeInOrderByCreatedAtDesc(
                List.of(OrganizationType.INSURER, OrganizationType.PARTNER));
    }

    @Override
    @Transactional
    public Organization updateOrganization(UUID id, String name) {
        Organization organization = organizationRepository
                .findById(id)
                .orElseThrow(() -> new BusinessException("Organization not found"));
        organization.setName(name.trim());
        organization.setUpdatedAt(Instant.now());
        return organizationRepository.save(organization);
    }

    @Override
    @Transactional
    public Organization updateStatus(UUID id, OrganizationStatus status) {
        Organization organization = organizationRepository
                .findById(id)
                .orElseThrow(() -> new BusinessException("Organization not found"));
        organization.setStatus(status);
        organization.setUpdatedAt(Instant.now());
        return organizationRepository.save(organization);
    }
}

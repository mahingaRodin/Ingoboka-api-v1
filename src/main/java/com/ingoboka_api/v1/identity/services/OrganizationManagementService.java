package com.ingoboka_api.v1.identity.services;

import com.ingoboka_api.v1.common.enums.OrganizationStatus;
import com.ingoboka_api.v1.common.enums.OrganizationType;
import com.ingoboka_api.v1.identity.models.Organization;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;

public interface OrganizationManagementService {

    Organization createOrganization(String name, String code, OrganizationType type);

    Optional<Organization> findById(UUID id);

    Page<Organization> listTenantOrganizations(int page, int size);

    Organization updateOrganization(UUID id, String name);

    Organization updateStatus(UUID id, OrganizationStatus status);
}

package com.ingoboka_api.v1.identity.services;

import com.ingoboka_api.v1.common.enums.OrganizationStatus;
import com.ingoboka_api.v1.common.enums.OrganizationType;
import com.ingoboka_api.v1.identity.models.Organization;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationManagementService {

    Organization createOrganization(String name, String code, OrganizationType type);

    Optional<Organization> findById(UUID id);

    List<Organization> listTenantOrganizations();

    Organization updateOrganization(UUID id, String name);

    Organization updateStatus(UUID id, OrganizationStatus status);
}

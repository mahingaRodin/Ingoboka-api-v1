package com.ingoboka_api.v1.identity.services;

import com.ingoboka_api.v1.common.responses.StaffCreatedResponse;
import java.util.UUID;

public interface StaffProvisioningService {

    StaffCreatedResponse createStaffMember(
            UUID organizationId,
            String email,
            String phoneNumber,
            String firstName,
            String lastName,
            String roleCode);

    StaffCreatedResponse createStaffMemberWithDefaultPassword(
            UUID organizationId,
            String email,
            String phoneNumber,
            String firstName,
            String lastName,
            String roleCode,
            String defaultPassword);
}

package com.ingoboka_api.v1.partner.services;

import com.ingoboka_api.v1.common.requests.CreateStaffRequest;
import com.ingoboka_api.v1.common.requests.ResetManagedUserPasswordRequest;
import com.ingoboka_api.v1.common.requests.UpdateStaffRequest;
import com.ingoboka_api.v1.common.requests.UpdateStaffStatusRequest;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.PartnerStaffOverviewResponse;
import com.ingoboka_api.v1.common.responses.StaffCreatedResponse;
import com.ingoboka_api.v1.common.responses.StaffResponse;
import java.util.UUID;

public interface PartnerStaffService {

    StaffCreatedResponse createStaff(UUID partnerId, CreateStaffRequest request);

    PageResponse<StaffResponse> listStaff(UUID partnerId, int page, int size);

    StaffResponse getStaff(UUID partnerId, UUID userId);

    StaffResponse updateStaff(UUID partnerId, UUID userId, UpdateStaffRequest request);

    StaffResponse updateStaffStatus(UUID partnerId, UUID userId, UpdateStaffStatusRequest request);

    StaffResponse resetStaffCredentials(UUID partnerId, UUID userId, ResetManagedUserPasswordRequest request);

    void deleteStaff(UUID partnerId, UUID userId);

    PartnerStaffOverviewResponse getStaffOverview(UUID partnerId);
}

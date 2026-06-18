package com.ingoboka_api.v1.partner.services;

import com.ingoboka_api.v1.common.requests.CreateStaffRequest;
import com.ingoboka_api.v1.common.requests.UpdateStaffStatusRequest;
import com.ingoboka_api.v1.common.responses.StaffCreatedResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.StaffResponse;
import java.util.UUID;

public interface PartnerStaffService {

    StaffCreatedResponse createStaff(UUID partnerId, CreateStaffRequest request);

    PageResponse<StaffResponse> listStaff(UUID partnerId, int page, int size);

    StaffResponse updateStaffStatus(UUID partnerId, UUID userId, UpdateStaffStatusRequest request);
}

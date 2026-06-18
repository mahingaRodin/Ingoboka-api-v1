package com.ingoboka_api.v1.partner.services;

import com.ingoboka_api.v1.common.requests.OnboardPartnerRequest;
import com.ingoboka_api.v1.common.requests.UpdatePartnerRequest;
import com.ingoboka_api.v1.common.requests.UpdatePartnerStatusRequest;
import com.ingoboka_api.v1.common.responses.OnboardPartnerResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.PartnerResponse;
import java.util.UUID;

public interface PartnerService {

    OnboardPartnerResponse onboardPartner(OnboardPartnerRequest request);

    PageResponse<PartnerResponse> listPartners(int page, int size);

    PartnerResponse getPartner(UUID partnerId);

    PartnerResponse getMyPartner();

    PartnerResponse updatePartner(UUID partnerId, UpdatePartnerRequest request);

    PartnerResponse updatePartnerStatus(UUID partnerId, UpdatePartnerStatusRequest request);
}

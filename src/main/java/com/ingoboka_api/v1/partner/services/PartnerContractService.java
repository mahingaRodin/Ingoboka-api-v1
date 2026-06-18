package com.ingoboka_api.v1.partner.services;

import com.ingoboka_api.v1.common.requests.CreatePartnerContractRequest;
import com.ingoboka_api.v1.common.requests.UpdateContractStatusRequest;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.PartnerContractResponse;
import java.util.UUID;

public interface PartnerContractService {

    PartnerContractResponse createContract(UUID partnerId, CreatePartnerContractRequest request);

    PageResponse<PartnerContractResponse> listContracts(UUID partnerId, int page, int size);

    PartnerContractResponse updateContractStatus(
            UUID partnerId, UUID contractId, UpdateContractStatusRequest request);
}

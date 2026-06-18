package com.ingoboka_api.v1.partner.services;

import com.ingoboka_api.v1.common.requests.CreatePartnerContractRequest;
import com.ingoboka_api.v1.common.requests.UpdateContractStatusRequest;
import com.ingoboka_api.v1.common.responses.PartnerContractResponse;
import java.util.List;
import java.util.UUID;

public interface PartnerContractService {

    PartnerContractResponse createContract(UUID partnerId, CreatePartnerContractRequest request);

    List<PartnerContractResponse> listContracts(UUID partnerId);

    PartnerContractResponse updateContractStatus(
            UUID partnerId, UUID contractId, UpdateContractStatusRequest request);
}

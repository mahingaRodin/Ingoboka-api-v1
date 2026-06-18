package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.ContractStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PartnerContractResponse {
    UUID id;
    UUID organizationId;
    String contractReference;
    ContractStatus status;
    LocalDate startDate;
    LocalDate endDate;
    String notes;
    Instant createdAt;
}

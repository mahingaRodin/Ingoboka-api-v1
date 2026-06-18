package com.ingoboka_api.v1.common.requests;

import com.ingoboka_api.v1.common.enums.ContractStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateContractStatusRequest {

    @NotNull(message = "Status is required")
    private ContractStatus status;
}

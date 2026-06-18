package com.ingoboka_api.v1.common.requests;

import com.ingoboka_api.v1.common.enums.ClaimStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateClaimStatusRequest {

    @NotNull
    private ClaimStatus status;

    private String reason;
}

package com.ingoboka_api.v1.common.requests;

import com.ingoboka_api.v1.common.enums.KycStatus;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class ReviewKycRequest {

    @NotNull
    private UUID citizenProfileId;

    @NotNull
    private KycStatus status;

    private String notes;
}

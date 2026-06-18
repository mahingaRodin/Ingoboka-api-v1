package com.ingoboka_api.v1.common.requests;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class SubmitApplicationRequest {

    @NotNull(message = "Quote ID is required")
    private UUID quoteId;

    @NotNull(message = "Consent ID is required")
    private UUID consentId;
}

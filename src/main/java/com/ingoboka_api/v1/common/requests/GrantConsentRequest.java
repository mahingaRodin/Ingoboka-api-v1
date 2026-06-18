package com.ingoboka_api.v1.common.requests;

import com.ingoboka_api.v1.common.enums.ConsentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GrantConsentRequest {

    @NotNull(message = "Consent type is required")
    private ConsentType consentType;

    @NotBlank(message = "Consent version is required")
    private String version;
}

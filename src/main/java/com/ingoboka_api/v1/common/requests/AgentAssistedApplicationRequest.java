package com.ingoboka_api.v1.common.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class AgentAssistedApplicationRequest {

    @NotBlank
    private String citizenPhone;

    @NotNull
    private UUID productPlanId;
}

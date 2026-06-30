package com.ingoboka_api.v1.common.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class CreateClaimRequest {

    @NotNull
    private UUID policyId;

    @NotBlank
    private String claimType;

    @NotBlank
    private String description;

    private BigDecimal claimedAmount;

    private java.time.LocalDate incidentDate;
}

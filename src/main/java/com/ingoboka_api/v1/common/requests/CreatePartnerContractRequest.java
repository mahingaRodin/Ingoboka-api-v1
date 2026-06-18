package com.ingoboka_api.v1.common.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreatePartnerContractRequest {

    @NotBlank(message = "Contract reference is required")
    @Pattern(regexp = "^[A-Za-z0-9_-]{3,64}$", message = "Contract reference must be 3-64 alphanumeric characters")
    private String contractReference;

    private LocalDate startDate;
    private LocalDate endDate;
    private String notes;
}

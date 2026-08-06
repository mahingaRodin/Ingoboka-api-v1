package com.ingoboka_api.v1.common.requests;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class UpdateClaimRequest {
    private String claimType;
    private String description;
    private BigDecimal claimedAmount;
    private LocalDate incidentDate;
}

package com.ingoboka_api.v1.common.requests;

import com.ingoboka_api.v1.common.enums.OrganizationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePartnerStatusRequest {

    @NotNull(message = "Status is required")
    private OrganizationStatus status;
}

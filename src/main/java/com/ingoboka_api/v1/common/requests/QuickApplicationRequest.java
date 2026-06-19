package com.ingoboka_api.v1.common.requests;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class QuickApplicationRequest {

    @NotNull(message = "Product plan ID is required")
    private UUID productPlanId;

    private List<UUID> beneficiaries;
}

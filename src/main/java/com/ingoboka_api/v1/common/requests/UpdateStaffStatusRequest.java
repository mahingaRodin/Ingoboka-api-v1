package com.ingoboka_api.v1.common.requests;

import com.ingoboka_api.v1.common.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStaffStatusRequest {

    @NotNull(message = "Status is required")
    private UserStatus status;
}

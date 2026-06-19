package com.ingoboka_api.v1.common.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateManagedUserRolesRequest {

    @NotBlank(message = "Role code is required")
    private String roleCode;
}

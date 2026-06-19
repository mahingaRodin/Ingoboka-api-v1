package com.ingoboka_api.v1.common.requests;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetManagedUserPasswordRequest {

    @Size(min = 8, message = "Default password must be at least 8 characters when provided")
    private String defaultPassword;
}

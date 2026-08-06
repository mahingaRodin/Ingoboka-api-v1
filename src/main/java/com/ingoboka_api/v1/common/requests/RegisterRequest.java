package com.ingoboka_api.v1.common.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "National ID is required")
    private String nationalId;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    /** Required when server OTP mode is EMAIL (SMS unavailable). Optional otherwise. */
    private String email;

    @NotBlank(message = "Province is required")
    @Size(max = 120)
    private String province;

    @NotBlank(message = "District is required")
    @Size(max = 120)
    private String district;

    @NotBlank(message = "Sector is required")
    @Size(max = 120)
    private String sector;

    @NotBlank(message = "Cell is required")
    @Size(max = 120)
    private String cell;

    @NotBlank(message = "Village is required")
    @Size(max = 120)
    private String village;
}

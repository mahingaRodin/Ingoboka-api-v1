package com.ingoboka_api.v1.common.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateStaffRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    private String phoneNumber;

    @NotBlank(message = "Role code is required")
    private String roleCode;

    @Size(min = 8, message = "Default password must be at least 8 characters when provided")
    private String defaultPassword;

    /** When true, sends an invite link instead of a temporary password email. Defaults to true. */
    private boolean inviteOnly = true;
}

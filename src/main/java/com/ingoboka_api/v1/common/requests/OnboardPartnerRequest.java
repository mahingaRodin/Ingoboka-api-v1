package com.ingoboka_api.v1.common.requests;

import com.ingoboka_api.v1.common.enums.OrganizationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OnboardPartnerRequest {

    @NotBlank(message = "Organization name is required")
    private String name;

    @NotBlank(message = "Organization code is required")
    @Pattern(regexp = "^[A-Za-z0-9_-]{3,64}$", message = "Code must be 3-64 alphanumeric characters")
    private String code;

    @NotNull(message = "Organization type is required")
    private OrganizationType type;

    private String registrationNumber;
    private String contactEmail;
    private String contactPhone;
    private String addressLine;
    private String district;
    private String country = "RW";
    private String website;

    @NotBlank(message = "Admin first name is required")
    private String adminFirstName;

    @NotBlank(message = "Admin last name is required")
    private String adminLastName;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Admin email must be valid")
    private String adminEmail;

    private String adminPhone;

    /** Optional. When omitted a secure temporary password is generated and emailed. */
    @Size(min = 8, message = "Default password must be at least 8 characters")
    private String adminDefaultPassword;
}

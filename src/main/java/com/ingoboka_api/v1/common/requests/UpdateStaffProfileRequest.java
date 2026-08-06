package com.ingoboka_api.v1.common.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateStaffProfileRequest {

    private String firstName;
    private String lastName;

    @Email(message = "Email must be valid")
    private String email;

    private String phoneNumber;

    /** Optional external profile picture URL (http/https). */
    @Size(max = 2048)
    private String profilePictureUrl;
}

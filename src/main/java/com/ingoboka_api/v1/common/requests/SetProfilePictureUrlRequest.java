package com.ingoboka_api.v1.common.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SetProfilePictureUrlRequest {

    @NotBlank(message = "Profile picture URL is required")
    @Size(max = 2048, message = "Profile picture URL is too long")
    private String profilePictureUrl;
}

package com.ingoboka_api.v1.common.requests;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePartnerRequest {

    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    private String registrationNumber;
    private String contactEmail;
    private String contactPhone;
    private String addressLine;
    private String district;
    private String country;
    private String website;
}

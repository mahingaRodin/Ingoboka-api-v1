package com.ingoboka_api.v1.common.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateProductRequest {

    @NotBlank(message = "Product code is required")
    private String code;

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotBlank(message = "Category is required")
    private String category;
}

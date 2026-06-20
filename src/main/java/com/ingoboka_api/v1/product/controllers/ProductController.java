package com.ingoboka_api.v1.product.controllers;

import com.ingoboka_api.v1.common.requests.CreateProductPlanRequest;
import com.ingoboka_api.v1.common.requests.CreateProductRequest;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.ProductDetailResponse;
import com.ingoboka_api.v1.common.responses.ProductPlanResponse;
import com.ingoboka_api.v1.common.responses.ProductResponse;
import com.ingoboka_api.v1.product.services.ProductCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Catalog", description = "Insurance products, plans, benefits, and eligibility")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductCatalogService productCatalogService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('INSURER_PRODUCT_MANAGER', 'PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "Create product", description = "Insurer product manager creates a draft product")
    public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ApiResponse.ok("Product created", productCatalogService.createProduct(request));
    }

    @PostMapping("/{productId}/publish")
    @PreAuthorize("hasAnyRole('INSURER_PRODUCT_MANAGER', 'PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "Publish product")
    public ApiResponse<ProductResponse> publishProduct(@PathVariable UUID productId) {
        return ApiResponse.ok("Product published", productCatalogService.publishProduct(productId));
    }

    @GetMapping("/tenant")
    @PreAuthorize("hasAnyRole('INSURER_PRODUCT_MANAGER', 'PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "List tenant products", description = "All products for the authenticated insurer tenant")
    public ApiResponse<PageResponse<ProductResponse>> listTenantProducts(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Products retrieved", productCatalogService.listTenantProducts(page, size));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Browse published products", description = "Citizens and staff browse the public catalog")
    public ApiResponse<PageResponse<ProductResponse>> listPublishedProducts(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Products retrieved", productCatalogService.listPublishedProducts(page, size));
    }

    @GetMapping("/{productId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get product")
    public ApiResponse<ProductResponse> getProduct(@PathVariable UUID productId) {
        return ApiResponse.ok("Product retrieved", productCatalogService.getProduct(productId));
    }

    @GetMapping("/{productId}/detail")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get product detail", description = "Structured product content for citizen detail tabs")
    public ApiResponse<ProductDetailResponse> getProductDetail(@PathVariable UUID productId) {
        return ApiResponse.ok("Product detail retrieved", productCatalogService.getProductDetail(productId));
    }

    @PostMapping("/{productId}/plans")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('INSURER_PRODUCT_MANAGER', 'PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "Create plan", description = "Add plan with benefits, exclusions, and eligibility rules")
    public ApiResponse<ProductPlanResponse> createPlan(
            @PathVariable UUID productId, @Valid @RequestBody CreateProductPlanRequest request) {
        return ApiResponse.ok("Plan created", productCatalogService.createPlan(productId, request));
    }

    @PostMapping("/{productId}/plans/{planId}/publish")
    @PreAuthorize("hasAnyRole('INSURER_PRODUCT_MANAGER', 'PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "Publish plan")
    public ApiResponse<ProductPlanResponse> publishPlan(
            @PathVariable UUID productId, @PathVariable UUID planId) {
        return ApiResponse.ok("Plan published", productCatalogService.publishPlan(productId, planId));
    }

    @GetMapping("/{productId}/plans")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List plans for product")
    public ApiResponse<PageResponse<ProductPlanResponse>> listPlans(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Plans retrieved", productCatalogService.listPlans(productId, page, size));
    }

    @GetMapping("/{productId}/plans/{planId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get plan details")
    public ApiResponse<ProductPlanResponse> getPlan(
            @PathVariable UUID productId, @PathVariable UUID planId) {
        return ApiResponse.ok("Plan retrieved", productCatalogService.getPlan(planId));
    }
}

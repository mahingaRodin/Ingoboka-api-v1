package com.ingoboka_api.v1.product.services;

import com.ingoboka_api.v1.common.requests.CreateProductPlanRequest;
import com.ingoboka_api.v1.common.requests.CreateProductRequest;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.ProductDetailResponse;
import com.ingoboka_api.v1.common.responses.ProductPlanResponse;
import com.ingoboka_api.v1.common.responses.ProductResponse;
import com.ingoboka_api.v1.product.models.ProductPlan;
import java.util.UUID;

public interface ProductCatalogService {

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse publishProduct(UUID productId);

    PageResponse<ProductResponse> listTenantProducts(int page, int size);

    PageResponse<ProductResponse> listPublishedProducts(int page, int size);

    ProductResponse getProduct(UUID productId);

    ProductDetailResponse getProductDetail(UUID productId);

    ProductPlanResponse createPlan(UUID productId, CreateProductPlanRequest request);

    ProductPlanResponse publishPlan(UUID productId, UUID planId);

    PageResponse<ProductPlanResponse> listPlans(UUID productId, int page, int size);

    ProductPlanResponse getPlan(UUID planId);

    ProductPlan requirePublishedPlan(UUID planId);
}

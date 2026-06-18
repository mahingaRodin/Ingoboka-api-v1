package com.ingoboka_api.v1.product.services;

import com.ingoboka_api.v1.common.requests.CreateProductPlanRequest;
import com.ingoboka_api.v1.common.requests.CreateProductRequest;
import com.ingoboka_api.v1.common.responses.ProductPlanResponse;
import com.ingoboka_api.v1.common.responses.ProductResponse;
import com.ingoboka_api.v1.product.models.ProductPlan;
import java.util.List;
import java.util.UUID;

public interface ProductCatalogService {

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse publishProduct(UUID productId);

    List<ProductResponse> listTenantProducts();

    List<ProductResponse> listPublishedProducts();

    ProductResponse getProduct(UUID productId);

    ProductPlanResponse createPlan(UUID productId, CreateProductPlanRequest request);

    ProductPlanResponse publishPlan(UUID productId, UUID planId);

    List<ProductPlanResponse> listPlans(UUID productId);

    ProductPlanResponse getPlan(UUID planId);

    ProductPlan requirePublishedPlan(UUID planId);
}

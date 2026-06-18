package com.ingoboka_api.v1.product.impls;

import com.ingoboka_api.v1.common.enums.OrganizationType;
import com.ingoboka_api.v1.common.enums.ProductStatus;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.requests.CreateProductPlanRequest;
import com.ingoboka_api.v1.common.requests.CreateProductRequest;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.ProductPlanResponse;
import com.ingoboka_api.v1.common.responses.ProductResponse;
import com.ingoboka_api.v1.common.security.IngobokaUserDetails;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.common.util.PaginationUtils;
import com.ingoboka_api.v1.identity.models.Organization;
import com.ingoboka_api.v1.identity.models.RoleCodes;
import com.ingoboka_api.v1.identity.services.OrganizationManagementService;
import com.ingoboka_api.v1.product.models.EligibilityRule;
import com.ingoboka_api.v1.product.models.InsuranceProduct;
import com.ingoboka_api.v1.product.models.ProductBenefit;
import com.ingoboka_api.v1.product.models.ProductExclusion;
import com.ingoboka_api.v1.product.models.ProductPlan;
import com.ingoboka_api.v1.product.repositories.EligibilityRuleRepository;
import com.ingoboka_api.v1.product.repositories.InsuranceProductRepository;
import com.ingoboka_api.v1.product.repositories.ProductBenefitRepository;
import com.ingoboka_api.v1.product.repositories.ProductExclusionRepository;
import com.ingoboka_api.v1.product.repositories.ProductPlanRepository;
import com.ingoboka_api.v1.product.services.ProductCatalogService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductCatalogServiceImpl implements ProductCatalogService {

    private final InsuranceProductRepository productRepository;
    private final ProductPlanRepository planRepository;
    private final ProductBenefitRepository benefitRepository;
    private final ProductExclusionRepository exclusionRepository;
    private final EligibilityRuleRepository eligibilityRuleRepository;
    private final OrganizationManagementService organizationManagementService;

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        UUID orgId = requireInsurerOrganizationId();
        if (productRepository.existsByOrganizationIdAndCode(orgId, request.getCode())) {
            throw new BusinessException("Product code already exists for this insurer");
        }

        Instant now = Instant.now();
        InsuranceProduct product = new InsuranceProduct();
        product.setId(UUID.randomUUID());
        product.setOrganizationId(orgId);
        product.setCode(request.getCode());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setStatus(ProductStatus.DRAFT);
        product.setCreatedAt(now);
        product.setUpdatedAt(now);
        return toProductResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse publishProduct(UUID productId) {
        InsuranceProduct product = requireTenantProduct(productId);
        assertCanManageProducts(product.getOrganizationId());
        if (product.getStatus() == ProductStatus.ARCHIVED) {
            throw new BusinessException("Archived products cannot be published");
        }
        product.setStatus(ProductStatus.PUBLISHED);
        product.setPublishedAt(Instant.now());
        product.setUpdatedAt(Instant.now());
        return toProductResponse(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> listTenantProducts(int page, int size) {
        UUID orgId = requireInsurerOrganizationId();
        Page<InsuranceProduct> result = productRepository.findByOrganizationIdOrderByCreatedAtDesc(
                orgId, PaginationUtils.toPageable(page, size));
        return PageResponse.from(result.map(this::toProductResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> listPublishedProducts(int page, int size) {
        Page<InsuranceProduct> result = productRepository.findByStatusOrderByPublishedAtDesc(
                ProductStatus.PUBLISHED, PaginationUtils.toPageable(page, size, "publishedAt"));
        return PageResponse.from(result.map(this::toProductResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID productId) {
        InsuranceProduct product = productRepository
                .findById(productId)
                .orElseThrow(() -> new BusinessException("Product not found"));
        if (product.getStatus() != ProductStatus.PUBLISHED && !canManageProducts(product.getOrganizationId())) {
            throw new BusinessException("Product is not available");
        }
        return toProductResponse(product);
    }

    @Override
    @Transactional
    public ProductPlanResponse createPlan(UUID productId, CreateProductPlanRequest request) {
        InsuranceProduct product = requireTenantProduct(productId);
        assertCanManageProducts(product.getOrganizationId());
        if (planRepository.existsByProductIdAndCode(productId, request.getCode())) {
            throw new BusinessException("Plan code already exists for this product");
        }

        Instant now = Instant.now();
        ProductPlan plan = new ProductPlan();
        plan.setId(UUID.randomUUID());
        plan.setProductId(productId);
        plan.setCode(request.getCode());
        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setPremiumAmount(request.getPremiumAmount());
        plan.setPremiumFrequency(request.getPremiumFrequency());
        plan.setWaitingPeriodDays(
                request.getWaitingPeriodDays() != null ? request.getWaitingPeriodDays() : 0);
        plan.setStatus(ProductStatus.DRAFT);
        plan.setCreatedAt(now);
        plan.setUpdatedAt(now);
        planRepository.save(plan);

        savePlanDetails(plan.getId(), request, now);
        return getPlan(plan.getId());
    }

    @Override
    @Transactional
    public ProductPlanResponse publishPlan(UUID productId, UUID planId) {
        InsuranceProduct product = requireTenantProduct(productId);
        assertCanManageProducts(product.getOrganizationId());
        if (product.getStatus() != ProductStatus.PUBLISHED) {
            throw new BusinessException("Publish the product before publishing plans");
        }

        ProductPlan plan = planRepository
                .findByIdAndProductId(planId, productId)
                .orElseThrow(() -> new BusinessException("Plan not found"));
        plan.setStatus(ProductStatus.PUBLISHED);
        plan.setUpdatedAt(Instant.now());
        planRepository.save(plan);
        return getPlan(planId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductPlanResponse> listPlans(UUID productId, int page, int size) {
        InsuranceProduct product = productRepository
                .findById(productId)
                .orElseThrow(() -> new BusinessException("Product not found"));
        boolean manager = canManageProducts(product.getOrganizationId());
        Page<ProductPlan> result = manager
                ? planRepository.findByProductIdOrderByCreatedAtDesc(
                        productId, PaginationUtils.toPageable(page, size))
                : planRepository.findByProductIdAndStatusOrderByCreatedAtDesc(
                        productId, ProductStatus.PUBLISHED, PaginationUtils.toPageable(page, size));
        return PageResponse.from(result.map(plan -> buildPlanResponse(plan, true)));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductPlanResponse getPlan(UUID planId) {
        ProductPlan plan = planRepository
                .findById(planId)
                .orElseThrow(() -> new BusinessException("Plan not found"));
        InsuranceProduct product = productRepository
                .findById(plan.getProductId())
                .orElseThrow(() -> new BusinessException("Product not found"));
        if (plan.getStatus() != ProductStatus.PUBLISHED && !canManageProducts(product.getOrganizationId())) {
            throw new BusinessException("Plan is not available");
        }
        return buildPlanResponse(plan, true);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductPlan requirePublishedPlan(UUID planId) {
        ProductPlan plan = planRepository
                .findById(planId)
                .orElseThrow(() -> new BusinessException("Plan not found"));
        if (plan.getStatus() != ProductStatus.PUBLISHED) {
            throw new BusinessException("Plan is not published");
        }
        InsuranceProduct product = productRepository
                .findById(plan.getProductId())
                .orElseThrow(() -> new BusinessException("Product not found"));
        if (product.getStatus() != ProductStatus.PUBLISHED) {
            throw new BusinessException("Product is not published");
        }
        return plan;
    }

    private void savePlanDetails(UUID planId, CreateProductPlanRequest request, Instant now) {
        if (request.getBenefits() != null) {
            int order = 0;
            for (CreateProductPlanRequest.PlanBenefitRequest benefitRequest : request.getBenefits()) {
                ProductBenefit benefit = new ProductBenefit();
                benefit.setId(UUID.randomUUID());
                benefit.setPlanId(planId);
                benefit.setTitle(benefitRequest.getTitle());
                benefit.setDescription(benefitRequest.getDescription());
                benefit.setCoverageLimit(benefitRequest.getCoverageLimit());
                benefit.setSortOrder(
                        benefitRequest.getSortOrder() != null ? benefitRequest.getSortOrder() : order++);
                benefit.setCreatedAt(now);
                benefitRepository.save(benefit);
            }
        }
        if (request.getExclusions() != null) {
            int order = 0;
            for (CreateProductPlanRequest.PlanExclusionRequest exclusionRequest : request.getExclusions()) {
                ProductExclusion exclusion = new ProductExclusion();
                exclusion.setId(UUID.randomUUID());
                exclusion.setPlanId(planId);
                exclusion.setTitle(exclusionRequest.getTitle());
                exclusion.setDescription(exclusionRequest.getDescription());
                exclusion.setSortOrder(
                        exclusionRequest.getSortOrder() != null ? exclusionRequest.getSortOrder() : order++);
                exclusion.setCreatedAt(now);
                exclusionRepository.save(exclusion);
            }
        }
        if (request.getEligibility() != null) {
            EligibilityRule rule = new EligibilityRule();
            rule.setId(UUID.randomUUID());
            rule.setPlanId(planId);
            rule.setMinAge(request.getEligibility().getMinAge());
            rule.setMaxAge(request.getEligibility().getMaxAge());
            rule.setRuleType("AGE_RANGE");
            rule.setCreatedAt(now);
            eligibilityRuleRepository.save(rule);
        }
    }

    private ProductPlanResponse buildPlanResponse(ProductPlan plan, boolean includeDetails) {
        ProductPlanResponse.ProductPlanResponseBuilder builder = ProductPlanResponse.builder()
                .id(plan.getId())
                .productId(plan.getProductId())
                .code(plan.getCode())
                .name(plan.getName())
                .description(plan.getDescription())
                .premiumAmount(plan.getPremiumAmount())
                .premiumFrequency(plan.getPremiumFrequency())
                .waitingPeriodDays(plan.getWaitingPeriodDays())
                .status(plan.getStatus())
                .createdAt(plan.getCreatedAt());

        if (includeDetails) {
            builder.benefits(benefitRepository.findByPlanIdOrderBySortOrderAsc(plan.getId()).stream()
                    .map(b -> ProductPlanResponse.PlanBenefitResponse.builder()
                            .id(b.getId())
                            .title(b.getTitle())
                            .description(b.getDescription())
                            .coverageLimit(b.getCoverageLimit())
                            .sortOrder(b.getSortOrder())
                            .build())
                    .toList());
            builder.exclusions(exclusionRepository.findByPlanIdOrderBySortOrderAsc(plan.getId()).stream()
                    .map(e -> ProductPlanResponse.PlanExclusionResponse.builder()
                            .id(e.getId())
                            .title(e.getTitle())
                            .description(e.getDescription())
                            .sortOrder(e.getSortOrder())
                            .build())
                    .toList());
            eligibilityRuleRepository.findByPlanId(plan.getId()).stream()
                    .findFirst()
                    .ifPresent(rule -> builder.eligibility(ProductPlanResponse.EligibilityRuleResponse.builder()
                            .id(rule.getId())
                            .minAge(rule.getMinAge())
                            .maxAge(rule.getMaxAge())
                            .build()));
        }
        return builder.build();
    }

    private InsuranceProduct requireTenantProduct(UUID productId) {
        return productRepository
                .findById(productId)
                .orElseThrow(() -> new BusinessException("Product not found"));
    }

    private UUID requireInsurerOrganizationId() {
        IngobokaUserDetails user = SecurityUtils.currentUser();
        if (user.getOrganizationId() == null) {
            throw new BusinessException("No organization associated with this account");
        }
        if (!user.hasRole(RoleCodes.INSURER_PRODUCT_MANAGER)
                && !user.hasRole(RoleCodes.PARTNER_ADMIN)
                && !user.hasRole(RoleCodes.PLATFORM_ADMIN)) {
            throw new BusinessException("Only product managers can manage the catalog");
        }
        Organization org = organizationManagementService
                .findById(user.getOrganizationId())
                .orElseThrow(() -> new BusinessException("Organization not found"));
        if (org.getType() != OrganizationType.INSURER && !user.hasRole(RoleCodes.PLATFORM_ADMIN)) {
            throw new BusinessException("Products can only be managed by insurer tenants");
        }
        return user.getOrganizationId();
    }

    private void assertCanManageProducts(UUID organizationId) {
        if (!canManageProducts(organizationId)) {
            throw new BusinessException("Access denied to manage products");
        }
    }

    private boolean canManageProducts(UUID organizationId) {
        IngobokaUserDetails user = SecurityUtils.currentUser();
        if (user.hasRole(RoleCodes.PLATFORM_ADMIN)) {
            return true;
        }
        return organizationId.equals(user.getOrganizationId())
                && (user.hasRole(RoleCodes.INSURER_PRODUCT_MANAGER)
                        || user.hasRole(RoleCodes.PARTNER_ADMIN));
    }

    private ProductResponse toProductResponse(InsuranceProduct product) {
        return ProductResponse.builder()
                .id(product.getId())
                .organizationId(product.getOrganizationId())
                .code(product.getCode())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .status(product.getStatus())
                .publishedAt(product.getPublishedAt())
                .createdAt(product.getCreatedAt())
                .build();
    }
}

package com.ingoboka_api.v1.enrollment.services;

import com.ingoboka_api.v1.common.enums.ProductStatus;
import com.ingoboka_api.v1.common.responses.RecommendedProductResponse;
import com.ingoboka_api.v1.product.models.InsuranceProduct;
import com.ingoboka_api.v1.product.models.ProductPlan;
import com.ingoboka_api.v1.product.repositories.InsuranceProductRepository;
import com.ingoboka_api.v1.product.repositories.ProductPlanRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NeedsAssessmentRecommendationService {

    private final InsuranceProductRepository productRepository;
    private final ProductPlanRepository productPlanRepository;

    public record RecommendationResult(
            int score,
            String guidance,
            List<String> recommendedCategories,
            List<RecommendedProductResponse> recommendedProducts) {}

    @Transactional(readOnly = true)
    public RecommendationResult recommend(
            String occupation, String incomeRange, Integer dependents, String primaryRisk) {
        int score = 50;
        int dependantCount = dependents != null ? dependents : 0;
        if (dependantCount > 2) {
            score += 15;
        }
        if (primaryRisk != null && !primaryRisk.isBlank()) {
            score += 10;
        }

        List<String> categories = resolveCategories(occupation, primaryRisk, dependantCount, incomeRange);
        final int assessmentScore = score;
        List<RecommendedProductResponse> recommendedProducts = productRepository
                .findByStatusOrderByPublishedAtDesc(ProductStatus.PUBLISHED)
                .stream()
                .filter(product -> categories.contains(product.getCategory()))
                .sorted(Comparator.comparingInt(product -> categoryPriority(categories, product.getCategory())))
                .limit(5)
                .map(product -> toRecommendedProduct(product, categories, assessmentScore))
                .toList();

        return new RecommendationResult(
                score,
                buildGuidance(occupation, dependantCount, categories),
                categories,
                recommendedProducts);
    }

    @Transactional(readOnly = true)
    public RecommendationResult recommendFromPreferences(Map<String, Object> preferences) {
        if (preferences == null || preferences.isEmpty()) {
            return new RecommendationResult(0, "", List.of(), List.of());
        }
        String occupation = stringValue(preferences.get("occupation"));
        String incomeRange = stringValue(preferences.get("incomeRange"));
        String primaryRisk = stringValue(preferences.get("primaryRisk"));
        Integer dependents = intValue(preferences.get("dependents"));
        if (dependents == null) {
            dependents = parseDependentsFromAnswers(preferences.get("answers"));
        }
        return recommend(occupation, incomeRange, dependents, primaryRisk);
    }

    private List<String> resolveCategories(
            String occupation, String primaryRisk, int dependantCount, String incomeRange) {
        Set<String> categories = new LinkedHashSet<>();

        if ("ACCIDENT".equalsIgnoreCase(primaryRisk) || "MOTO_RIDER".equalsIgnoreCase(occupation)) {
            categories.add("PERSONAL_ACCIDENT");
        }
        if ("FARMER".equalsIgnoreCase(occupation) || "VENDOR".equalsIgnoreCase(occupation)) {
            categories.add("PERSONAL_ACCIDENT");
            categories.add("HOSPITAL_CASH");
        }
        if ("OFFICE".equalsIgnoreCase(occupation)) {
            categories.add("HEALTH_MICRO");
            categories.add("HOSPITAL_CASH");
        }
        if (dependantCount >= 3) {
            categories.add("HEALTH_MICRO");
            categories.add("FUNERAL");
        } else if (dependantCount > 0) {
            categories.add("HEALTH_MICRO");
        }
        if ("HEALTH".equalsIgnoreCase(primaryRisk)) {
            categories.add("HEALTH_MICRO");
            categories.add("HOSPITAL_CASH");
        }
        if (incomeRange != null && (incomeRange.startsWith("5K") || "OVER_10K".equals(incomeRange))) {
            categories.add("HEALTH_MICRO");
        }

        if (categories.isEmpty()) {
            categories.add("PERSONAL_ACCIDENT");
            categories.add("HOSPITAL_CASH");
        }

        return new ArrayList<>(categories);
    }

    private int categoryPriority(List<String> categories, String category) {
        int index = categories.indexOf(category);
        return index >= 0 ? index : categories.size();
    }

    private RecommendedProductResponse toRecommendedProduct(
            InsuranceProduct product, List<String> categories, int assessmentScore) {
        var plans = productPlanRepository.findByProductIdAndStatus(product.getId(), ProductStatus.PUBLISHED);
        var startingPremium = plans.stream()
                .map(ProductPlan::getPremiumAmount)
                .min(Comparator.naturalOrder())
                .orElse(null);
        int categoryBoost = Math.max(0, categories.size() - categoryPriority(categories, product.getCategory())) * 5;
        return RecommendedProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .category(product.getCategory())
                .startingPremium(startingPremium)
                .currency("RWF")
                .matchScore(Math.min(100, assessmentScore + categoryBoost + 15))
                .reason("Matches " + product.getCategory().replace('_', ' ').toLowerCase())
                .build();
    }

    private String buildGuidance(String occupation, int dependantCount, List<String> categories) {
        if ("MOTO_RIDER".equalsIgnoreCase(occupation)) {
            return "Based on your work as a moto rider, personal accident cover should be your first priority.";
        }
        if (dependantCount >= 3) {
            return "With several dependants, consider health and family protection plans alongside accident cover.";
        }
        if (categories.contains("HEALTH_MICRO")) {
            return "Based on your profile, health micro cover is a strong fit alongside accident protection.";
        }
        return "Based on your profile, consider personal accident cover first.";
    }

    private String stringValue(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Integer parseDependentsFromAnswers(Object answersObj) {
        if (!(answersObj instanceof Map<?, ?> answers)) {
            return null;
        }
        Object raw = answers.get("dependents");
        if (raw == null) {
            return null;
        }
        String range = String.valueOf(raw);
        return switch (range) {
            case "0" -> 0;
            case "1-2" -> 2;
            case "3-4" -> 4;
            case "5+" -> 5;
            default -> intValue(raw);
        };
    }
}

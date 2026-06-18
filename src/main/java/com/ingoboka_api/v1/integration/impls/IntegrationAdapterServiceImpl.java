package com.ingoboka_api.v1.integration.impls;

import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.requests.InvokeIntegrationRequest;
import com.ingoboka_api.v1.common.responses.IntegrationAdapterResponse;
import com.ingoboka_api.v1.common.responses.IntegrationInvokeResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.util.PaginationUtils;
import com.ingoboka_api.v1.integration.models.IntegrationAdapter;
import com.ingoboka_api.v1.integration.repositories.IntegrationAdapterRepository;
import com.ingoboka_api.v1.integration.services.IntegrationAdapterService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IntegrationAdapterServiceImpl implements IntegrationAdapterService {

    private final IntegrationAdapterRepository integrationAdapterRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<IntegrationAdapterResponse> listAdapters(int page, int size) {
        Page<IntegrationAdapter> result =
                integrationAdapterRepository.findAllByOrderByCodeAsc(PaginationUtils.toPageable(page, size));
        return PageResponse.from(result.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public IntegrationInvokeResponse invoke(String code, InvokeIntegrationRequest request) {
        IntegrationAdapter adapter = integrationAdapterRepository
                .findByCode(code)
                .orElseThrow(() -> new BusinessException("Integration adapter not found"));
        if (!adapter.isEnabled()) {
            throw new BusinessException("Integration adapter is disabled");
        }

        Map<String, Object> payload = request.getPayload() != null ? request.getPayload() : Map.of();
        return IntegrationInvokeResponse.builder()
                .adapterCode(adapter.getCode())
                .status("SIMULATED_SUCCESS")
                .message("Sandbox adapter invoked successfully")
                .result(Map.of(
                        "adapterType", adapter.getAdapterType().name(),
                        "mode", "sandbox",
                        "echo", payload))
                .build();
    }

    private IntegrationAdapterResponse toResponse(IntegrationAdapter adapter) {
        return IntegrationAdapterResponse.builder()
                .id(adapter.getId())
                .code(adapter.getCode())
                .adapterType(adapter.getAdapterType())
                .name(adapter.getName())
                .enabled(adapter.isEnabled())
                .configJson(adapter.getConfigJson())
                .createdAt(adapter.getCreatedAt())
                .updatedAt(adapter.getUpdatedAt())
                .build();
    }
}

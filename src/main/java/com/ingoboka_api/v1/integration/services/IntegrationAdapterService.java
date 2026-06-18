package com.ingoboka_api.v1.integration.services;

import com.ingoboka_api.v1.common.requests.InvokeIntegrationRequest;
import com.ingoboka_api.v1.common.responses.IntegrationAdapterResponse;
import com.ingoboka_api.v1.common.responses.IntegrationInvokeResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;

public interface IntegrationAdapterService {

    PageResponse<IntegrationAdapterResponse> listAdapters(int page, int size);

    IntegrationInvokeResponse invoke(String code, InvokeIntegrationRequest request);
}

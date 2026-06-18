package com.ingoboka_api.v1.audit.services;

import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.AuditLogResponse;
import com.ingoboka_api.v1.common.requests.SubmitDataSubjectRequest;
import com.ingoboka_api.v1.common.requests.ResolveDataSubjectRequest;
import com.ingoboka_api.v1.common.responses.DataSubjectRequestResponse;
import java.util.UUID;

public interface AuditComplianceService {

    void log(String action, String entityType, UUID entityId, String summary);

    PageResponse<AuditLogResponse> listAuditLogs(int page, int size);

    PageResponse<AuditLogResponse> listTenantAuditLogs(int page, int size);

    DataSubjectRequestResponse submitDataSubjectRequest(SubmitDataSubjectRequest request);

    PageResponse<DataSubjectRequestResponse> listMyDataSubjectRequests(int page, int size);

    PageResponse<DataSubjectRequestResponse> listAllDataSubjectRequests(int page, int size);

    DataSubjectRequestResponse resolveDataSubjectRequest(ResolveDataSubjectRequest request);
}

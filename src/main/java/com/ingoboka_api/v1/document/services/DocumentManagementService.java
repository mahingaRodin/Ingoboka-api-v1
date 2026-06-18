package com.ingoboka_api.v1.document.services;

import com.ingoboka_api.v1.common.enums.DocumentEntityType;
import com.ingoboka_api.v1.common.requests.RegisterDocumentRequest;
import com.ingoboka_api.v1.common.responses.DocumentResponse;
import com.ingoboka_api.v1.common.responses.DownloadUrlResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.UploadUrlResponse;
import java.util.UUID;

public interface DocumentManagementService {

    UploadUrlResponse requestUploadUrl(String documentType, String mimeType);

    DocumentResponse registerDocument(RegisterDocumentRequest request);

    DocumentResponse getDocument(UUID documentId);

    DownloadUrlResponse getDownloadUrl(UUID documentId);

    PageResponse<DocumentResponse> listByEntity(DocumentEntityType entityType, UUID entityId, int page, int size);

    PageResponse<DocumentResponse> listTenantDocuments(int page, int size);

    DocumentResponse markScannedClean(UUID documentId);
}

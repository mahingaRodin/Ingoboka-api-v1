package com.ingoboka_api.v1.document.impls;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.ingoboka_api.v1.audit.services.AuditComplianceService;
import com.ingoboka_api.v1.common.config.MinioProperties;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.document.repositories.DocumentRegistryRepository;
import com.ingoboka_api.v1.document.services.DocumentStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentManagementServiceImplTest {

    private DocumentManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DocumentManagementServiceImpl(
                mock(DocumentRegistryRepository.class),
                mock(AuditComplianceService.class),
                mock(DocumentStorageService.class),
                new MinioProperties());
    }

    @Test
    void rejectsClaimEvidencePresignedUploadUrlRequests() {
        assertThatThrownBy(() -> service.requestUploadUrl("CLAIM_EVIDENCE", "application/pdf"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("multipart");
    }
}

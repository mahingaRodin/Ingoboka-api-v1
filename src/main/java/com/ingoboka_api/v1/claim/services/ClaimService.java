package com.ingoboka_api.v1.claim.services;

import com.ingoboka_api.v1.common.enums.ClaimStatus;
import com.ingoboka_api.v1.common.requests.AttachClaimDocumentRequest;
import com.ingoboka_api.v1.common.requests.CreateClaimAppealRequest;
import com.ingoboka_api.v1.common.requests.CreateClaimRequest;
import com.ingoboka_api.v1.common.requests.RecordClaimDecisionRequest;
import com.ingoboka_api.v1.common.requests.UpdateClaimRequest;
import com.ingoboka_api.v1.common.requests.UpdateClaimStatusRequest;
import com.ingoboka_api.v1.common.responses.ClaimAppealResponse;
import com.ingoboka_api.v1.common.responses.ClaimsBreakdownResponse;
import com.ingoboka_api.v1.common.responses.ClaimResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ClaimService {

    ClaimResponse createClaim(CreateClaimRequest request);

    ClaimResponse submitClaim(UUID claimId);

    ClaimResponse cancelClaim(UUID claimId);

    ClaimResponse getClaim(UUID claimId);

    PageResponse<ClaimResponse> listMyClaims(int page, int size);

    PageResponse<ClaimResponse> listTenantClaims(ClaimStatus status, int page, int size);

    PageResponse<ClaimResponse> listTenantClaimsFiltered(
            ClaimStatus status,
            String search,
            String province,
            String district,
            String sortBy,
            String sortDir,
            int page,
            int size);

    ClaimResponse createTenantClaim(CreateClaimRequest request);

    ClaimResponse updateClaim(UUID claimId, UpdateClaimRequest request);

    ClaimResponse updateStatus(UUID claimId, UpdateClaimStatusRequest request);

    ClaimResponse recordDecision(UUID claimId, RecordClaimDecisionRequest request);

    void attachDocument(UUID claimId, AttachClaimDocumentRequest request);

    void uploadDocuments(UUID claimId, MultipartFile[] files);

    ClaimAppealResponse createAppeal(UUID claimId, CreateClaimAppealRequest request);

    ClaimsBreakdownResponse getClaimsBreakdown();
}

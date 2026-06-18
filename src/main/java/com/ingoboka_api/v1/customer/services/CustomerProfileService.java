package com.ingoboka_api.v1.customer.services;

import com.ingoboka_api.v1.common.enums.ConsentType;
import com.ingoboka_api.v1.common.requests.CreateDependantRequest;
import com.ingoboka_api.v1.common.requests.GrantConsentRequest;
import com.ingoboka_api.v1.common.requests.ReviewKycRequest;
import com.ingoboka_api.v1.common.requests.UpdateCitizenProfileRequest;
import com.ingoboka_api.v1.common.responses.CitizenProfileResponse;
import com.ingoboka_api.v1.common.responses.ConsentResponse;
import com.ingoboka_api.v1.common.responses.DependantResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.customer.models.CitizenProfile;
import com.ingoboka_api.v1.customer.models.Consent;
import java.util.UUID;

public interface CustomerProfileService {

    CitizenProfileResponse getMyProfile();

    CitizenProfileResponse updateMyProfile(UpdateCitizenProfileRequest request);

    PageResponse<DependantResponse> listMyDependants(int page, int size);

    DependantResponse addDependant(CreateDependantRequest request);

    void removeDependant(UUID dependantId);

    ConsentResponse grantConsent(GrantConsentRequest request, String ipAddress);

    PageResponse<ConsentResponse> listMyConsents(int page, int size);

    void revokeConsent(ConsentType consentType);

    CitizenProfile requireProfileForUser(UUID userId);

    Consent requireActiveConsent(UUID userId, UUID consentId);

    CitizenProfileResponse reviewKyc(ReviewKycRequest request);
}

package com.ingoboka_api.v1.enrollment.services;

import com.ingoboka_api.v1.common.enums.ApplicationStatus;
import com.ingoboka_api.v1.common.requests.GenerateQuoteRequest;
import com.ingoboka_api.v1.common.requests.ReviewApplicationRequest;
import com.ingoboka_api.v1.common.requests.SubmitApplicationRequest;
import com.ingoboka_api.v1.common.responses.ApplicationResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.QuoteResponse;
import java.util.UUID;

public interface EnrollmentService {

    QuoteResponse generateQuote(GenerateQuoteRequest request);

    ApplicationResponse submitApplication(SubmitApplicationRequest request);

    PageResponse<ApplicationResponse> listMyApplications(int page, int size);

    ApplicationResponse getApplication(UUID applicationId);

    PageResponse<ApplicationResponse> listTenantApplications(ApplicationStatus status, int page, int size);

    ApplicationResponse reviewApplication(UUID applicationId, ReviewApplicationRequest request);
}

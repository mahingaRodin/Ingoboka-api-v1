package com.ingoboka_api.v1.enrollment.services;

import com.ingoboka_api.v1.common.enums.ApplicationStatus;
import com.ingoboka_api.v1.common.requests.GenerateQuoteRequest;
import com.ingoboka_api.v1.common.requests.ReviewApplicationRequest;
import com.ingoboka_api.v1.common.requests.SubmitApplicationRequest;
import com.ingoboka_api.v1.common.responses.ApplicationResponse;
import com.ingoboka_api.v1.common.responses.QuoteResponse;
import java.util.List;
import java.util.UUID;

public interface EnrollmentService {

    QuoteResponse generateQuote(GenerateQuoteRequest request);

    ApplicationResponse submitApplication(SubmitApplicationRequest request);

    List<ApplicationResponse> listMyApplications();

    ApplicationResponse getApplication(UUID applicationId);

    List<ApplicationResponse> listTenantApplications(ApplicationStatus status);

    ApplicationResponse reviewApplication(UUID applicationId, ReviewApplicationRequest request);
}

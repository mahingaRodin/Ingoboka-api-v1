package com.ingoboka_api.v1.identity.services;

import com.ingoboka_api.v1.common.requests.*;
import com.ingoboka_api.v1.common.responses.AuthTokensResponse;

public interface AuthService {
    void signup(SignupRequest request);
    AuthTokensResponse login(LoginRequest request);
    void requestEmailVerification(EmailRequest request);
    void confirmEmailVerification(VerifyEmailConfirmRequest request);
    void verifyOtp(VerifyOtpRequest request);
    void resendOtp(ResendOtpRequest request);
    AuthTokensResponse refresh(RefreshTokenRequest request);
    void requestPasswordReset(EmailRequest request);
    void resetPassword(PasswordResetRequest request);
    void activateAccount(ActivateAccountRequest request);
}

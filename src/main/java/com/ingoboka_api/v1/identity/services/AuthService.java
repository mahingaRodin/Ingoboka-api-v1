package com.ingoboka_api.v1.identity.services;

import com.ingoboka_api.v1.common.requests.*;
import com.ingoboka_api.v1.common.responses.AuthTokensResponse;
import com.ingoboka_api.v1.common.responses.PasswordResetTokenResponse;
import org.springframework.transaction.annotation.Transactional;

public interface AuthService {
    void signup(SignupRequest request);

    void register(RegisterRequest request);

    AuthTokensResponse login(LoginRequest request);

    void requestEmailVerification(EmailRequest request);

    void confirmEmailVerification(VerifyEmailConfirmRequest request);

    AuthTokensResponse confirmEmailOtp(VerifyEmailOtpRequest request);

    void resendEmailVerificationOtp();

    AuthTokensResponse verifyOtp(VerifyOtpRequest request);

    void resendOtp(ResendOtpRequest request);

    AuthTokensResponse refresh(RefreshTokenRequest request);

    void logout(LogoutRequest request);

    void requestPasswordReset(EmailRequest request);

    PasswordResetTokenResponse verifyPasswordResetOtp(VerifyPasswordResetOtpRequest request);

    void resendPasswordResetOtp(EmailRequest request);

    void resetPassword(PasswordResetRequest request);

    void activateAccount(ActivateAccountRequest request);

    AuthTokensResponse changePassword(ChangePasswordRequest request);
}

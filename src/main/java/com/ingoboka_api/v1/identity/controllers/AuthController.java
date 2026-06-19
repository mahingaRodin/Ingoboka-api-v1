package com.ingoboka_api.v1.identity.controllers;

import com.ingoboka_api.v1.common.config.OtpDeliveryProperties;
import com.ingoboka_api.v1.common.enums.OtpDeliveryChannel;
import com.ingoboka_api.v1.common.requests.*;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.AuthTokensResponse;
import com.ingoboka_api.v1.common.responses.OtpDeliveryConfigResponse;
import com.ingoboka_api.v1.identity.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Identity and access endpoints")
public class AuthController {

    private final AuthService authService;
    private final OtpDeliveryProperties otpDeliveryProperties;

    @GetMapping("/otp-delivery-config")
    @Operation(
            summary = "OTP delivery mode",
            description = "Public config so the frontend knows whether OTP is sent by email, SMS, or server logs")
    public ApiResponse<OtpDeliveryConfigResponse> otpDeliveryConfig() {
        return ApiResponse.ok("OTP delivery configuration", OtpDeliveryConfigResponse.from(otpDeliveryProperties));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register citizen", description = "Phone-first registration; OTP channel depends on server config")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.ok(registrationSuccessMessage(), null);
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Signup user (email)", description = "Legacy email signup for staff users")
    public ApiResponse<Void> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ApiResponse.ok(registrationSuccessMessage(), null);
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Login with phone or email + password")
    public ApiResponse<AuthTokensResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Login successful", authService.login(request));
    }

    @PostMapping("/verify-email/request")
    @Operation(summary = "Request email verification")
    public ApiResponse<Void> requestEmailVerification(@Valid @RequestBody EmailRequest request) {
        authService.requestEmailVerification(request);
        return ApiResponse.ok("If the account exists, a verification email has been sent.", null);
    }

    @PostMapping("/verify-email/confirm")
    @Operation(summary = "Confirm email verification")
    public ApiResponse<Void> confirmEmailVerification(@Valid @RequestBody VerifyEmailConfirmRequest request) {
        authService.confirmEmailVerification(request);
        return ApiResponse.ok("Email verified successfully.", null);
    }

    @PostMapping("/forgot-password/request")
    @Operation(summary = "Request password reset")
    public ApiResponse<Void> requestPasswordReset(@Valid @RequestBody EmailRequest request) {
        authService.requestPasswordReset(request);
        return ApiResponse.ok("If the account exists, a password reset email has been sent.", null);
    }

    @PostMapping("/forgot-password/reset")
    @Operation(summary = "Reset password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok("Password reset successful.", null);
    }

    @PostMapping("/activate-account")
    @Operation(summary = "Activate admin-created account")
    public ApiResponse<Void> activateAccount(@Valid @RequestBody ActivateAccountRequest request) {
        authService.activateAccount(request);
        return ApiResponse.ok("Account activated successfully.", null);
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify signup OTP", description = "Returns access tokens and user profile (auto-login)")
    public ApiResponse<AuthTokensResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ApiResponse.ok("Account verified successfully.", authService.verifyOtp(request));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend signup OTP")
    public ApiResponse<Void> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request);
        return ApiResponse.ok(resendOtpMessage(), null);
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Required after logging in with a temporary admin-provided password")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<AuthTokensResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ApiResponse.ok("Password changed successfully", authService.changePassword(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ApiResponse<AuthTokensResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok("Token refreshed", authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revokes refresh token when provided")
    public ApiResponse<Void> logout(@RequestBody(required = false) LogoutRequest request) {
        authService.logout(request != null ? request : new LogoutRequest());
        return ApiResponse.ok("Logged out", null);
    }

    private String registrationSuccessMessage() {
        return switch (otpDeliveryProperties.getDeliveryChannel()) {
            case EMAIL -> "Registration successful. Check your email for the 6-digit verification code.";
            case SMS -> "Registration successful. Check your phone for the verification code by SMS.";
            case LOG -> "Registration successful. Ask your administrator for the OTP from API logs (dev mode).";
        };
    }

    private String resendOtpMessage() {
        return switch (otpDeliveryProperties.getDeliveryChannel()) {
            case EMAIL -> "If the account exists, a new verification code has been sent to your email.";
            case SMS -> "If the account exists, a new verification code has been sent to your phone.";
            case LOG -> "If the account exists, a new OTP was written to the API logs (dev mode).";
        };
    }
}

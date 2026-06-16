package com.ingoboka_api.v1.identity.web;

import com.ingoboka_api.v1.common.dto.ApiResponse;
import com.ingoboka_api.v1.identity.dto.ActivateAccountRequest;
import com.ingoboka_api.v1.identity.dto.AuthTokensResponse;
import com.ingoboka_api.v1.identity.dto.EmailRequest;
import com.ingoboka_api.v1.identity.dto.LoginRequest;
import com.ingoboka_api.v1.identity.dto.PasswordResetRequest;
import com.ingoboka_api.v1.identity.dto.SignupRequest;
import com.ingoboka_api.v1.identity.dto.VerifyEmailConfirmRequest;
import com.ingoboka_api.v1.identity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Identity and access endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Signup user", description = "Public citizen self-registration. Assigns CITIZEN role.")
    public ApiResponse<Void> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ApiResponse.ok("Signup successful. Please verify your email.", null);
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Public login for all active user roles.")
    public ApiResponse<AuthTokensResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Login successful", authService.login(request));
    }

    @PostMapping("/verify-email/request")
    @Operation(summary = "Request email verification", description = "Public endpoint to resend email verification token.")
    public ApiResponse<Void> requestEmailVerification(@Valid @RequestBody EmailRequest request) {
        authService.requestEmailVerification(request);
        return ApiResponse.ok("If the account exists, a verification email has been sent.", null);
    }

    @PostMapping("/verify-email/confirm")
    @Operation(summary = "Confirm email verification", description = "Public endpoint to confirm email with token.")
    public ApiResponse<Void> confirmEmailVerification(@Valid @RequestBody VerifyEmailConfirmRequest request) {
        authService.confirmEmailVerification(request);
        return ApiResponse.ok("Email verified successfully.", null);
    }

    @PostMapping("/forgot-password/request")
    @Operation(summary = "Request password reset", description = "Public endpoint to request a password reset token.")
    public ApiResponse<Void> requestPasswordReset(@Valid @RequestBody EmailRequest request) {
        authService.requestPasswordReset(request);
        return ApiResponse.ok("If the account exists, a password reset email has been sent.", null);
    }

    @PostMapping("/forgot-password/reset")
    @Operation(summary = "Reset password", description = "Public endpoint to reset password using token.")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok("Password reset successful.", null);
    }

    @PostMapping("/activate-account")
    @Operation(
            summary = "Activate admin-created account",
            description = "Public endpoint for staff accounts created by PLATFORM_ADMIN or PARTNER_ADMIN.")
    public ApiResponse<Void> activateAccount(@Valid @RequestBody ActivateAccountRequest request) {
        authService.activateAccount(request);
        return ApiResponse.ok("Account activated successfully.", null);
    }
}

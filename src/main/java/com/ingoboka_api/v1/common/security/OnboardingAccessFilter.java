package com.ingoboka_api.v1.common.security;

import com.ingoboka_api.v1.common.enums.UserStatus;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.identity.models.RoleCodes;
import com.ingoboka_api.v1.identity.models.User;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class OnboardingAccessFilter extends OncePerRequestFilter {

    private static final Set<String> PASSWORD_CHANGE_ALLOWED_PREFIXES = Set.of(
            "/api/v1/auth/change-password",
            "/api/v1/auth/logout",
            "/api/v1/auth/refresh",
            "/api/v1/auth/otp-delivery-config");

    private static final Set<String> EMAIL_VERIFICATION_ALLOWED_PREFIXES = Set.of(
            "/api/v1/auth/verify-email/request",
            "/api/v1/auth/verify-email/confirm",
            "/api/v1/auth/logout",
            "/api/v1/auth/refresh");

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityUtils.isAuthenticated()) {
            User user = userRepository
                    .findWithDetailsById(SecurityUtils.currentUser().getUserId())
                    .orElse(null);
            if (user != null && isStaff(user)) {
                String path = request.getRequestURI();
                if (user.isMustChangePassword() || user.getStatus() == UserStatus.PENDING_PASSWORD_CHANGE) {
                    if (!isAllowed(path, PASSWORD_CHANGE_ALLOWED_PREFIXES)) {
                        writeBlocked(response, "MUST_CHANGE_PASSWORD", "You must change your temporary password first");
                        return;
                    }
                } else if (!user.isEmailVerified() || user.getStatus() == UserStatus.PENDING_EMAIL_VERIFICATION) {
                    if (!isAllowed(path, EMAIL_VERIFICATION_ALLOWED_PREFIXES)) {
                        writeBlocked(
                                response,
                                "EMAIL_VERIFICATION_REQUIRED",
                                "Verify your email address to activate your account");
                        return;
                    }
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isStaff(User user) {
        return user.getRoles().stream()
                .noneMatch(role -> RoleCodes.CITIZEN.equals(role.getCode())
                        || RoleCodes.BENEFICIARY.equals(role.getCode()));
    }

    private boolean isAllowed(String path, Set<String> allowedPrefixes) {
        return allowedPrefixes.stream().anyMatch(path::startsWith);
    }

    private void writeBlocked(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter()
                .write("{\"success\":false,\"message\":\"" + message + "\",\"code\":\"" + code + "\"}");
    }
}

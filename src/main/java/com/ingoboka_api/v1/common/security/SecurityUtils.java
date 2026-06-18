package com.ingoboka_api.v1.common.security;

import com.ingoboka_api.v1.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static IngobokaUserDetails currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof IngobokaUserDetails details)) {
            throw new BusinessException("Unauthorized");
        }
        return details;
    }

    public static boolean isPlatformAdmin() {
        return currentUser().hasRole("PLATFORM_ADMIN");
    }

    public static boolean isPartnerAdmin() {
        return currentUser().hasRole("PARTNER_ADMIN");
    }
}

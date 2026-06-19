package com.ingoboka_api.v1.common.security;

import com.ingoboka_api.v1.identity.models.User;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class IngobokaUserDetails implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String passwordHash;
    private final UUID organizationId;
    private final Set<String> roleCodes;
    private final boolean enabled;
    private final boolean accountNonLocked;

    public IngobokaUserDetails(User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.organizationId = user.getOrganization() != null ? user.getOrganization().getId() : null;
        this.roleCodes = user.getRoles().stream().map(role -> role.getCode()).collect(Collectors.toSet());
        this.enabled = switch (user.getStatus()) {
            case ACTIVE, PENDING_PASSWORD_CHANGE, PENDING_EMAIL_VERIFICATION -> true;
            case PENDING_ACTIVATION, LOCKED, DISABLED -> false;
        };
        this.accountNonLocked = user.getStatus() != com.ingoboka_api.v1.common.enums.UserStatus.LOCKED;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roleCodes.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    public boolean hasRole(String roleCode) {
        return roleCodes.contains(roleCode);
    }
}

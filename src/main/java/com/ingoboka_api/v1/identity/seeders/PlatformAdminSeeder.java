package com.ingoboka_api.v1.identity.bootstrap;

import com.ingoboka_api.v1.common.enums.OrganizationStatus;
import com.ingoboka_api.v1.common.enums.UserStatus;
import com.ingoboka_api.v1.identity.domain.Organization;
import com.ingoboka_api.v1.identity.domain.Role;
import com.ingoboka_api.v1.identity.domain.RoleCodes;
import com.ingoboka_api.v1.identity.domain.User;
import com.ingoboka_api.v1.identity.repositories.OrganizationRepository;
import com.ingoboka_api.v1.identity.repositories.RoleRepository;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformAdminSeeder implements ApplicationRunner {

    private static final UUID PLATFORM_ORG_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ingoboka.seed.platform-admin.enabled:true}")
    private boolean seedEnabled;

    @Value("${ingoboka.seed.platform-admin.email:agressive.one04@gmail.com}")
    private String adminEmail;

    @Value("${ingoboka.seed.platform-admin.phone:+250794415318}")
    private String adminPhone;

    @Value("${ingoboka.seed.platform-admin.password:admin@123}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            return;
        }

        if (userRepository.existsByEmailIgnoreCase(adminEmail)) {
            return;
        }

        Organization platformOrg = organizationRepository
                .findById(PLATFORM_ORG_ID)
                .orElseGet(this::createPlatformOrganization);

        Role platformAdminRole = roleRepository
                .findByCode(RoleCodes.PLATFORM_ADMIN)
                .orElseThrow(() -> new IllegalStateException("PLATFORM_ADMIN role missing"));

        Instant now = Instant.now();
        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setOrganization(platformOrg);
        admin.setEmail(adminEmail.trim().toLowerCase());
        admin.setPhoneNumber(adminPhone);
        admin.setFirstName("System");
        admin.setLastName("Administrator");
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setStatus(UserStatus.ACTIVE);
        admin.setEmailVerified(true);
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        admin.getRoles().add(platformAdminRole);

        userRepository.save(admin);
        log.info("Seeded platform administrator {}", adminEmail);
    }

    private Organization createPlatformOrganization() {
        Instant now = Instant.now();
        Organization organization = new Organization();
        organization.setId(PLATFORM_ORG_ID);
        organization.setName("Ingoboka Platform");
        organization.setCode("INGOBOKA_PLATFORM");
        organization.setType(com.ingoboka_api.v1.common.enums.OrganizationType.PLATFORM);
        organization.setStatus(OrganizationStatus.ACTIVE);
        organization.setCreatedAt(now);
        organization.setUpdatedAt(now);
        return organizationRepository.save(organization);
    }
}

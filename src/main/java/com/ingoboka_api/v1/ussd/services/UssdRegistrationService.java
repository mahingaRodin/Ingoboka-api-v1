package com.ingoboka_api.v1.ussd.services;

import com.ingoboka_api.v1.common.enums.ConsentType;
import com.ingoboka_api.v1.common.enums.KycStatus;
import com.ingoboka_api.v1.common.enums.UserStatus;
import com.ingoboka_api.v1.common.enums.UssdRegistrationType;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.util.PhoneNumberUtils;
import com.ingoboka_api.v1.common.util.TemporaryPasswordGenerator;
import com.ingoboka_api.v1.customer.models.CitizenProfile;
import com.ingoboka_api.v1.customer.models.Consent;
import com.ingoboka_api.v1.customer.repositories.CitizenProfileRepository;
import com.ingoboka_api.v1.customer.repositories.ConsentRepository;
import com.ingoboka_api.v1.identity.models.Role;
import com.ingoboka_api.v1.identity.models.RoleCodes;
import com.ingoboka_api.v1.identity.models.User;
import com.ingoboka_api.v1.identity.repositories.RoleRepository;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import com.ingoboka_api.v1.messaging.services.SmsDeliveryService;
import com.ingoboka_api.v1.ussd.menu.UssdMessages;
import com.ingoboka_api.v1.ussd.models.UssdRegistration;
import com.ingoboka_api.v1.ussd.repositories.UssdRegistrationRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UssdRegistrationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UssdRegistrationRepository ussdRegistrationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CitizenProfileRepository citizenProfileRepository;
    private final ConsentRepository consentRepository;
    private final PasswordEncoder passwordEncoder;
    private final SmsDeliveryService smsDeliveryService;

    @Transactional(readOnly = true)
    public Optional<UssdRegistration> findByPhone(String phone) {
        return ussdRegistrationRepository.findByPhoneNumber(PhoneNumberUtils.normalizeRwanda(phone));
    }

    @Transactional(readOnly = true)
    public boolean isRegistered(String phone) {
        return ussdRegistrationRepository.existsByPhoneNumber(PhoneNumberUtils.normalizeRwanda(phone));
    }

    @Transactional
    public UssdRegistration register(
            String rawPhone,
            UssdRegistrationType type,
            String fullName,
            String businessName,
            String district,
            String language) {
        String phone = PhoneNumberUtils.normalizeRwanda(rawPhone);
        if (ussdRegistrationRepository.existsByPhoneNumber(phone)) {
            throw new BusinessException("Phone number is already registered on USSD");
        }
        if (userRepository.existsByPhoneNumber(phone)) {
            throw new BusinessException("Phone number is already registered");
        }

        Instant now = Instant.now();
        User user = createCitizenUser(phone, fullName, now);
        CitizenProfile profile = createProfile(user.getId(), district, language, type, now);
        grantDataConsent(user.getId(), now);

        String reference = generateReference();
        UssdRegistration registration = new UssdRegistration();
        registration.setId(UUID.randomUUID());
        registration.setPhoneNumber(phone);
        registration.setRegistrationType(type);
        registration.setFullName(fullName.trim());
        registration.setBusinessName(
                type == UssdRegistrationType.BUSINESS && businessName != null
                        ? businessName.trim()
                        : null);
        registration.setDistrict(district != null ? district.trim() : null);
        registration.setLanguage(language != null ? language : "rw");
        registration.setUserId(user.getId());
        registration.setReferenceCode(reference);
        registration.setCreatedAt(now);
        ussdRegistrationRepository.save(registration);

        String displayName = type == UssdRegistrationType.BUSINESS && businessName != null
                ? businessName.trim()
                : fullName.trim();
        smsDeliveryService.send(
                phone,
                UssdMessages.smsRegistrationBody(
                        language,
                        displayName,
                        type.name(),
                        district != null ? district.trim() : "-",
                        phone,
                        reference));

        // Keep profile occupation hint for NFIR informal-worker reporting later
        if (type == UssdRegistrationType.BUSINESS) {
            profile.setOccupation("USSD Business");
            citizenProfileRepository.save(profile);
        } else {
            profile.setOccupation("USSD Family");
            citizenProfileRepository.save(profile);
        }

        return registration;
    }

    private User createCitizenUser(String phone, String fullName, Instant now) {
        Role citizenRole = roleRepository
                .findByCode(RoleCodes.CITIZEN)
                .orElseThrow(() -> new BusinessException("Citizen role is not configured"));

        String[] parts = splitName(fullName);
        String email = phone.replace("+", "") + "@ussd.ingoboka.rw";

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email.toLowerCase(Locale.ROOT));
        user.setPhoneNumber(phone);
        user.setFirstName(parts[0]);
        user.setLastName(parts[1]);
        user.setPasswordHash(passwordEncoder.encode(TemporaryPasswordGenerator.generate(12)));
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setPhoneVerified(true);
        user.setMustChangePassword(false);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.getRoles().add(citizenRole);
        return userRepository.save(user);
    }

    private CitizenProfile createProfile(
            UUID userId, String district, String language, UssdRegistrationType type, Instant now) {
        CitizenProfile profile = new CitizenProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(userId);
        profile.setDateOfBirth(LocalDate.now().minusYears(30));
        profile.setDistrict(district);
        profile.setCountry("RW");
        profile.setPreferredLanguage(language != null ? language : "rw");
        profile.setKycStatus(KycStatus.PENDING);
        profile.setOccupation(type == UssdRegistrationType.BUSINESS ? "Business" : "Family");
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        return citizenProfileRepository.save(profile);
    }

    private void grantDataConsent(UUID userId, Instant now) {
        Consent consent = new Consent();
        consent.setId(UUID.randomUUID());
        consent.setUserId(userId);
        consent.setConsentType(ConsentType.DATA_PROCESSING);
        consent.setVersion("ussd-1.0");
        consent.setGranted(true);
        consent.setGrantedAt(now);
        consent.setCreatedAt(now);
        consentRepository.save(consent);
    }

    private String generateReference() {
        String ref;
        do {
            ref = "USSD" + String.format("%06d", RANDOM.nextInt(1_000_000));
        } while (ussdRegistrationRepository.existsByReferenceCode(ref));
        return ref;
    }

    private static String[] splitName(String fullName) {
        String trimmed = fullName.trim();
        int space = trimmed.indexOf(' ');
        if (space < 0) {
            return new String[] {trimmed, trimmed};
        }
        return new String[] {trimmed.substring(0, space), trimmed.substring(space + 1).trim()};
    }
}

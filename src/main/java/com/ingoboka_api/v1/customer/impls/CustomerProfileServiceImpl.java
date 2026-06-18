package com.ingoboka_api.v1.customer.impls;

import com.ingoboka_api.v1.common.enums.ConsentType;
import com.ingoboka_api.v1.common.enums.KycStatus;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.requests.CreateDependantRequest;
import com.ingoboka_api.v1.common.requests.GrantConsentRequest;
import com.ingoboka_api.v1.common.requests.UpdateCitizenProfileRequest;
import com.ingoboka_api.v1.common.responses.CitizenProfileResponse;
import com.ingoboka_api.v1.common.responses.ConsentResponse;
import com.ingoboka_api.v1.common.responses.DependantResponse;
import com.ingoboka_api.v1.common.security.IngobokaUserDetails;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.common.util.HashUtils;
import com.ingoboka_api.v1.customer.models.CitizenProfile;
import com.ingoboka_api.v1.customer.models.Consent;
import com.ingoboka_api.v1.customer.models.Dependant;
import com.ingoboka_api.v1.customer.repositories.CitizenProfileRepository;
import com.ingoboka_api.v1.customer.repositories.ConsentRepository;
import com.ingoboka_api.v1.customer.repositories.DependantRepository;
import com.ingoboka_api.v1.customer.services.CustomerProfileService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerProfileServiceImpl implements CustomerProfileService {

    private final CitizenProfileRepository citizenProfileRepository;
    private final DependantRepository dependantRepository;
    private final ConsentRepository consentRepository;

    @Override
    @Transactional(readOnly = true)
    public CitizenProfileResponse getMyProfile() {
        return toResponse(requireMyProfile());
    }

    @Override
    @Transactional
    public CitizenProfileResponse updateMyProfile(UpdateCitizenProfileRequest request) {
        IngobokaUserDetails user = SecurityUtils.currentUser();
        CitizenProfile profile = citizenProfileRepository
                .findByUserId(user.getUserId())
                .orElseGet(() -> createEmptyProfile(user.getUserId()));

        if (request.getNationalId() != null && !request.getNationalId().isBlank()) {
            String hash = HashUtils.sha256(request.getNationalId());
            if (hash != null
                    && citizenProfileRepository.existsByNationalIdHash(hash)
                    && !hash.equals(profile.getNationalIdHash())) {
                throw new BusinessException("National ID is already registered");
            }
            profile.setNationalIdHash(hash);
        }

        profile.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        if (request.getAddressLine() != null) {
            profile.setAddressLine(request.getAddressLine());
        }
        if (request.getDistrict() != null) {
            profile.setDistrict(request.getDistrict());
        }
        if (request.getCountry() != null) {
            profile.setCountry(request.getCountry());
        }
        if (request.getOccupation() != null) {
            profile.setOccupation(request.getOccupation());
        }
        if (request.getPreferredLanguage() != null) {
            profile.setPreferredLanguage(request.getPreferredLanguage());
        }
        profile.setUpdatedAt(Instant.now());
        return toResponse(citizenProfileRepository.save(profile));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DependantResponse> listMyDependants() {
        CitizenProfile profile = requireMyProfile();
        return dependantRepository.findByCitizenProfileIdOrderByCreatedAtAsc(profile.getId()).stream()
                .map(this::toDependantResponse)
                .toList();
    }

    @Override
    @Transactional
    public DependantResponse addDependant(CreateDependantRequest request) {
        CitizenProfile profile = requireMyProfile();
        Instant now = Instant.now();
        Dependant dependant = new Dependant();
        dependant.setId(UUID.randomUUID());
        dependant.setCitizenProfileId(profile.getId());
        dependant.setFirstName(request.getFirstName());
        dependant.setLastName(request.getLastName());
        dependant.setRelationship(request.getRelationship());
        dependant.setDateOfBirth(request.getDateOfBirth());
        if (request.getNationalId() != null && !request.getNationalId().isBlank()) {
            dependant.setNationalIdHash(HashUtils.sha256(request.getNationalId()));
        }
        dependant.setCreatedAt(now);
        dependant.setUpdatedAt(now);
        return toDependantResponse(dependantRepository.save(dependant));
    }

    @Override
    @Transactional
    public void removeDependant(UUID dependantId) {
        CitizenProfile profile = requireMyProfile();
        Dependant dependant = dependantRepository
                .findById(dependantId)
                .orElseThrow(() -> new BusinessException("Dependant not found"));
        if (!profile.getId().equals(dependant.getCitizenProfileId())) {
            throw new BusinessException("Access denied");
        }
        dependantRepository.delete(dependant);
    }

    @Override
    @Transactional
    public ConsentResponse grantConsent(GrantConsentRequest request, String ipAddress) {
        IngobokaUserDetails user = SecurityUtils.currentUser();
        consentRepository
                .findByUserIdAndConsentTypeAndGrantedTrueAndRevokedAtIsNull(
                        user.getUserId(), request.getConsentType())
                .ifPresent(existing -> {
                    existing.setGranted(false);
                    existing.setRevokedAt(Instant.now());
                    consentRepository.save(existing);
                });

        Instant now = Instant.now();
        Consent consent = new Consent();
        consent.setId(UUID.randomUUID());
        consent.setUserId(user.getUserId());
        consent.setConsentType(request.getConsentType());
        consent.setVersion(request.getVersion());
        consent.setGranted(true);
        consent.setGrantedAt(now);
        consent.setIpAddress(ipAddress);
        consent.setCreatedAt(now);
        return toConsentResponse(consentRepository.save(consent));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsentResponse> listMyConsents() {
        return consentRepository
                .findByUserIdOrderByGrantedAtDesc(SecurityUtils.currentUser().getUserId())
                .stream()
                .map(this::toConsentResponse)
                .toList();
    }

    @Override
    @Transactional
    public void revokeConsent(ConsentType consentType) {
        Consent consent = consentRepository
                .findByUserIdAndConsentTypeAndGrantedTrueAndRevokedAtIsNull(
                        SecurityUtils.currentUser().getUserId(), consentType)
                .orElseThrow(() -> new BusinessException("Active consent not found"));
        consent.setGranted(false);
        consent.setRevokedAt(Instant.now());
        consentRepository.save(consent);
    }

    @Override
    @Transactional(readOnly = true)
    public CitizenProfile requireProfileForUser(UUID userId) {
        return citizenProfileRepository
                .findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Citizen profile is required before enrollment"));
    }

    @Override
    @Transactional(readOnly = true)
    public Consent requireActiveConsent(UUID userId, UUID consentId) {
        Consent consent = consentRepository
                .findByIdAndUserId(consentId, userId)
                .orElseThrow(() -> new BusinessException("Consent not found"));
        if (!consent.isGranted() || consent.getRevokedAt() != null) {
            throw new BusinessException("Active consent is required to submit an application");
        }
        if (consent.getConsentType() != ConsentType.TERMS_OF_SERVICE
                && consent.getConsentType() != ConsentType.DATA_PROCESSING) {
            throw new BusinessException("Application requires terms and data-processing consent");
        }
        return consent;
    }

    private CitizenProfile requireMyProfile() {
        return citizenProfileRepository
                .findByUserId(SecurityUtils.currentUser().getUserId())
                .orElseThrow(() -> new BusinessException("Citizen profile not found. Complete your profile first."));
    }

    private CitizenProfile createEmptyProfile(UUID userId) {
        Instant now = Instant.now();
        CitizenProfile profile = new CitizenProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(userId);
        profile.setCountry("RW");
        profile.setPreferredLanguage("en");
        profile.setKycStatus(KycStatus.PENDING);
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        return profile;
    }

    private CitizenProfileResponse toResponse(CitizenProfile profile) {
        return CitizenProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender())
                .addressLine(profile.getAddressLine())
                .district(profile.getDistrict())
                .country(profile.getCountry())
                .occupation(profile.getOccupation())
                .preferredLanguage(profile.getPreferredLanguage())
                .kycStatus(profile.getKycStatus())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private DependantResponse toDependantResponse(Dependant dependant) {
        return DependantResponse.builder()
                .id(dependant.getId())
                .firstName(dependant.getFirstName())
                .lastName(dependant.getLastName())
                .relationship(dependant.getRelationship())
                .dateOfBirth(dependant.getDateOfBirth())
                .createdAt(dependant.getCreatedAt())
                .build();
    }

    private ConsentResponse toConsentResponse(Consent consent) {
        return ConsentResponse.builder()
                .id(consent.getId())
                .consentType(consent.getConsentType())
                .version(consent.getVersion())
                .granted(consent.isGranted())
                .grantedAt(consent.getGrantedAt())
                .revokedAt(consent.getRevokedAt())
                .build();
    }
}

package com.ingoboka_api.v1.claim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ingoboka_api.v1.audit.services.AuditComplianceService;
import com.ingoboka_api.v1.claim.impls.ClaimServiceImpl;
import com.ingoboka_api.v1.claim.models.Claim;
import com.ingoboka_api.v1.claim.repositories.ClaimAppealRepository;
import com.ingoboka_api.v1.claim.repositories.ClaimDecisionRepository;
import com.ingoboka_api.v1.claim.repositories.ClaimDocumentRepository;
import com.ingoboka_api.v1.claim.repositories.ClaimRepository;
import com.ingoboka_api.v1.claim.repositories.ClaimStatusHistoryRepository;
import com.ingoboka_api.v1.common.enums.ClaimDecisionType;
import com.ingoboka_api.v1.common.enums.ClaimStatus;
import com.ingoboka_api.v1.common.enums.UserStatus;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.requests.RecordClaimDecisionRequest;
import com.ingoboka_api.v1.common.requests.UpdateClaimStatusRequest;
import com.ingoboka_api.v1.common.security.IngobokaUserDetails;
import com.ingoboka_api.v1.customer.models.CitizenProfile;
import com.ingoboka_api.v1.customer.repositories.CitizenProfileRepository;
import com.ingoboka_api.v1.identity.models.Organization;
import com.ingoboka_api.v1.identity.models.Role;
import com.ingoboka_api.v1.identity.models.RoleCodes;
import com.ingoboka_api.v1.identity.models.User;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import com.ingoboka_api.v1.messaging.services.InsurerStaffNotificationService;
import com.ingoboka_api.v1.messaging.services.NotificationTemplateService;
import com.ingoboka_api.v1.policy.repositories.PolicyRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ClaimServiceValidationTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private ClaimDocumentRepository claimDocumentRepository;

    @Mock
    private ClaimStatusHistoryRepository claimStatusHistoryRepository;

    @Mock
    private ClaimDecisionRepository claimDecisionRepository;

    @Mock
    private ClaimAppealRepository claimAppealRepository;

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private CitizenProfileRepository citizenProfileRepository;

    @Mock
    private NotificationTemplateService notificationTemplateService;

    @Mock
    private InsurerStaffNotificationService insurerStaffNotificationService;

    @Mock
    private AuditComplianceService auditComplianceService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ClaimServiceImpl claimService;

    private final UUID claimId = UUID.randomUUID();
    private final UUID citizenProfileId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUpSecurityContext() {
        authenticateAs(buildUser(userId, null, RoleCodes.CITIZEN));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitClaimWithoutDocumentsIsRejected() {
        Claim claim = draftClaim();
        CitizenProfile profile = new CitizenProfile();
        profile.setId(citizenProfileId);

        when(citizenProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(claimDocumentRepository.countByClaimId(claimId)).thenReturn(0L);

        BusinessException ex = assertThrows(BusinessException.class, () -> claimService.submitClaim(claimId));
        assertEquals("At least one supporting document is required before submitting a claim", ex.getMessage());
    }

    @Test
    void submitClaimWithDocumentsProceeds() {
        Claim claim = draftClaim();
        CitizenProfile profile = new CitizenProfile();
        profile.setId(citizenProfileId);

        when(citizenProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(claimRepository.findById(claimId)).thenReturn(Optional.of(claim));
        when(claimDocumentRepository.countByClaimId(claimId)).thenReturn(1L);
        when(claimRepository.save(any(Claim.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(policyRepository.findById(claim.getPolicyId())).thenReturn(Optional.empty());

        claimService.submitClaim(claimId);

        verify(claimStatusHistoryRepository).save(any());
        verify(claimRepository).save(any(Claim.class));
    }

    @Test
    void recordDecisionWithoutReasonIsRejected() {
        UUID orgId = UUID.randomUUID();
        authenticateAs(buildUser(UUID.randomUUID(), orgId, RoleCodes.CLAIMS_OFFICER));
        when(claimRepository.findById(claimId)).thenReturn(Optional.of(submittedClaim(orgId)));
        when(claimDecisionRepository.findByClaimId(claimId)).thenReturn(Optional.empty());

        RecordClaimDecisionRequest request = new RecordClaimDecisionRequest();
        request.setDecision(ClaimDecisionType.APPROVED);
        request.setReason("  ");

        BusinessException ex = assertThrows(BusinessException.class, () -> claimService.recordDecision(claimId, request));
        assertEquals("A reason is required for this claim decision", ex.getMessage());
    }

    @Test
    void requestInfoWithoutReasonIsRejected() {
        UUID orgId = UUID.randomUUID();
        authenticateAs(buildUser(UUID.randomUUID(), orgId, RoleCodes.CLAIMS_OFFICER));
        when(claimRepository.findById(claimId)).thenReturn(Optional.of(submittedClaim(orgId)));

        UpdateClaimStatusRequest request = new UpdateClaimStatusRequest();
        request.setStatus(ClaimStatus.INFORMATION_REQUIRED);
        request.setReason("");

        BusinessException ex = assertThrows(BusinessException.class, () -> claimService.updateStatus(claimId, request));
        assertEquals("A reason is required for this claim decision", ex.getMessage());
    }

    private void authenticateAs(User user) {
        IngobokaUserDetails userDetails = new IngobokaUserDetails(user);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    private User buildUser(UUID id, UUID organizationId, String roleCode) {
        User user = new User();
        user.setId(id);
        user.setEmail(id + "@test.ingoboka");
        user.setPasswordHash("hash");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setStatus(UserStatus.ACTIVE);
        if (organizationId != null) {
            Organization organization = new Organization();
            organization.setId(organizationId);
            user.setOrganization(organization);
        }
        Role role = new Role();
        role.setCode(roleCode);
        user.setRoles(Set.of(role));
        return user;
    }

    private Claim draftClaim() {
        Claim claim = new Claim();
        claim.setId(claimId);
        claim.setPolicyId(UUID.randomUUID());
        claim.setOrganizationId(UUID.randomUUID());
        claim.setCitizenProfileId(citizenProfileId);
        claim.setStatus(ClaimStatus.DRAFT);
        claim.setClaimNumber("CLM-TEST001");
        return claim;
    }

    private Claim submittedClaim(UUID organizationId) {
        Claim claim = draftClaim();
        claim.setOrganizationId(organizationId);
        claim.setStatus(ClaimStatus.SUBMITTED);
        return claim;
    }
}

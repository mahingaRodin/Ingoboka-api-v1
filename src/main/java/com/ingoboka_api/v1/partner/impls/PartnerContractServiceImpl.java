package com.ingoboka_api.v1.partner.impls;

import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.requests.CreatePartnerContractRequest;
import com.ingoboka_api.v1.common.requests.UpdateContractStatusRequest;
import com.ingoboka_api.v1.common.responses.PartnerContractResponse;
import com.ingoboka_api.v1.common.security.IngobokaUserDetails;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.identity.models.RoleCodes;
import com.ingoboka_api.v1.partner.models.PartnerContract;
import com.ingoboka_api.v1.partner.repositories.PartnerContractRepository;
import com.ingoboka_api.v1.partner.services.PartnerContractService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PartnerContractServiceImpl implements PartnerContractService {

    private final PartnerContractRepository partnerContractRepository;

    @Override
    @Transactional
    public PartnerContractResponse createContract(UUID partnerId, CreatePartnerContractRequest request) {
        if (!SecurityUtils.isPlatformAdmin()) {
            throw new BusinessException("Only platform administrators can create contracts");
        }

        if (partnerContractRepository.existsByContractReference(request.getContractReference())) {
            throw new BusinessException("Contract reference already exists");
        }

        Instant now = Instant.now();
        PartnerContract contract = new PartnerContract();
        contract.setId(UUID.randomUUID());
        contract.setOrganizationId(partnerId);
        contract.setContractReference(request.getContractReference().trim().toUpperCase());
        contract.setStartDate(request.getStartDate());
        contract.setEndDate(request.getEndDate());
        contract.setNotes(request.getNotes());
        contract.setCreatedAt(now);
        contract.setUpdatedAt(now);

        return toResponse(partnerContractRepository.save(contract));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartnerContractResponse> listContracts(UUID partnerId) {
        assertCanViewContracts(partnerId);
        return partnerContractRepository.findByOrganizationIdOrderByCreatedAtDesc(partnerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PartnerContractResponse updateContractStatus(
            UUID partnerId, UUID contractId, UpdateContractStatusRequest request) {
        if (!SecurityUtils.isPlatformAdmin()) {
            throw new BusinessException("Only platform administrators can update contract status");
        }

        PartnerContract contract = partnerContractRepository
                .findByIdAndOrganizationId(contractId, partnerId)
                .orElseThrow(() -> new BusinessException("Contract not found"));

        contract.setStatus(request.getStatus());
        contract.setUpdatedAt(Instant.now());
        return toResponse(partnerContractRepository.save(contract));
    }

    private PartnerContractResponse toResponse(PartnerContract contract) {
        return PartnerContractResponse.builder()
                .id(contract.getId())
                .organizationId(contract.getOrganizationId())
                .contractReference(contract.getContractReference())
                .status(contract.getStatus())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .notes(contract.getNotes())
                .createdAt(contract.getCreatedAt())
                .build();
    }

    private void assertCanViewContracts(UUID partnerId) {
        IngobokaUserDetails user = SecurityUtils.currentUser();
        if (user.hasRole(RoleCodes.PLATFORM_ADMIN)) {
            return;
        }
        if (user.hasRole(RoleCodes.PARTNER_ADMIN) && partnerId.equals(user.getOrganizationId())) {
            return;
        }
        throw new BusinessException("Access denied to partner contracts");
    }
}

package com.smart.therapy.flow.superadmin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.superadmin.entity.PlatformPartnerAccess;
import com.smart.therapy.flow.superadmin.repository.PlatformPartnerAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SuperAdminPartnerAccessService {

    private final PlatformPartnerAccessRepository partnerAccessRepository;
    private final OrganisationRepository organisationRepository;
    private final PlatformAuditService platformAuditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public List<PlatformPartnerAccess> upsertAccess(
            String partnerId,
            List<Long> organisationIds,
            Map<String, Object> featureFlags,
            Boolean active,
            Long actorAuthId
    ) {
        if (partnerId == null || partnerId.isBlank()) {
            throw new IllegalArgumentException("partnerId is required");
        }
        if (organisationIds == null || organisationIds.isEmpty()) {
            throw new IllegalArgumentException("organisationIds is required");
        }
        String flagsJson;
        try {
            flagsJson = objectMapper.writeValueAsString(featureFlags != null ? featureFlags : Map.of());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid featureFlags");
        }

        Instant now = Instant.now();
        List<PlatformPartnerAccess> saved = new ArrayList<>();
        for (Long orgId : organisationIds) {
            Organisation org = organisationRepository.findById(orgId)
                    .orElseThrow(() -> new IllegalArgumentException("Organisation not found: " + orgId));
            PlatformPartnerAccess row = partnerAccessRepository
                    .findByPartnerIdAndOrganisation_Id(partnerId, orgId)
                    .stream()
                    .findFirst()
                    .orElseGet(() -> PlatformPartnerAccess.builder()
                            .partnerId(partnerId)
                            .organisation(org)
                            .createdAt(now)
                            .build());
            row.setFeatureFlagsJson(flagsJson);
            row.setIsActive(active != null ? active : Boolean.TRUE);
            row.setUpdatedAt(now);
            saved.add(partnerAccessRepository.save(row));
        }
        platformAuditService.log(actorAuthId, "PARTNER_ACCESS_UPDATED", "Partner", partnerId, "orgCount=" + saved.size());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PlatformPartnerAccess> listByPartner(String partnerId) {
        if (partnerId == null || partnerId.isBlank()) {
            throw new IllegalArgumentException("partnerId is required");
        }
        return partnerAccessRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId);
    }
}

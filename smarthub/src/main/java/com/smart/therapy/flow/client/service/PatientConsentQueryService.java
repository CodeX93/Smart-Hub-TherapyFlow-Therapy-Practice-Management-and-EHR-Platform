package com.smart.therapy.flow.client.service;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.client.dto.ClientConsentResponse;
import com.smart.therapy.flow.client.dto.PatientConsentManagementResponse;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.PatientConsent;
import com.smart.therapy.flow.client.enums.ConsentManagementStatusFilter;
import com.smart.therapy.flow.client.enums.ConsentManagementTypeFilter;
import com.smart.therapy.flow.client.enums.ConsentType;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.repository.PatientConsentRepository;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.report.service.ClientReportAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PatientConsentQueryService {

    private final PatientConsentRepository consentRepository;
    private final ClientRepository clientRepository;
    private final ClientPortalSettingsService portalSettingsService;
    private final CurrentUserService currentUserService;
    private final AuditLogService auditLogService;
    private final ClientReportAccessService clientReportAccessService;

    public List<PatientConsentManagementResponse> getConsentManagementList(
            String consentType,
            String status,
            String search,
            AuthPrincipal userPrincipal,
            String clientIpAddress,
            String userAgent
    ) {
        return getConsentManagementList(consentType, status, search, userPrincipal, clientIpAddress, userAgent, "consent_management_viewed");
    }

    public List<PatientConsentManagementResponse> getConsentManagementList(
            String consentType,
            String status,
            String search,
            AuthPrincipal userPrincipal,
            String clientIpAddress,
            String userAgent,
            String auditAction
    ) {
        List<Client> clients = clientRepository.findAll();
        List<ManagementRow> rows = new ArrayList<>();

        for (Client client : clients) {
            List<PatientConsent> allConsents = consentRepository.findByClientIdOrderByCreatedAtDesc(client.getId());

            Boolean hasPortalAccess = portalSettingsService.findByClientId(client.getId())
                    .map(s -> Boolean.TRUE.equals(s.getHasPortalAccess()) && Boolean.TRUE.equals(s.getIsActivated()))
                    .orElse(false);

            PatientConsentManagementResponse.ConsentStatus aiProcessingStatus = getConsentStatus(allConsents, ConsentType.AI_PROCESSING);
            PatientConsentManagementResponse.ConsentStatus dataSharingStatus = getConsentStatus(allConsents, ConsentType.DATA_SHARING);
            PatientConsentManagementResponse.ConsentStatus researchStatus = getConsentStatus(allConsents, ConsentType.RESEARCH);
            PatientConsentManagementResponse.ConsentStatus marketingStatus = getConsentStatus(allConsents, ConsentType.MARKETING);

            boolean matchesConsentType = matchesConsentTypeFilter(consentType, aiProcessingStatus, dataSharingStatus, researchStatus, marketingStatus);
            boolean matchesStatus = matchesStatusFilter(status, aiProcessingStatus, dataSharingStatus, researchStatus, marketingStatus);
            boolean matchesSearch = matchesSearchFilter(search, client);

            if (matchesConsentType && matchesStatus && matchesSearch) {
                List<PatientConsentManagementResponse.ConsentDetail> consentDetails = allConsents.stream()
                        .map(c -> PatientConsentManagementResponse.ConsentDetail.builder()
                                .id(c.getId())
                                .consentType(c.getConsentType() != null ? c.getConsentType().getDisplayName() : null)
                                .status(getConsentStatusForDetail(c))
                                .version(c.getConsentFormVersion())
                                .grantedAt(c.getGrantedAt())
                                .withdrawnAt(c.getWithdrawnAt())
                                .build())
                        .toList();

                PatientConsentManagementResponse response = PatientConsentManagementResponse.builder()
                        .clientId(client.getId())
                        .fullName(client.getFullName())
                        .email(client.getPrimaryEmail())
                        .portalAccess(hasPortalAccess)
                        .aiProcessing(aiProcessingStatus)
                        .dataSharing(dataSharingStatus)
                        .research(researchStatus)
                        .marketing(marketingStatus)
                        .allConsents(consentDetails)
                        .build();

                rows.add(new ManagementRow(response, resolveLastActivity(client, allConsents)));
            }
        }

        // Latest activity on top: most recently updated/created consent (falling back to the client's own timestamps).
        rows.sort(Comparator.comparing(
                ManagementRow::lastActivity,
                Comparator.nullsLast(Comparator.reverseOrder())));

        List<PatientConsentManagementResponse> result = new ArrayList<>(rows.size());
        for (ManagementRow row : rows) {
            result.add(row.response());
        }

        if (userPrincipal != null) {
            Long userId = currentUserService.getCurrentUserId(userPrincipal);
            auditLogService.logClientAccess(
                    userId,
                    userPrincipal.getLoginIdentifier(),
                    null,
                    auditAction != null ? auditAction : "consent_management_viewed",
                    clientIpAddress,
                    userAgent,
                    Map.of("filter_consentType", consentType != null ? consentType : "ALL",
                            "filter_status", status != null ? status : "ALL",
                            "filter_search", StringUtils.hasText(search) ? search.trim() : "ALL"));
        }

        return result;
    }

    /**
     * Holds a management row alongside the timestamp used to order the list (latest first).
     */
    private record ManagementRow(PatientConsentManagementResponse response, Instant lastActivity) {
    }

    /**
     * Resolve the most recent activity timestamp for a client: the newest consent change
     * (updatedAt, falling back to createdAt), otherwise the client's own timestamps.
     */
    private Instant resolveLastActivity(Client client, List<PatientConsent> consents) {
        Instant latest = null;
        if (consents != null) {
            for (PatientConsent consent : consents) {
                Instant candidate = consent.getUpdatedAt() != null ? consent.getUpdatedAt() : consent.getCreatedAt();
                if (candidate != null && (latest == null || candidate.isAfter(latest))) {
                    latest = candidate;
                }
            }
        }
        if (latest == null) {
            latest = client.getUpdatedAt() != null ? client.getUpdatedAt() : client.getCreatedAt();
        }
        return latest;
    }

    public List<ClientConsentResponse> getAllConsents(String consentType,
                                                      Boolean granted,
                                                      AuthPrincipal userPrincipal,
                                                      String clientIpAddress,
                                                      String userAgent) {
        List<Client> clients = clientRepository.findAll();
        List<ClientConsentResponse> result = new ArrayList<>();

        for (Client client : clients) {
            List<PatientConsent> consents = consentRepository.findByClientIdOrderByCreatedAtDesc(client.getId());

            List<PatientConsent> filteredConsents = consents.stream()
                    .filter(c -> {
                        if (StringUtils.hasText(consentType)) {
                            try {
                                ConsentType type = ConsentType.fromValue(consentType);
                                if (!type.equals(c.getConsentType())) {
                                    return false;
                                }
                            } catch (IllegalArgumentException e) {
                                return false;
                            }
                        }
                        if (granted != null && !granted.equals(c.getGranted())) {
                            return false;
                        }
                        return true;
                    })
                    .toList();

            if (!filteredConsents.isEmpty() || (consentType == null && granted == null)) {
                ClientConsentResponse clientResponse = ClientConsentResponse.builder()
                        .id(client.getId())
                        .clientId(client.getClientId())
                        .fullName(client.getFullName())
                        .email(client.getPrimaryEmail())
                        .hasPortalAccess(null)
                        .consents(filteredConsents.stream()
                                .map(this::toConsentResponse)
                                .toList())
                        .build();
                result.add(clientResponse);
            }
        }

        if (userPrincipal != null) {
            Long userId = currentUserService.getCurrentUserId(userPrincipal);
            auditLogService.logClientAccess(
                    userId,
                    userPrincipal.getLoginIdentifier(),
                    null,
                    "consents_viewed",
                    clientIpAddress,
                    userAgent,
                    Map.of("filter_consentType", consentType != null ? consentType : "all",
                            "filter_granted", granted != null ? granted.toString() : "all"));
        }

        return result;
    }

    public List<com.smart.therapy.flow.client.portal.dto.PortalConsentResponse> getClientConsents(Long clientId,
                                                                                                    AuthPrincipal userPrincipal,
                                                                                                    String clientIpAddress,
                                                                                                    String userAgent) {
        if (userPrincipal != null) {
            clientReportAccessService.requireClientAccess(clientId, userPrincipal);
        } else if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Client not found");
        }

        List<PatientConsent> consents = consentRepository.findByClientIdOrderByCreatedAtDesc(clientId);

        if (userPrincipal != null) {
            Long userId = currentUserService.getCurrentUserId(userPrincipal);
            auditLogService.logClientAccess(
                    userId,
                    userPrincipal.getLoginIdentifier(),
                    clientId,
                    "client_consents_viewed",
                    clientIpAddress,
                    userAgent,
                    Map.of("consent_count", consents.size()));
        }

        return consents.stream().map(this::toPortalConsentResponse).toList();
    }

    private com.smart.therapy.flow.client.portal.dto.PortalConsentResponse toPortalConsentResponse(
            PatientConsent consent) {
        return com.smart.therapy.flow.client.portal.dto.PortalConsentResponse.builder()
                .id(consent.getId())
                .clientId(consent.getClient().getId())
                .consentType(consent.getConsentType() != null ? consent.getConsentType().getDisplayName() : null)
                .consentVersion(consent.getConsentFormVersion())
                .granted(consent.getGranted())
                .grantedAt(consent.getGrantedAt())
                .withdrawnAt(consent.getWithdrawnAt())
                .ipAddress(consent.getIpAddress())
                .userAgent(consent.getUserAgent())
                .notes(consent.getNotes())
                .createdAt(consent.getCreatedAt())
                .updatedAt(consent.getUpdatedAt())
                .build();
    }

    private ClientConsentResponse.ConsentInfo toConsentResponse(PatientConsent consent) {
        return ClientConsentResponse.ConsentInfo.builder()
                .id(consent.getId())
                .consentType(consent.getConsentType() != null ? consent.getConsentType().getDisplayName() : null)
                .granted(consent.getGranted())
                .grantedAt(consent.getGrantedAt())
                .withdrawnAt(consent.getWithdrawnAt())
                .consentVersion(consent.getConsentFormVersion())
                .createdAt(consent.getCreatedAt())
                .updatedAt(consent.getUpdatedAt())
                .build();
    }

    private PatientConsentManagementResponse.ConsentStatus getConsentStatus(
            List<PatientConsent> consents, ConsentType consentType) {
        Optional<PatientConsent> latestConsent = consents.stream()
                .filter(c -> c.getConsentType() == consentType)
                .max(Comparator.comparing((PatientConsent c) ->
                        c.getGrantedAt() != null ? c.getGrantedAt() : java.time.Instant.EPOCH));

        if (latestConsent.isEmpty()) {
            return PatientConsentManagementResponse.ConsentStatus.NOT_SET;
        }

        PatientConsent consent = latestConsent.get();
        if (Boolean.TRUE.equals(consent.getGranted()) && consent.getWithdrawnAt() == null) {
            return PatientConsentManagementResponse.ConsentStatus.GRANTED;
        } else {
            return PatientConsentManagementResponse.ConsentStatus.DENIED;
        }
    }

    private PatientConsentManagementResponse.ConsentStatus getConsentStatusForDetail(PatientConsent consent) {
        if (Boolean.TRUE.equals(consent.getGranted()) && consent.getWithdrawnAt() == null) {
            return PatientConsentManagementResponse.ConsentStatus.GRANTED;
        } else {
            return PatientConsentManagementResponse.ConsentStatus.DENIED;
        }
    }

    private boolean matchesConsentTypeFilter(
            String filter,
            PatientConsentManagementResponse.ConsentStatus aiProcessing,
            PatientConsentManagementResponse.ConsentStatus dataSharing,
            PatientConsentManagementResponse.ConsentStatus research,
            PatientConsentManagementResponse.ConsentStatus marketing) {
        ConsentManagementTypeFilter parsedFilter = ConsentManagementTypeFilter.tryParse(filter)
                .orElse(ConsentManagementTypeFilter.ALL);

        if (parsedFilter == ConsentManagementTypeFilter.ALL) {
            return true;
        }

        return switch (parsedFilter) {
            case AI_PROCESSING -> aiProcessing != PatientConsentManagementResponse.ConsentStatus.NOT_SET;
            case DATA_SHARING -> dataSharing != PatientConsentManagementResponse.ConsentStatus.NOT_SET;
            case RESEARCH -> research != PatientConsentManagementResponse.ConsentStatus.NOT_SET;
            case MARKETING -> marketing != PatientConsentManagementResponse.ConsentStatus.NOT_SET;
            case ALL -> true;
        };
    }

    private boolean matchesStatusFilter(
            String filter,
            PatientConsentManagementResponse.ConsentStatus aiProcessing,
            PatientConsentManagementResponse.ConsentStatus dataSharing,
            PatientConsentManagementResponse.ConsentStatus research,
            PatientConsentManagementResponse.ConsentStatus marketing) {
        ConsentManagementStatusFilter parsedFilter = ConsentManagementStatusFilter.tryParse(filter)
                .orElse(ConsentManagementStatusFilter.ALL);

        if (parsedFilter == ConsentManagementStatusFilter.ALL) {
            return true;
        }

        return switch (parsedFilter) {
            case GRANTED -> aiProcessing == PatientConsentManagementResponse.ConsentStatus.GRANTED ||
                    dataSharing == PatientConsentManagementResponse.ConsentStatus.GRANTED ||
                    research == PatientConsentManagementResponse.ConsentStatus.GRANTED ||
                    marketing == PatientConsentManagementResponse.ConsentStatus.GRANTED;
            case DENIED -> aiProcessing == PatientConsentManagementResponse.ConsentStatus.DENIED ||
                    dataSharing == PatientConsentManagementResponse.ConsentStatus.DENIED ||
                    research == PatientConsentManagementResponse.ConsentStatus.DENIED ||
                    marketing == PatientConsentManagementResponse.ConsentStatus.DENIED;
            case ALL -> true;
        };
    }

    private boolean matchesSearchFilter(String search, Client client) {
        if (!StringUtils.hasText(search)) {
            return true;
        }

        String trimmed = search.trim();
        String needle = trimmed.toLowerCase(Locale.ROOT);

        if (client.getId() != null) {
            try {
                Long numericSearch = Long.parseLong(trimmed);
                if (client.getId().equals(numericSearch)) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // Not a numeric search term; continue with text matching below.
            }
        }

        return containsIgnoreCase(client.getClientId(), needle)
                || containsIgnoreCase(client.getFullName(), needle)
                || containsIgnoreCase(client.getPrimaryEmail(), needle);
    }

    private boolean containsIgnoreCase(String value, String needleLower) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needleLower);
    }

}

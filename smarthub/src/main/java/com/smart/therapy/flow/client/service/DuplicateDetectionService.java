package com.smart.therapy.flow.client.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.dto.*;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientContact;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.util.RoleName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DuplicateDetectionService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ClientContactService contactService;
    private final CurrentUserService currentUserService;
    private final PermissionChecker permissionChecker;

    @Transactional(readOnly = true)
    public DuplicatesResponse detectDuplicates(AuthPrincipal requester) {
        Objects.requireNonNull(requester, "Requester is required");
        
        // PBAC: Check permission to access duplicate detection
        // Requires ability to view clients (admin/supervisor level)
        if (!permissionChecker.hasPermission(requester, "CLIENT_VIEW_ALL") && !permissionChecker.hasPermission(requester, "CLIENT_VIEW_TEAM")) {
            throw new ForbiddenException("Insufficient permissions to access duplicate detection");
        }

        // Get all clients that are not marked as duplicate
        List<Client> basicClients = clientRepository.findAll().stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsDuplicate()))
                .collect(Collectors.toList());

        // Enrich clients with session/document/billing counts
        Map<Long, ClientEnrichment> enrichmentMap = enrichClients(basicClients);

        // Find potential duplicates
        List<DuplicateGroupResponse> duplicateGroups = new ArrayList<>();
        Set<String> processedPairs = new HashSet<>();

        for (int i = 0; i < basicClients.size(); i++) {
            for (int j = i + 1; j < basicClients.size(); j++) {
                Client client1 = basicClients.get(i);
                Client client2 = basicClients.get(j);

                String pairKey = client1.getId() + "-" + client2.getId();
                if (processedPairs.contains(pairKey)) continue;
                processedPairs.add(pairKey);

                DuplicateMatch match = checkForDuplicate(client1, client2, enrichmentMap);
                if (match != null) {
                    duplicateGroups.add(match.toResponse());
                    processedPairs.add(client2.getId() + "-" + client1.getId()); // Mark reverse pair
                }
            }
        }

        int totalDuplicates = duplicateGroups.stream()
                .mapToInt(g -> g.getClients().size())
                .sum();

        return DuplicatesResponse.builder()
                .duplicateGroups(duplicateGroups)
                .totalDuplicates(totalDuplicates)
                .build();
    }

    @Transactional
    public void markDuplicate(Long clientId, MarkDuplicateRequest request, AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        
        // Only admin and supervisor can mark duplicates
        if (!hasRole(requester, "ADMIN") && !hasRole(requester, "SUPERVISOR")) {
            throw new ForbiddenException("Only administrators and supervisors can mark duplicates");
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        Client primaryClient = clientRepository.findById(request.getDuplicateOfClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Primary client not found"));

        if (clientId.equals(request.getDuplicateOfClientId())) {
            throw new BadRequestException("Client cannot be a duplicate of itself");
        }

        if (Boolean.TRUE.equals(client.getIsDuplicate())) {
            throw new BadRequestException("Client is already marked as duplicate");
        }

        client.setIsDuplicate(true);
        client.setDuplicateOfClient(primaryClient);
        client.setDuplicateMarkedAt(Instant.now());
        // Set duplicateMarkedBy
        User requesterUser = userRepository.findById(currentUserService.requireCurrentUser(requester).getId())
                .orElseThrow(() -> new ResourceNotFoundException("Requester user not found"));
        client.setDuplicateMarkedBy(requesterUser);
        
        clientRepository.save(client);
        clientRepository.flush();
    }

    @Transactional
    public void unmarkDuplicate(Long clientId, AuthPrincipal requester) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(requester, "Requester is required");
        
        // Only admin and supervisor can unmark duplicates
        if (!hasRole(requester, "ADMIN") && !hasRole(requester, "SUPERVISOR")) {
            throw new ForbiddenException("Only administrators and supervisors can unmark duplicates");
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        if (!Boolean.TRUE.equals(client.getIsDuplicate())) {
            throw new BadRequestException("Client is not marked as duplicate");
        }

        client.setIsDuplicate(false);
        client.setDuplicateOfClient(null);
        client.setDuplicateMarkedAt(null);
        client.setDuplicateMarkedBy(null);
        clientRepository.save(client);
        clientRepository.flush();
    }

    // ========== PRIVATE HELPER METHODS ==========

    private Map<Long, ClientEnrichment> enrichClients(List<Client> clients) {
        Map<Long, ClientEnrichment> enrichmentMap = new HashMap<>();
        
        // TODO: Enrich with session count, document count, billing count, last session date
        // This would require queries to Session, Document, SessionBilling repositories
        // For now, return empty enrichment
        for (Client client : clients) {
            enrichmentMap.put(client.getId(), ClientEnrichment.builder()
                    .sessionCount(0)
                    .documentCount(0)
                    .billingCount(0)
                    .lastSessionDate(null)
                    .build());
        }
        
        return enrichmentMap;
    }

    private DuplicateMatch checkForDuplicate(Client client1, Client client2, Map<Long, ClientEnrichment> enrichment) {
        String name1 = normalizeString(client1.getFullName());
        String name2 = normalizeString(client2.getFullName());
        
        // Use normalized entities for contact information
        String phone1 = getPrimaryPhone(client1.getId());
        String phone2 = getPrimaryPhone(client2.getId());
        String email1 = getPrimaryEmail(client1.getId());
        String email2 = getPrimaryEmail(client2.getId());

        boolean isDuplicate = false;
        String confidenceLevel = "medium";
        int confidenceScore = 0;
        String matchType = "";

        // High confidence: Matching name AND (phone OR email)
        if (name1.equals(name2) && (!phone1.isEmpty() || !email1.isEmpty())) {
            if ((!phone1.isEmpty() && phone1.equals(phone2)) || 
                (!email1.isEmpty() && email1.equals(email2))) {
                isDuplicate = true;
                confidenceLevel = "high";
                confidenceScore = 90;
                matchType = "name_and_contact";
            }
        }

        // Medium confidence: Matching phone OR email with similar name
        if (!isDuplicate) {
            if (!phone1.isEmpty() && phone1.equals(phone2)) {
                if (nameSimilarity(name1, name2) > 0.7) {
                    isDuplicate = true;
                    confidenceLevel = "medium";
                    confidenceScore = 70;
                    matchType = "phone_and_similar_name";
                }
            } else if (!email1.isEmpty() && email1.equals(email2)) {
                if (nameSimilarity(name1, name2) > 0.7) {
                    isDuplicate = true;
                    confidenceLevel = "medium";
                    confidenceScore = 70;
                    matchType = "email_and_similar_name";
                }
            }
        }

        if (!isDuplicate) {
            return null;
        }

        // Determine which client to keep
        ClientEnrichment enrich1 = enrichment.get(client1.getId());
        ClientEnrichment enrich2 = enrichment.get(client2.getId());
        
        DuplicateRecommendation recommendation = determineWhichToKeep(
                client1, client2, enrich1, enrich2);

        return DuplicateMatch.builder()
                .client1(client1)
                .client2(client2)
                .confidenceLevel(confidenceLevel)
                .confidenceScore(confidenceScore)
                .matchType(matchType)
                .recommendation(recommendation)
                .build();
    }

    private DuplicateRecommendation determineWhichToKeep(Client client1, Client client2,
                                                         ClientEnrichment enrich1, ClientEnrichment enrich2) {
        int score1 = 0;
        int score2 = 0;
        List<String> reasons = new ArrayList<>();

        // Score based on session count
        if (enrich1.getSessionCount() > enrich2.getSessionCount()) {
            score1 += 10;
            reasons.add("Client 1 has more sessions");
        } else if (enrich2.getSessionCount() > enrich1.getSessionCount()) {
            score2 += 10;
            reasons.add("Client 2 has more sessions");
        }

        // Score based on document count
        if (enrich1.getDocumentCount() > enrich2.getDocumentCount()) {
            score1 += 5;
        } else if (enrich2.getDocumentCount() > enrich1.getDocumentCount()) {
            score2 += 5;
        }

        // Score based on creation date (older = more established)
        if (client1.getCreatedAt().isBefore(client2.getCreatedAt())) {
            score1 += 5;
            reasons.add("Client 1 was created earlier");
        } else {
            score2 += 5;
            reasons.add("Client 2 was created earlier");
        }

        // Score based on last session date
        if (enrich1.getLastSessionDate() != null && enrich2.getLastSessionDate() != null) {
            if (enrich1.getLastSessionDate().isAfter(enrich2.getLastSessionDate())) {
                score1 += 5;
            } else {
                score2 += 5;
            }
        } else if (enrich1.getLastSessionDate() != null) {
            score1 += 5;
        } else if (enrich2.getLastSessionDate() != null) {
            score2 += 5;
        }

        Long keepClientId = score1 >= score2 ? client1.getId() : client2.getId();
        Long deleteClientId = score1 >= score2 ? client2.getId() : client1.getId();

        return DuplicateRecommendation.builder()
                .keepClientId(keepClientId)
                .deleteClientId(deleteClientId)
                .reasons(reasons)
                .build();
    }

    private String normalizeString(String str) {
        return str != null ? str.toLowerCase().trim() : "";
    }

    private String normalizePhone(String phone) {
        return phone != null ? phone.replaceAll("\\D", "") : "";
    }

    private String normalizeEmail(String email) {
        return email != null ? email.toLowerCase().trim() : "";
    }

    private double nameSimilarity(String name1, String name2) {
        if (name1.isEmpty() || name2.isEmpty()) return 0.0;
        
        // Simple Levenshtein-based similarity
        int maxLen = Math.max(name1.length(), name2.length());
        if (maxLen == 0) return 1.0;
        
        int distance = levenshteinDistance(name1, name2);
        return 1.0 - ((double) distance / maxLen);
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                            dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1)
                    );
                }
            }
        }
        
        return dp[s1.length()][s2.length()];
    }

    // DEPRECATED: Use permission checks instead
    // This method is kept for backward compatibility in business logic validation
    // For access control, always use principal.hasPermission() instead
    @Deprecated
    private boolean hasRole(AuthPrincipal principal, String roleName) {
        return principal.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + roleName));
    }

    /**
     * Get primary email from normalized ClientContact entity.
     * Falls back to deprecated client.email field for backward compatibility.
     */
    private String getPrimaryEmail(Long clientId) {
        try {
            Optional<ClientContact> primaryEmail = contactService.getPrimaryEmail(clientId);
            if (primaryEmail.isPresent() && primaryEmail.get().getContactValue() != null) {
                return normalizeEmail(primaryEmail.get().getContactValue());
            }
        } catch (Exception e) {
            // Fall through to deprecated field
        }
        // Fallback to client primary email from normalized contacts
        Client client = clientRepository.findById(clientId).orElse(null);
        return client != null ? normalizeEmail(client.getPrimaryEmail()) : "";
    }

    /**
     * Get primary phone from normalized ClientContact entity.
     * Falls back to deprecated client.phone field for backward compatibility.
     */
    private String getPrimaryPhone(Long clientId) {
        try {
            Optional<ClientContact> primaryPhone = contactService.getPrimaryPhone(clientId);
            if (primaryPhone.isPresent() && primaryPhone.get().getContactValue() != null) {
                return normalizePhone(primaryPhone.get().getContactValue());
            }
        } catch (Exception e) {
            // Fall through to deprecated field
        }
        // Fallback to client primary phone from normalized contacts
        Client client = clientRepository.findById(clientId).orElse(null);
        return client != null ? normalizePhone(client.getPrimaryPhone()) : "";
    }

    // Inner classes
    @lombok.Data
    @lombok.Builder
    private static class ClientEnrichment {
        private int sessionCount;
        private int documentCount;
        private int billingCount;
        private Instant lastSessionDate;
    }

    @lombok.Data
    @lombok.Builder
    private static class DuplicateMatch {
        private Client client1;
        private Client client2;
        private String confidenceLevel;
        private int confidenceScore;
        private String matchType;
        private DuplicateRecommendation recommendation;

        DuplicateGroupResponse toResponse() {
            ClientResponse resp1 = toClientResponse(client1);
            ClientResponse resp2 = toClientResponse(client2);
            return DuplicateGroupResponse.builder()
                    .clients(List.of(resp1, resp2))
                    .confidenceLevel(confidenceLevel)
                    .confidenceScore(confidenceScore)
                    .matchType(matchType)
                    .recommendation(recommendation)
                    .build();
        }
        
        private ClientResponse toClientResponse(Client client) {
            return ClientResponse.builder()
                    .id(client.getId())
                    .clientId(client.getClientId())
                    .fullName(client.getFullName())
                    .email(client.getPrimaryEmail())
                    .phone(client.getPrimaryPhone())
                    .dateOfBirth(client.getDateOfBirth())
                    .status(client.getStatus())
                    .stage(client.getStage())
                    .assignedTherapistId(client.getAssignedTherapist() != null ? client.getAssignedTherapist().getId() : null)
                    .createdAt(client.getCreatedAt())
                    .build();
        }
    }
}




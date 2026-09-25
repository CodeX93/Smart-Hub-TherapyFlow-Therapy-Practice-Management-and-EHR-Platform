package com.smart.therapy.flow.client.service;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.service.BlindIndexService;
import com.smart.therapy.flow.common.service.EncryptionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * MRN allocate / lookup for plaintext (legacy) and encrypted MRN + blind indexes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientMrnService {

    private final ClientRepository clientRepository;
    private final BlindIndexService blindIndexService;
    private final EncryptionService encryptionService;
    private final ClientSearchHelper clientSearchHelper;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Reserve a number independently so the year-counter lock is released before client onboarding.
     * Like generated database IDs, reserved MRNs can have gaps if the caller rolls back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String allocateNextMrn() {
        int year = LocalDate.now().getYear();
        try {
            entityManager.createNativeQuery(
                            "INSERT INTO client_mrn_counters(year, next_value) VALUES (:year, 0) "
                                    + "ON CONFLICT (year) DO NOTHING")
                    .setParameter("year", year)
                    .executeUpdate();
            entityManager.createNativeQuery(
                            "UPDATE client_mrn_counters SET next_value = next_value + 1 WHERE year = :year")
                    .setParameter("year", year)
                    .executeUpdate();
            Number nextNum = (Number) entityManager.createNativeQuery(
                            "SELECT next_value FROM client_mrn_counters WHERE year = :year")
                    .setParameter("year", year)
                    .getSingleResult();
            int next = nextNum.intValue();
            String candidate = String.format("CL-%d-%04d", year, next);
            int guard = 0;
            while (existsMrn(candidate, true) && guard++ < 10_000) {
                entityManager.createNativeQuery(
                                "UPDATE client_mrn_counters SET next_value = next_value + 1 WHERE year = :year")
                        .setParameter("year", year)
                        .executeUpdate();
                nextNum = (Number) entityManager.createNativeQuery(
                                "SELECT next_value FROM client_mrn_counters WHERE year = :year")
                        .setParameter("year", year)
                        .getSingleResult();
                next = nextNum.intValue();
                candidate = String.format("CL-%d-%04d", year, next);
            }
            return candidate;
        } catch (Exception ex) {
            log.warn("MRN counter table unavailable ({}); falling back to legacy scan", ex.getMessage());
            return allocateNextMrnLegacy(year);
        }
    }

    private String allocateNextMrnLegacy(int year) {
        String prefix = "CL-" + year + "-";
        Optional<Client> lastClient = clientRepository
                .findTopByClientIdStartingWithOrderByClientIdDescIncludingDeleted(prefix);
        int nextNumber = 1;
        if (lastClient.isPresent() && lastClient.get().getClientId() != null) {
            String lastId = displayMrn(lastClient.get());
            try {
                if (lastId != null && lastId.startsWith(prefix)) {
                    nextNumber = Integer.parseInt(lastId.substring(prefix.length())) + 1;
                }
            } catch (NumberFormatException e) {
                log.warn("Failed to parse client ID from {}", lastId);
            }
        }
        for (int i = 0; i < 10_000; i++) {
            String candidate = String.format("CL-%d-%04d", year, nextNumber + i);
            if (!existsMrn(candidate, true)) {
                return candidate;
            }
        }
        return String.format("CL-%d-%04d", year, System.currentTimeMillis() % 10000);
    }

    public Optional<Client> findActiveByMrn(String mrn) {
        if (!StringUtils.hasText(mrn)) {
            return Optional.empty();
        }
        List<String> candidates = clientSearchHelper.mrnSearchCandidates(mrn);
        if (blindIndexService.getSearchMode().writesBlindIndexes()) {
            for (String candidate : candidates) {
                byte[] digest = blindIndexService.compute(BlindIndexService.Kind.CLIENT_ID, candidate);
                Optional<Client> byBlind = clientRepository.findByClientIdBlindIdxAndIsDeletedFalse(digest);
                if (byBlind.isPresent()) {
                    return byBlind;
                }
            }
        }
        // Legacy plaintext rows only. TFENC:v2 (AES-GCM) is non-deterministic — equality on client_id
        // never matches and must not be relied on for PHI Approach C.
        for (String candidate : candidates) {
            Optional<Client> byPlain = clientRepository.findByClientId(candidate);
            if (byPlain.isPresent()) {
                return byPlain;
            }
        }
        Optional<Client> byRaw = clientRepository.findByClientId(mrn.trim());
        if (byRaw.isPresent()) {
            return byRaw;
        }
        // Blind index missing/stale (common after cutover before backfill, or HMAC/tenant mismatch local):
        // list decrypts MRN for UI; bulk upload must match the same visible value.
        return findActiveByDecryptedMrn(candidates);
    }

    public boolean existsMrn(String mrn, boolean includeDeleted) {
        if (!StringUtils.hasText(mrn)) {
            return false;
        }
        List<String> candidates = clientSearchHelper.mrnSearchCandidates(mrn);
        if (blindIndexService.getSearchMode().writesBlindIndexes()) {
            for (String candidate : candidates) {
                byte[] digest = blindIndexService.compute(BlindIndexService.Kind.CLIENT_ID, candidate);
                boolean byBlind = includeDeleted
                        ? clientRepository.existsByClientIdBlindIdx(digest)
                        : clientRepository.existsByClientIdBlindIdxAndIsDeletedFalse(digest);
                if (byBlind) {
                    return true;
                }
            }
        }
        for (String candidate : candidates) {
            boolean byPlain = includeDeleted
                    ? clientRepository.existsByClientIdIncludingDeleted(candidate)
                    : clientRepository.existsByClientId(candidate);
            if (byPlain) {
                return true;
            }
        }
        boolean byRaw = includeDeleted
                ? clientRepository.existsByClientIdIncludingDeleted(mrn.trim())
                : clientRepository.existsByClientId(mrn.trim());
        if (byRaw) {
            return true;
        }
        // Active-only decrypt scan (same visibility as the UI client list).
        return findActiveByDecryptedMrn(candidates).isPresent();
    }

    /**
     * Match by decrypted MRN among non-deleted clients in the current tenant schema.
     * Used when client_id is encrypted at rest and client_id_blind_idx is null or out of sync.
     */
    private Optional<Client> findActiveByDecryptedMrn(List<String> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }
        java.util.Set<String> needle = new java.util.LinkedHashSet<>(candidates);
        for (Object[] row : clientRepository.findActiveMrnCandidates()) {
            String value = (String) row[1];
            if (!StringUtils.hasText(value)) {
                continue;
            }
            String displayed = encryptionService.isEncrypted(value) ? encryptionService.decrypt(value) : value;
            if (!StringUtils.hasText(displayed)) {
                continue;
            }
            String upper = blindIndexService.normalizeMrn(displayed);
            String canonical = clientSearchHelper.normalizeMrn(displayed);
            if (needle.contains(upper) || (canonical != null && needle.contains(canonical))) {
                return clientRepository.findById(((Number) row[0]).longValue());
            }
        }
        return Optional.empty();
    }

    public String displayMrn(Client client) {
        if (client == null || !StringUtils.hasText(client.getClientId())) {
            return null;
        }
        String value = client.getClientId();
        return encryptionService.isEncrypted(value) ? encryptionService.decrypt(value) : value;
    }
}

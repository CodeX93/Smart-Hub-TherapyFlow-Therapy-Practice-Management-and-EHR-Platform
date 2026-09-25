package com.smart.therapy.flow.client.service;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientContact;
import com.smart.therapy.flow.client.entity.ClientNameBlindIndex;
import com.smart.therapy.flow.client.enums.ContactType;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.converter.EncryptedSearchableStringConverter;
import com.smart.therapy.flow.common.service.BlindIndexService;
import com.smart.therapy.flow.common.service.EncryptionService;
import com.smart.therapy.flow.common.util.PhoneNormalizationUtil;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Client search for PHI (Approach C).
 * Name / email / phone: exact match via blind indexes (never LIKE on encrypted columns).
 * <p>
 * MRN / client number: blind-index equality for exact forms, plus a tenant decrypt scan for
 * exact and partial matches (e.g. {@code CL}, {@code CL-2026}) when indexes are missing/stale
 * or the query is a fragment. {@code clients.client_id} is non-deterministically encrypted,
 * so ciphertext equality cannot be used.
 */
@Component
@RequiredArgsConstructor
public class ClientSearchHelper {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    /** Full MRN: CL-2026-0001 */
    private static final Pattern MRN_FULL_PATTERN = Pattern.compile("^CL-\\d{4}-\\d+$", Pattern.CASE_INSENSITIVE);
    /** Year + sequence without prefix: 2026-0001 */
    private static final Pattern MRN_YEAR_SEQ_PATTERN = Pattern.compile("^\\d{4}-\\d+$");
    private static final Pattern MRN_CANONICAL_PATTERN = Pattern.compile("^CL-(\\d{4})-(\\d+)$", Pattern.CASE_INSENSITIVE);

    private final EncryptionService encryptionService;
    private final BlindIndexService blindIndexService;
    private final ClientRepository clientRepository;

    public enum SearchKind {
        DATE_OF_BIRTH,
        MRN,
        EMAIL,
        PHONE,
        FULL_NAME
    }

    public SearchKind classify(String raw) {
        if (!StringUtils.hasText(raw)) {
            return SearchKind.FULL_NAME;
        }
        String trimmed = raw.trim();
        if (parseDateOfBirth(trimmed) != null) return SearchKind.DATE_OF_BIRTH;
        if (looksLikeMrn(trimmed)) {
            return SearchKind.MRN;
        }
        if (EMAIL_PATTERN.matcher(trimmed).matches()) {
            return SearchKind.EMAIL;
        }
        String e164 = PhoneNormalizationUtil.normalizePhoneE164(trimmed);
        if (e164 != null || looksLikePhone(trimmed)) {
            return SearchKind.PHONE;
        }
        return SearchKind.FULL_NAME;
    }

    private static LocalDate parseDateOfBirth(String value) {
        try {
            if (value.matches("\\d{4}-\\d{2}-\\d{2}")) return LocalDate.parse(value);
            if (value.matches("\\d{2}/\\d{2}/\\d{4}")) {
                return LocalDate.parse(value, DateTimeFormatter.ofPattern("MM/dd/uuuu")
                        .withResolverStyle(ResolverStyle.STRICT));
            }
        } catch (DateTimeParseException ignored) {
            // Invalid calendar dates must never be normalized into another date.
        }
        return null;
    }

    /**
     * Canonical MRN for indexing/search: upper-case, optional {@code CL-} prefix,
     * and zero-padded sequence (e.g. {@code cl-2025-581} → {@code CL-2025-0581}).
     */
    public String normalizeMrn(String raw) {
        if (raw == null) {
            return null;
        }
        String compact = canonicalizeMrnInput(raw);
        if (MRN_YEAR_SEQ_PATTERN.matcher(compact).matches()) {
            compact = "CL-" + compact;
        }
        var matcher = MRN_CANONICAL_PATTERN.matcher(compact);
        if (matcher.matches()) {
            int seq = Integer.parseInt(matcher.group(2));
            return String.format(Locale.ROOT, "CL-%s-%04d", matcher.group(1), seq);
        }
        return blindIndexService.normalizeMrn(compact);
    }

    /**
     * Candidate MRN strings to try against blind indexes (canonical + unpadded variants).
     */
    public List<String> mrnSearchCandidates(String raw) {
        Set<String> candidates = new LinkedHashSet<>();
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        String compact = canonicalizeMrnInput(raw);
        if (MRN_YEAR_SEQ_PATTERN.matcher(compact).matches()) {
            compact = "CL-" + compact;
        }
        candidates.add(blindIndexService.normalizeMrn(compact));
        String canonical = normalizeMrn(raw);
        if (StringUtils.hasText(canonical)) {
            candidates.add(canonical);
        }
        var matcher = MRN_CANONICAL_PATTERN.matcher(compact.startsWith("CL-") ? compact : "CL-" + compact);
        if (matcher.matches()) {
            candidates.add(String.format(Locale.ROOT, "CL-%s-%s", matcher.group(1), matcher.group(2)));
            candidates.add(String.format(Locale.ROOT, "CL-%s-%04d", matcher.group(1), Integer.parseInt(matcher.group(2))));
        }
        return candidates.stream().filter(StringUtils::hasText).toList();
    }

    /** Upper-case and turn whitespace into hyphens so {@code CL 2026 0001} works. */
    private static String canonicalizeMrnInput(String raw) {
        return raw.trim().replaceAll("\\s+", "-").replaceAll("-{2,}", "-").toUpperCase(Locale.ROOT);
    }

    public String normalizeEmail(String raw) {
        return blindIndexService.normalizeEmail(raw);
    }

    public String normalizeFullName(String raw) {
        return blindIndexService.normalizeFullName(raw);
    }

    public String normalizePhone(String raw) {
        return blindIndexService.normalizePhone(raw);
    }

    public String encryptContactLookup(String plaintext) {
        if (!StringUtils.hasText(plaintext)) {
            return plaintext;
        }
        return encryptionService.encryptDeterministic(
                plaintext, EncryptedSearchableStringConverter.PURPOSE_CONTACT_VALUE);
    }

    public String encryptNameLookup(String plaintext) {
        if (!StringUtils.hasText(plaintext)) {
            return plaintext;
        }
        return encryptionService.encryptDeterministic(
                plaintext, EncryptedSearchableStringConverter.PURPOSE_CLIENT_FULL_NAME);
    }

    public Specification<Client> clientSearchSpecification(String search) {
        if (!StringUtils.hasText(search)) {
            return (root, query, cb) -> cb.conjunction();
        }
        String trimmed = search.trim();
        if (classify(trimmed) == SearchKind.MRN) {
            // A paged query evaluates its specification for both content and count.
            // Resolve encrypted candidates once per search, never in each evaluation.
            List<Long> matchingIds = findClientIdsMatchingMrn(trimmed);
            boolean legacyToo = blindIndexService.getSearchMode() != BlindIndexService.SearchMode.BLIND_ONLY;
            return (root, query, cb) -> mrnPredicate(root, cb, trimmed, legacyToo, matchingIds);
        }
        return (root, query, cb) -> predicateForClientRoot(root, query, cb, trimmed);
    }

    public Predicate predicateForClientRoot(
            Root<Client> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            String search) {
        return buildExactMatchPredicate(root, query, cb, search);
    }

    public Predicate predicateForClientPath(
            Path<Client> clientPath,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            String search) {
        return buildExactMatchPredicate(clientPath, query, cb, search);
    }

    private Predicate buildExactMatchPredicate(
            Path<?> clientPath,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            String search) {
        SearchKind kind = classify(search);
        BlindIndexService.SearchMode mode = blindIndexService.getSearchMode();
        boolean useBlind = mode == BlindIndexService.SearchMode.DUAL
                || mode == BlindIndexService.SearchMode.BLIND_ONLY;
        boolean legacyToo = mode != BlindIndexService.SearchMode.BLIND_ONLY;

        return switch (kind) {
            case DATE_OF_BIRTH -> cb.equal(clientPath.get("dateOfBirthBlindIdx"),
                    blindIndexService.compute(BlindIndexService.Kind.DATE_OF_BIRTH,
                            blindIndexService.normalizeDateOfBirth(parseDateOfBirth(search.trim()))));
            case MRN -> mrnPredicate(clientPath, cb, search, legacyToo, findClientIdsMatchingMrn(search));
            case EMAIL -> emailPredicate(clientPath, query, cb, search, useBlind, legacyToo);
            case PHONE -> phonePredicate(clientPath, query, cb, search, useBlind, legacyToo);
            case FULL_NAME -> namePredicate(clientPath, query, cb, search, useBlind, legacyToo);
        };
    }

    private Predicate mrnPredicate(
            Path<?> clientPath, CriteriaBuilder cb, String search, boolean legacyToo, List<Long> decryptedIds) {
        List<Predicate> parts = new ArrayList<>();
        List<String> candidates = mrnSearchCandidates(search);
        // Prefer blind indexes when present (fast path for full MRNs).
        if (!candidates.isEmpty()) {
            for (String candidate : candidates) {
                byte[] digest = blindIndexService.compute(BlindIndexService.Kind.CLIENT_ID, candidate);
                parts.add(cb.equal(clientPath.get("clientIdBlindIdx"), digest));
            }
        }
        if (legacyToo) {
            for (String candidate : candidates) {
                // Best-effort for pre-encryption tenants; ciphertext equality will not match TFENC values.
                parts.add(cb.equal(cb.upper(clientPath.get("clientId")), candidate));
            }
        }
        // Decrypt scan: covers missing/stale blind indexes and MRN prefixes (e.g. "CL", "CL-2026").
        if (!decryptedIds.isEmpty()) {
            parts.add(clientPath.get("id").in(decryptedIds));
        }
        return orAll(cb, parts);
    }

    /**
     * Match active clients whose decrypted MRN equals a search candidate or contains the
     * normalized query (case-insensitive). Used when blind indexes cannot answer the query.
     */
    public List<Long> findClientIdsMatchingMrn(String search) {
        if (!StringUtils.hasText(search)) {
            return List.of();
        }
        String prefix = canonicalizeMrnInput(search);
        Set<String> exactCandidates = new LinkedHashSet<>(mrnSearchCandidates(search));
        // Full canonical form for exact equality against padded display MRNs.
        String canonicalQuery = normalizeMrn(search);
        if (StringUtils.hasText(canonicalQuery)) {
            exactCandidates.add(canonicalQuery);
        }

        Set<Long> ids = new LinkedHashSet<>();
        for (Object[] row : clientRepository.findActiveClientNumberCandidates()) {
            if (mrnMatchesSearch(displayNumber((String) row[1]), prefix, exactCandidates)
                    || mrnMatchesSearch(displayNumber((String) row[2]), prefix, exactCandidates)) {
                ids.add((Long) row[0]);
            }
        }
        return List.copyOf(ids);
    }

    /** Match encrypted types without hydrating client profiles or associations. */
    public List<Long> findClientIdsMatchingType(String type) {
        return clientRepository.findActiveClientTypeCandidates().stream()
                .filter(row -> type.equalsIgnoreCase(displayNumber((String) row[1])))
                .map(row -> (Long) row[0]).toList();
    }

    private boolean mrnMatchesSearch(String displayed, String prefix, Set<String> exactCandidates) {
        if (!StringUtils.hasText(displayed)) {
            return false;
        }
        String displayedUpper = canonicalizeMrnInput(displayed);
        String displayedCanonical = normalizeMrn(displayed);
        boolean exact = exactCandidates.contains(displayedUpper)
                || (displayedCanonical != null && exactCandidates.contains(displayedCanonical));
        boolean partialMatch = displayedUpper.contains(prefix)
                || (displayedCanonical != null && displayedCanonical.contains(prefix));
        return exact || partialMatch;
    }

    private String displayNumber(String value) {
        if (!StringUtils.hasText(value)) return null;
        return encryptionService.isEncrypted(value) ? encryptionService.decrypt(value) : value;
    }

    private Predicate namePredicate(
            Path<?> clientPath, CriteriaQuery<?> query, CriteriaBuilder cb, String search,
            boolean useBlind, boolean legacyToo) {
        List<Predicate> parts = new ArrayList<>();
        String trimmed = search.trim();
        String normalized = normalizeFullName(trimmed);
        if (useBlind) {
            byte[] digest = blindIndexService.compute(BlindIndexService.Kind.FULL_NAME, normalized);
            parts.add(cb.equal(clientPath.get("fullNameBlindIdx"), digest));
            Predicate tokenMatch = nameTokenPredicate(clientPath, query, cb, normalized);
            if (tokenMatch != null) {
                parts.add(tokenMatch);
            }
        }
        if (legacyToo) {
            String encryptedName = encryptNameLookup(trimmed);
            String encryptedNormalized = encryptNameLookup(normalized);
            parts.add(cb.or(
                    cb.equal(clientPath.get("fullName"), encryptedName),
                    cb.equal(clientPath.get("fullName"), encryptedNormalized),
                    cb.equal(cb.lower(clientPath.get("fullName")), normalized)));
        }
        return orAll(cb, parts);
    }

    /**
     * Exact-match on name tokens (first/last name). Multi-word queries require every distinct
     * token to be present. Returns null when there are no usable tokens.
     */
    private Predicate nameTokenPredicate(
            Path<?> clientPath, CriteriaQuery<?> query, CriteriaBuilder cb, String normalizedFullName) {
        Set<String> tokens = new LinkedHashSet<>();
        Arrays.stream(normalizedFullName.split(" "))
                .filter(StringUtils::hasText)
                .forEach(tokens::add);
        if (tokens.isEmpty()) {
            return null;
        }
        List<Predicate> tokenPreds = new ArrayList<>();
        for (String token : tokens) {
            byte[] digest = blindIndexService.compute(BlindIndexService.Kind.NAME_TOKEN, token);
            Subquery<Long> sub = query.subquery(Long.class);
            Root<ClientNameBlindIndex> tokenRoot = sub.from(ClientNameBlindIndex.class);
            sub.select(cb.literal(1L));
            sub.where(cb.and(
                    cb.equal(tokenRoot.get("client").get("id"), clientPath.get("id")),
                    cb.equal(tokenRoot.get("tokenBlindIdx"), digest)));
            tokenPreds.add(cb.exists(sub));
        }
        if (tokenPreds.size() == 1) {
            return tokenPreds.get(0);
        }
        return cb.and(tokenPreds.toArray(Predicate[]::new));
    }

    private Predicate emailPredicate(
            Path<?> clientPath,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            String search,
            boolean useBlind,
            boolean legacyToo) {
        String normalized = normalizeEmail(search);
        Subquery<Long> sub = query.subquery(Long.class);
        Root<ClientContact> contact = sub.from(ClientContact.class);
        sub.select(cb.literal(1L));
        List<Predicate> valuePreds = new ArrayList<>();
        if (useBlind) {
            byte[] digest = blindIndexService.compute(BlindIndexService.Kind.CONTACT, normalized);
            valuePreds.add(cb.equal(contact.get("contactBlindIdx"), digest));
        }
        if (legacyToo) {
            String encrypted = encryptContactLookup(normalized);
            valuePreds.add(cb.equal(contact.get("contactValue"), encrypted));
            valuePreds.add(cb.equal(cb.lower(contact.get("contactValue")), normalized));
        }
        sub.where(cb.and(
                cb.equal(contact.get("client").get("id"), clientPath.get("id")),
                cb.equal(contact.get("contactType"), ContactType.EMAIL),
                cb.equal(contact.get("isDeleted"), false),
                orAll(cb, valuePreds)));
        return cb.exists(sub);
    }

    private Predicate phonePredicate(
            Path<?> clientPath,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            String search,
            boolean useBlind,
            boolean legacyToo) {
        String normalized = normalizePhone(search);
        Subquery<Long> sub = query.subquery(Long.class);
        Root<ClientContact> contact = sub.from(ClientContact.class);
        sub.select(cb.literal(1L));
        List<Predicate> valuePreds = new ArrayList<>();
        if (useBlind) {
            byte[] digest = blindIndexService.compute(BlindIndexService.Kind.CONTACT, normalized);
            valuePreds.add(cb.equal(contact.get("contactBlindIdx"), digest));
        }
        if (legacyToo) {
            String encrypted = encryptContactLookup(normalized);
            String encryptedRaw = encryptContactLookup(search.trim());
            valuePreds.add(cb.equal(contact.get("contactValue"), encrypted));
            valuePreds.add(cb.equal(contact.get("contactValue"), encryptedRaw));
            valuePreds.add(cb.equal(contact.get("contactValue"), normalized));
            valuePreds.add(cb.equal(contact.get("contactValue"), search.trim()));
        }
        sub.where(cb.and(
                cb.equal(contact.get("client").get("id"), clientPath.get("id")),
                contact.get("contactType").in(ContactType.PHONE, ContactType.WORK_PHONE),
                cb.equal(contact.get("isDeleted"), false),
                orAll(cb, valuePreds)));
        return cb.exists(sub);
    }

    private static Predicate orAll(CriteriaBuilder cb, List<Predicate> parts) {
        if (parts.isEmpty()) {
            return cb.disjunction();
        }
        if (parts.size() == 1) {
            return parts.get(0);
        }
        return cb.or(parts.toArray(Predicate[]::new));
    }

    private static boolean looksLikeMrn(String trimmed) {
        String compact = canonicalizeMrnInput(trimmed);
        if ("CL".equals(compact) || compact.startsWith("CL-")) {
            return true;
        }
        if (MRN_FULL_PATTERN.matcher(compact).matches()) {
            return true;
        }
        return MRN_YEAR_SEQ_PATTERN.matcher(compact).matches()
                || compact.matches("\\d{1,9}");
    }

    private static boolean looksLikePhone(String trimmed) {
        String digits = trimmed.replaceAll("\\D", "");
        return digits.length() >= 10 && digits.length() <= 15;
    }
}

package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientAddress;
import com.smart.therapy.flow.client.entity.ClientContact;
import com.smart.therapy.flow.client.entity.ClientEmployment;
import com.smart.therapy.flow.client.entity.ClientInsurance;
import com.smart.therapy.flow.client.entity.ClientReferral;
import com.smart.therapy.flow.client.enums.AddressType;
import com.smart.therapy.flow.client.enums.ContactType;
import com.smart.therapy.flow.client.enums.ReferralType;
import com.smart.therapy.flow.client.repository.ClientAddressRepository;
import com.smart.therapy.flow.client.repository.ClientContactRepository;
import com.smart.therapy.flow.client.repository.ClientEmploymentRepository;
import com.smart.therapy.flow.client.repository.ClientInsuranceRepository;
import com.smart.therapy.flow.client.repository.ClientReferralRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.service.BlindIndexService;
import com.smart.therapy.flow.common.service.BlindIndexService.Kind;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceClientRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import com.smart.therapy.flow.system.service.SystemOptionKeyMatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientHubClientExecuteService {

    private static final int BATCH_SIZE = 50;

    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final JdbcTemplate jdbcTemplate;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ClientContactRepository contactRepository;
    private final ClientAddressRepository addressRepository;
    private final ClientInsuranceRepository insuranceRepository;
    private final ClientReferralRepository referralRepository;
    private final ClientEmploymentRepository employmentRepository;
    private final BlindIndexService blindIndexService;

    public ClientExecuteResult execute(List<SourceClientRecord> sourceClients, TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException("Exactly one target organisation is required for client execution");
        }

        ClientExecuteResult total = ClientExecuteResult.empty();
        int batchNumber = 0;
        for (int start = 0; start < sourceClients.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, sourceClients.size());
            int currentBatchNumber = ++batchNumber;
            List<SourceClientRecord> batch = sourceClients.subList(start, end);
            ClientExecuteResult batchResult = tenantTransactionExecutor.executeWrite(target.organisationId(), target.schemaName(),
                    () -> executeInTenant(batch, target));
            total = total.plus(batchResult);
            log.info("ClientHubAI client execute batch complete: batch={} batch_size={} processed={} total={}",
                    currentBatchNumber, batch.size(), total.sourceClients(), sourceClients.size());
        }

        return total;
    }

    private ClientExecuteResult executeInTenant(List<SourceClientRecord> sourceClients, TargetInventory target) {
        int created = 0;
        int updated = 0;
        int contacts = 0;
        int addresses = 0;
        int insurance = 0;
        int referrals = 0;
        int employment = 0;

        for (SourceClientRecord source : sourceClients) {
            Optional<Long> mappedClientId = resolveMappedClientId(target, source.legacyClientPk());
            Client client;
            boolean existing;
            if (mappedClientId.isPresent()) {
                // Re-runs must refresh PHI search digests (name tokens / MRN / DOB).
                // Skipping mapped rows left clients searchable by MRN decrypt-scan only.
                client = clientRepository.findById(mappedClientId.get())
                        .or(() -> resolveExistingClientByBlindIndex(source))
                        .orElseGet(Client::new);
                existing = client.getId() != null;
            } else {
                client = resolveExistingClientByBlindIndex(source).orElseGet(Client::new);
                existing = client.getId() != null;
            }

            applyCoreFields(client, source, target);
            blindIndexService.updateBlindIndexes(client, null);
            Client saved = clientRepository.save(client);
            upsertLegacyMapping(target, source, saved.getId());

            contacts += upsertContacts(saved, source, existing);
            addresses += upsertAddress(saved, source, existing);
            insurance += upsertInsurance(saved, source, existing);
            referrals += upsertReferral(saved, source, existing);
            employment += upsertEmployment(saved, source, existing);
            if (existing) {
                updated++;
            } else {
                created++;
            }
        }

        return new ClientExecuteResult(
                sourceClients.size(), created, updated, contacts, addresses, insurance, referrals, employment);
    }

    private void applyCoreFields(Client client, SourceClientRecord source, TargetInventory target) {
        client.setClientId(source.clientId().trim());
        client.setFullName(source.fullName().trim());
        client.setDateOfBirth(sanitizeDateOfBirth(source));
        client.setGender(trim(source.gender()));
        client.setMaritalStatus(trim(source.maritalStatus()));
        client.setPreferredLanguage(trim(source.preferredLanguage()));
        client.setPronouns(trim(source.pronouns()));
        client.setTimezone(trim(source.timezone()));
        client.setStartDate(source.startDate());
        client.setServiceType(trim(source.serviceType()));
        client.setServiceFrequency(trim(source.serviceFrequency()));
        client.setClientType(trim(source.clientType()));
        client.setStatus(normalizeClientStatus(source.status()));
        client.setStage(normalizeClientStage(source.stage()));
        client.setLastUpdateDate(source.lastUpdateDate() == null ? Instant.now() : source.lastUpdateDate());
        client.setNotes(trim(source.notes()));
        client.setIsDeleted(false);
        client.setDeletedAt(null);
        client.setCreatedBy(0L);
        client.setUpdatedBy(0L);

        if (source.assignedTherapistLegacyId() != null) {
            Long therapistId = resolveMappedTenantUserId(target, "users", source.assignedTherapistLegacyId())
                    .orElseThrow(() -> new IllegalStateException("Missing therapist mapping for source client"));
            User therapist = userRepository.getReferenceById(therapistId);
            client.setAssignedTherapist(therapist);
        } else {
            client.setAssignedTherapist(null);
        }
    }

    private int upsertContacts(Client client, SourceClientRecord source, boolean existing) {
        int changed = 0;
        if (hasText(source.email())) {
            ClientContact email = existing
                    ? contactRepository.findPrimaryByClientIdAndContactType(client.getId(), ContactType.EMAIL)
                    .orElseGet(ClientContact::new)
                    : new ClientContact();
            email.setClient(client);
            email.setContactType(ContactType.EMAIL);
            email.setContactValue(source.email().trim());
            email.setLabel("Email");
            email.setIsPrimary(true);
            email.setIsVerified(false);
            email.setDisplayOrder(0);
            email.setIsDeleted(false);
            blindIndexService.applyContactBlindIndex(email);
            contactRepository.save(email);
            changed++;
        }
        if (hasText(source.phone())) {
            ClientContact phone = existing
                    ? contactRepository.findPrimaryByClientIdAndContactType(client.getId(), ContactType.PHONE)
                    .orElseGet(ClientContact::new)
                    : new ClientContact();
            phone.setClient(client);
            phone.setContactType(ContactType.PHONE);
            phone.setContactValue(source.phone().trim());
            phone.setLabel("Phone");
            phone.setIsPrimary(true);
            phone.setIsVerified(false);
            phone.setDisplayOrder(1);
            phone.setIsDeleted(false);
            blindIndexService.applyContactBlindIndex(phone);
            contactRepository.save(phone);
            changed++;
        }
        if (hasText(source.emergencyContactName()) || hasText(source.emergencyContactPhone())) {
            ClientContact emergency = existing
                    ? contactRepository.findEmergencyContactsByClientId(client.getId(), ContactType.EMERGENCY_CONTACT)
                    .stream()
                    .findFirst()
                    .orElseGet(ClientContact::new)
                    : new ClientContact();
            emergency.setClient(client);
            emergency.setContactType(ContactType.EMERGENCY_CONTACT);
            emergency.setContactPersonName(trim(source.emergencyContactName()));
            emergency.setContactValue(trimOrDefault(source.emergencyContactPhone(), "unknown"));
            emergency.setRelationship(trim(source.emergencyContactRelationship()));
            emergency.setLabel("Emergency Contact");
            emergency.setIsPrimary(false);
            emergency.setIsVerified(false);
            emergency.setDisplayOrder(2);
            emergency.setIsDeleted(false);
            blindIndexService.applyContactBlindIndex(emergency);
            contactRepository.save(emergency);
            changed++;
        }
        return changed;
    }

    private int upsertAddress(Client client, SourceClientRecord source, boolean existing) {
        if (!hasText(source.streetAddress1()) && !hasText(source.addressLegacy()) && !hasText(source.city())) {
            return 0;
        }
        ClientAddress address = existing
                ? addressRepository.findPrimaryByClientIdAndAddressType(client.getId(), AddressType.HOME)
                .orElseGet(ClientAddress::new)
                : new ClientAddress();
        address.setClient(client);
        address.setAddressType(AddressType.HOME);
        address.setStreetAddress1(trimOrDefault(source.streetAddress1(), trimOrDefault(source.addressLegacy(), "Unknown")));
        address.setStreetAddress2(trim(source.streetAddress2()));
        address.setCity(trimOrDefault(source.city(), "Unknown"));
        address.setStateProvince(trim(source.province()));
        address.setPostalCode(trim(source.postalCode()));
        address.setCountry(trimOrDefault(source.country(), "United States"));
        address.setAddressLegacy(trim(source.addressLegacy()));
        address.setStateLegacy(trim(source.stateLegacy()));
        address.setZipCodeLegacy(trim(source.zipCodeLegacy()));
        address.setIsPrimary(true);
        address.setIsCurrent(true);
        address.setIsVerified(false);
        address.setDisplayOrder(0);
        address.setIsDeleted(false);
        addressRepository.save(address);
        return 1;
    }

    private int upsertInsurance(Client client, SourceClientRecord source, boolean existing) {
        if (!hasText(source.insuranceProvider()) || !hasText(source.policyNumber())) {
            return 0;
        }
        ClientInsurance insurance = existing
                ? insuranceRepository.findByClientId(client.getId()).orElseGet(ClientInsurance::new)
                : new ClientInsurance();
        insurance.setClient(client);
        insurance.setInsuranceProvider(source.insuranceProvider().trim());
        insurance.setPolicyNumber(source.policyNumber().trim());
        insurance.setGroupNumber(trim(source.groupNumber()));
        insurance.setInsurancePhone(trim(source.insurancePhone()));
        insurance.setCopayAmount(source.copayAmount());
        insurance.setDeductible(source.deductible());
        insurance.setDeductibleMet(java.math.BigDecimal.ZERO);
        insurance.setIsActive(true);
        insurance.setIsVerified(false);
        insurance.setAuthorizationRequired(false);
        insurance.setMentalHealthCoverage(true);
        insurance.setTelehealthCoverage(false);
        insurance.setSessionsUsed(0);
        insurance.setIsDeleted(false);
        insuranceRepository.save(insurance);
        return 1;
    }

    private int upsertReferral(Client client, SourceClientRecord source, boolean existing) {
        if (!hasText(source.referrerName()) && !hasText(source.referringPerson()) && !hasText(source.referralSource())) {
            return 0;
        }
        ClientReferral referral = existing
                ? referralRepository.findByClientId(client.getId()).orElseGet(ClientReferral::new)
                : new ClientReferral();
        referral.setClient(client);
        referral.setReferralDate(sanitizePastOrPresentDate(source.referralDate(), "referral_date", source.legacyClientPk()));
        referral.setReferralSource(trim(source.referralSource()));
        referral.setReferralType(parseReferralType(source.referralType()));
        referral.setReferrerName(trimOrDefault(source.referrerName(), trim(source.referringPerson())));
        referral.setReferenceNumber(trim(source.referenceNumber()));
        referral.setClientSource(trim(source.clientSource()));
        referral.setReferralNotes(trim(source.referralNotes()));
        referral.setIsCourtOrdered(false);
        referral.setRequiresReporting(false);
        referral.setConsentToContactReferrer(false);
        referral.setIsDeleted(false);
        referralRepository.save(referral);
        return 1;
    }

    private int upsertEmployment(Client client, SourceClientRecord source, boolean existing) {
        if (!hasText(source.employmentStatus()) && !hasText(source.educationLevel()) && source.dependents() == null) {
            return 0;
        }
        ClientEmployment employment = existing
                ? employmentRepository.findByClientId(client.getId()).orElseGet(ClientEmployment::new)
                : new ClientEmployment();
        employment.setClient(client);
        employment.setEmploymentStatus(trim(source.employmentStatus()));
        employment.setEducationLevel(trim(source.educationLevel()));
        employment.setDependents(source.dependents());
        employment.setIsCurrentlyEmployed(false);
        employment.setIsStudent(false);
        employment.setFinancialHardship(false);
        employment.setEligibleForSlidingScale(false);
        employment.setVeteranStatus(false);
        employment.setIsDeleted(false);
        employmentRepository.save(employment);
        return 1;
    }

    private Optional<Long> resolveMappedClientId(TargetInventory target, String sourceId) {
        return resolveMappedTenantUserId(target, "clients", sourceId);
    }

    private Optional<Client> resolveExistingClientByBlindIndex(SourceClientRecord source) {
        if (!hasText(source.clientId())) {
            return Optional.empty();
        }
        byte[] clientIdBlindIndex = blindIndexService.compute(
                Kind.CLIENT_ID,
                blindIndexService.normalizeMrn(source.clientId()));
        return clientRepository.findByClientIdBlindIdxAndIsDeletedFalse(clientIdBlindIndex);
    }

    private Optional<Long> resolveMappedTenantUserId(TargetInventory target, String entityName, String sourceId) {
        List<Long> ids = jdbcTemplate.query("""
                SELECT target_id
                FROM public.clienthub_legacy_id_mappings
                WHERE organisation_id = ?
                  AND source_system = 'ClientHubAI'
                  AND entity_name = ?
                  AND source_id = ?
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getLong("target_id"),
                target.organisationId(),
                entityName,
                sourceId);
        return ids.stream().findFirst();
    }

    private void upsertLegacyMapping(TargetInventory target, SourceClientRecord source, Long clientId) {
        jdbcTemplate.update("""
                INSERT INTO public.clienthub_legacy_id_mappings (
                    organisation_id, source_system, entity_name, source_id,
                    target_schema, target_table, target_id, source_checksum_sha256,
                    first_seen_run_id, last_seen_run_id
                )
                VALUES (?, 'ClientHubAI', 'clients', ?, ?, 'clients', ?, ?, NULL, NULL)
                ON CONFLICT (organisation_id, source_system, entity_name, source_id)
                DO UPDATE SET
                    target_schema = EXCLUDED.target_schema,
                    target_table = EXCLUDED.target_table,
                    target_id = EXCLUDED.target_id,
                    source_checksum_sha256 = EXCLUDED.source_checksum_sha256,
                    updated_at = CURRENT_TIMESTAMP
                """,
                target.organisationId(),
                source.legacyClientPk(),
                target.schemaName(),
                clientId,
                checksum(source));
    }

    LocalDate sanitizeDateOfBirth(SourceClientRecord source) {
        LocalDate dateOfBirth = source.dateOfBirth();
        if (dateOfBirth == null) {
            return null;
        }
        // Bean Validation @Past rejects today and future dates; keep import moving by nulling them.
        if (!dateOfBirth.isBefore(LocalDate.now())) {
            log.warn("ClientHubAI client date_of_birth not in the past; storing null. source_client_pk={}",
                    source.legacyClientPk());
            return null;
        }
        return dateOfBirth;
    }

    LocalDate sanitizePastOrPresentDate(LocalDate value, String fieldName, String sourceClientPk) {
        if (value == null) {
            return null;
        }
        // Bean Validation @PastOrPresent rejects future dates.
        if (value.isAfter(LocalDate.now())) {
            log.warn("ClientHubAI client {} is in the future; storing null. source_client_pk={}",
                    fieldName, sourceClientPk);
            return null;
        }
        return value;
    }

    private ReferralType parseReferralType(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return ReferralType.valueOf(value.trim().toUpperCase().replace('-', '_').replace(' ', '_'));
        } catch (IllegalArgumentException ex) {
            return ReferralType.EXTERNAL;
        }
    }

    /**
     * Map ClientHubAI status labels onto SmartHub {@code client_status} keys.
     * Legacy "closed" is a file-closed status in V1; SmartHub stores that as inactive.
     */
    private String normalizeClientStatus(String raw) {
        String normalized = SystemOptionKeyMatcher.normalize(trimOrDefault(raw, "active"));
        return switch (normalized) {
            case "closed", "close", "file_closed", "discharged", "discharge" -> "inactive";
            case "onhold", "on_hold", "hold" -> "on_hold";
            case "waitlist", "waiting", "wait_list" -> "waitlist";
            case "pending", "intake" -> "pending";
            case "inactive", "active" -> normalized;
            default -> normalized;
        };
    }

    private String normalizeClientStage(String raw) {
        if (!hasText(raw)) {
            return null;
        }
        String normalized = SystemOptionKeyMatcher.normalize(raw);
        return switch (normalized) {
            case "closed", "close", "file_closed" -> "closed";
            case "psychotherapy", "therapy", "treatment", "active_treatment" -> "psychotherapy";
            case "assessment", "assess" -> "assessment";
            case "intake", "new" -> "intake";
            default -> normalized;
        };
    }

    private String checksum(SourceClientRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyClientPk(),
                source.clientId(),
                source.assignedTherapistLegacyId() == null ? "" : source.assignedTherapistLegacyId(),
                source.status() == null ? "" : source.status(),
                source.stage() == null ? "" : source.stage()));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimOrDefault(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    public record ClientExecuteResult(
            int sourceClients,
            int created,
            int updated,
            int contactsUpserted,
            int addressesUpserted,
            int insuranceUpserted,
            int referralsUpserted,
            int employmentUpserted) {

        static ClientExecuteResult empty() {
            return new ClientExecuteResult(0, 0, 0, 0, 0, 0, 0, 0);
        }

        ClientExecuteResult plus(ClientExecuteResult other) {
            return new ClientExecuteResult(
                    sourceClients + other.sourceClients,
                    created + other.created,
                    updated + other.updated,
                    contactsUpserted + other.contactsUpserted,
                    addressesUpserted + other.addressesUpserted,
                    insuranceUpserted + other.insuranceUpserted,
                    referralsUpserted + other.referralsUpserted,
                    employmentUpserted + other.employmentUpserted);
        }
    }
}

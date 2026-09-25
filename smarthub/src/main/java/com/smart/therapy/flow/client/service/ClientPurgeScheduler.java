package com.smart.therapy.flow.client.service;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientAddress;
import com.smart.therapy.flow.client.entity.ClientContact;
import com.smart.therapy.flow.client.repository.ClientAddressRepository;
import com.smart.therapy.flow.client.repository.ClientContactRepository;
import com.smart.therapy.flow.client.repository.ClientInsuranceRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.tenant.TenantExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Anonymizes soft-deleted clients (and their contact/address/insurance dependents) once they
 * are past the configured retention window. Never hard-deletes client rows and never drops the
 * tenant schema - PHI columns are overwritten with placeholder values so foreign-key integrity
 * (sessions, notes, billing, audit trail, etc.) is preserved.
 *
 * Disabled by default; enable only when the retention policy is approved for an environment.
 * Mirrors {@code TranscriptRetentionScheduler}'s tenant-iteration + feature-flag style.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClientPurgeScheduler {

    private static final String PURGED_TEXT = "PURGED";
    private static final String PURGED_ID_PREFIX = "PURGED-";

    private final ClientRepository clientRepository;
    private final ClientContactRepository clientContactRepository;
    private final ClientAddressRepository clientAddressRepository;
    private final ClientInsuranceRepository clientInsuranceRepository;
    private final TenantExecutionService tenantExecutionService;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.client-purge.enabled:false}")
    private boolean purgeEnabled;

    @Value("${app.client-purge.retention-days:30}")
    private int retentionDays;

    @Value("${app.client-purge.dry-run:false}")
    private boolean dryRun;

    @Scheduled(cron = "${app.client-purge.cron:0 0 4 * * ?}")
    public void purgeSoftDeletedClients() {
        if (!purgeEnabled) {
            return;
        }
        tenantExecutionService.runForEachActiveTenant("client-purge", tenant ->
                transactionTemplate.execute(status -> {
                    purgeSoftDeletedClientsForTenant();
                    return null;
                })
        );
    }

    protected void purgeSoftDeletedClientsForTenant() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

        List<Client> candidates = clientRepository.findSoftDeletedBefore(cutoff).stream()
                .filter(client -> !isAlreadyPurged(client))
                .toList();

        if (candidates.isEmpty()) {
            return;
        }

        if (dryRun) {
            log.info("Client purge dry-run: {} soft-deleted client(s) past {}-day retention would be anonymized",
                    candidates.size(), retentionDays);
            return;
        }

        int purged = 0;
        for (Client client : candidates) {
            anonymizeClient(client);
            purged++;
        }
        log.info("Client purge anonymized {} soft-deleted client(s) past {}-day retention", purged, retentionDays);
    }

    private boolean isAlreadyPurged(Client client) {
        return PURGED_TEXT.equals(client.getFullName());
    }

    private void anonymizeClient(Client client) {
        anonymizeContacts(client.getId());
        anonymizeAddresses(client.getId());
        anonymizeInsurance(client.getId());

        client.setFullName(PURGED_TEXT);
        client.setFullNameBlindIdx(null);
        if (client.getNameBlindIndexes() != null) {
            client.getNameBlindIndexes().clear();
        }
        client.setClientId(PURGED_ID_PREFIX + client.getId());
        client.setClientIdBlindIdx(null);
        client.setDateOfBirth(null);
        client.setDateOfBirthBlindIdx(null);
        client.setGender(null);
        client.setMaritalStatus(null);
        client.setPreferredLanguage(null);
        client.setPronouns(null);
        client.setServiceType(null);
        client.setServiceFrequency(null);
        client.setTreatmentModality(null);
        client.setClientType(null);
        client.setFollowUpNotes(null);
        client.setNotes(null);
        client.updateLastUpdateDate();

        clientRepository.save(client);
    }

    private void anonymizeContacts(Long clientId) {
        List<ClientContact> contacts = clientContactRepository.findByClientId(clientId);
        for (ClientContact contact : contacts) {
            contact.setContactValue(PURGED_TEXT);
            contact.setContactBlindIdx(null);
            contact.setContactPersonName(null);
            contact.setRelationship(null);
            contact.setLabel(null);
            contact.setNotes(null);
            contact.setIsPrimary(false);
            contact.setIsVerified(false);
            clientContactRepository.save(contact);
        }
    }

    private void anonymizeAddresses(Long clientId) {
        List<ClientAddress> addresses = clientAddressRepository.findByClientId(clientId);
        for (ClientAddress address : addresses) {
            address.setStreetAddress1(PURGED_TEXT);
            address.setStreetAddress2(null);
            address.setCity(PURGED_TEXT);
            address.setStateProvince(null);
            address.setPostalCode(null);
            address.setCountry(PURGED_TEXT);
            address.setNotes(null);
            address.setAddressLegacy(null);
            address.setStateLegacy(null);
            address.setZipCodeLegacy(null);
            address.setIsPrimary(false);
            address.setIsVerified(false);
            clientAddressRepository.save(address);
        }
    }

    private void anonymizeInsurance(Long clientId) {
        clientInsuranceRepository.findByClientId(clientId).ifPresent(insurance -> {
            insurance.setInsuranceProvider(PURGED_TEXT);
            insurance.setInsuranceType(null);
            insurance.setPolicyNumber(PURGED_ID_PREFIX + insurance.getId());
            insurance.setGroupNumber(null);
            insurance.setSubscriberName(null);
            insurance.setSubscriberRelationship(null);
            insurance.setInsurancePhone(null);
            insurance.setInsuranceEmail(null);
            insurance.setVerifiedBy(null);
            insurance.setAuthorizationNumber(null);
            insurance.setNotes(null);
            insurance.setIsActive(false);
            clientInsuranceRepository.save(insurance);
        });
    }
}

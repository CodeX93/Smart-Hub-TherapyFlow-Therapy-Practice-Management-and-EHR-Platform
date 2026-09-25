package com.smart.therapy.flow.migration.clienthub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientPortalSettings;
import com.smart.therapy.flow.client.entity.PatientConsent;
import com.smart.therapy.flow.client.enums.ConsentStatus;
import com.smart.therapy.flow.client.enums.ConsentType;
import com.smart.therapy.flow.client.repository.ClientPortalSettingsRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.repository.PatientConsentRepository;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceChecklistItemRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceChecklistTemplateRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceClientChecklistItemRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceClientChecklistRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceClientPortalRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourcePatientConsentRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceSupervisorAssignmentRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceUserZoomIntegrationRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import com.smart.therapy.flow.task.entity.ChecklistItem;
import com.smart.therapy.flow.task.entity.ChecklistTemplate;
import com.smart.therapy.flow.task.entity.ClientChecklist;
import com.smart.therapy.flow.task.entity.ClientChecklistItem;
import com.smart.therapy.flow.task.enums.CheckListCategory;
import com.smart.therapy.flow.task.repository.ChecklistItemRepository;
import com.smart.therapy.flow.task.repository.ChecklistTemplateRepository;
import com.smart.therapy.flow.task.repository.ClientChecklistItemRepository;
import com.smart.therapy.flow.task.repository.ClientChecklistRepository;
import com.smart.therapy.flow.user.dto.AssignmentType;
import com.smart.therapy.flow.user.dto.RequiredMeetingFrequency;
import com.smart.therapy.flow.user.entity.SupervisorAssignment;
import com.smart.therapy.flow.user.entity.UserIntegration;
import com.smart.therapy.flow.user.repository.SupervisorAssignmentRepository;
import com.smart.therapy.flow.user.repository.UserIntegrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ClientHubClinicalExtrasExecuteService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int BATCH_SIZE = 25;

    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final JdbcTemplate jdbcTemplate;
    private final PatientConsentRepository patientConsentRepository;
    private final SupervisorAssignmentRepository supervisorAssignmentRepository;
    private final ClientRepository clientRepository;
    private final ClientPortalSettingsRepository clientPortalSettingsRepository;
    private final ChecklistTemplateRepository checklistTemplateRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final ClientChecklistRepository clientChecklistRepository;
    private final ClientChecklistItemRepository clientChecklistItemRepository;
    private final UserIntegrationRepository userIntegrationRepository;
    private final UserRepository userRepository;

    public ClinicalExtrasExecuteResult execute(
            List<SourcePatientConsentRecord> consents,
            List<SourceSupervisorAssignmentRecord> supervisors,
            List<SourceClientPortalRecord> portals,
            List<SourceChecklistTemplateRecord> templates,
            List<SourceChecklistItemRecord> checklistItems,
            List<SourceClientChecklistRecord> clientChecklists,
            List<SourceClientChecklistItemRecord> clientChecklistItems,
            List<SourceUserZoomIntegrationRecord> zoomIntegrations,
            TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException(
                    "Exactly one target organisation is required for clinical extras execution");
        }

        // Load shared mappings once, then commit each stage/batch separately so an Azure
        // connection drop cannot roll back already-imported clinical extras rows.
        Map<String, Long> clientMappings = loadMappingsOutsideTenant(target.organisationId(), "clients");
        Map<String, Long> userMappings = loadMappingsOutsideTenant(target.organisationId(), "users");
        Long defaultStaffUserId = resolveLowestMappedStaffUserId(userMappings);

        Counts consentCounts = runBatched(
                "patient_consents",
                consents,
                batch -> tenantTransactionExecutor.executeWrite(target.organisationId(), target.schemaName(), () -> {
                    Map<String, Long> consentMappings = loadMappings(target.organisationId(), "patient_consents");
                    return upsertConsents(batch, target, clientMappings, consentMappings, defaultStaffUserId);
                }));

        Counts supervisorCounts = runBatched(
                "supervisor_assignments",
                supervisors,
                batch -> tenantTransactionExecutor.executeWrite(target.organisationId(), target.schemaName(), () -> {
                    Map<String, Long> supervisorMappings =
                            loadMappings(target.organisationId(), "supervisor_assignments");
                    return upsertSupervisors(batch, target, userMappings, supervisorMappings);
                }));

        Counts portalCounts = runBatched(
                "client_portal",
                portals,
                batch -> tenantTransactionExecutor.executeWrite(target.organisationId(), target.schemaName(), () -> {
                    Map<String, Long> portalMappings = loadMappings(target.organisationId(), "client_portal");
                    return upsertPortals(batch, target, clientMappings, portalMappings);
                }));

        Counts templateCounts = runBatched(
                "checklist_templates",
                templates,
                batch -> tenantTransactionExecutor.executeWrite(target.organisationId(), target.schemaName(), () -> {
                    Map<String, Long> templateMappings = loadMappings(target.organisationId(), "checklist_templates");
                    Map<String, Long> templatesByName = loadChecklistTemplateIdsByName();
                    return upsertChecklistTemplates(batch, target, templateMappings, templatesByName);
                }));

        Counts itemCounts = runBatched(
                "checklist_items",
                checklistItems,
                batch -> tenantTransactionExecutor.executeWrite(target.organisationId(), target.schemaName(), () -> {
                    Map<String, Long> templateMappings = loadMappings(target.organisationId(), "checklist_templates");
                    Map<String, Long> itemMappings = loadMappings(target.organisationId(), "checklist_items");
                    return upsertChecklistItems(batch, target, templateMappings, itemMappings);
                }));

        Counts clientChecklistCounts = runBatched(
                "client_checklists",
                clientChecklists,
                batch -> tenantTransactionExecutor.executeWrite(target.organisationId(), target.schemaName(), () -> {
                    Map<String, Long> templateMappings = loadMappings(target.organisationId(), "checklist_templates");
                    Map<String, Long> clientChecklistMappings =
                            loadMappings(target.organisationId(), "client_checklists");
                    return upsertClientChecklists(
                            batch, target, clientMappings, userMappings, templateMappings, clientChecklistMappings);
                }));

        Counts clientChecklistItemCounts = runBatched(
                "client_checklist_items",
                clientChecklistItems,
                batch -> tenantTransactionExecutor.executeWrite(target.organisationId(), target.schemaName(), () -> {
                    Map<String, Long> itemMappings = loadMappings(target.organisationId(), "checklist_items");
                    Map<String, Long> clientChecklistMappings =
                            loadMappings(target.organisationId(), "client_checklists");
                    Map<String, Long> clientChecklistItemMappings =
                            loadMappings(target.organisationId(), "client_checklist_items");
                    return upsertClientChecklistItems(
                            batch, target, userMappings, itemMappings,
                            clientChecklistMappings, clientChecklistItemMappings);
                }));

        Counts zoomCounts = runBatched(
                "user_integrations_zoom",
                zoomIntegrations,
                batch -> tenantTransactionExecutor.executeWrite(target.organisationId(), target.schemaName(), () -> {
                    Map<String, Long> zoomMappings = loadMappings(target.organisationId(), "user_integrations_zoom");
                    return upsertZoomIntegrations(batch, target, userMappings, zoomMappings);
                }));

        return new ClinicalExtrasExecuteResult(
                consents.size(), consentCounts.created, consentCounts.updated, consentCounts.mappedReruns,
                supervisors.size(), supervisorCounts.created, supervisorCounts.updated, supervisorCounts.mappedReruns,
                portals.size(), portalCounts.created, portalCounts.updated, portalCounts.mappedReruns,
                portalCounts.passwordResetRequired,
                templates.size(), templateCounts.created, templateCounts.updated, templateCounts.mappedReruns,
                checklistItems.size(), itemCounts.created, itemCounts.updated, itemCounts.mappedReruns,
                clientChecklists.size(), clientChecklistCounts.created, clientChecklistCounts.updated,
                clientChecklistCounts.mappedReruns,
                clientChecklistItems.size(), clientChecklistItemCounts.created, clientChecklistItemCounts.updated,
                clientChecklistItemCounts.mappedReruns,
                zoomIntegrations.size(), zoomCounts.created, zoomCounts.updated, zoomCounts.mappedReruns);
    }

    private <T> Counts runBatched(String stage, List<T> rows, java.util.function.Function<List<T>, Counts> worker) {
        Counts total = new Counts();
        if (rows.isEmpty()) {
            return total;
        }
        int batchNumber = 0;
        for (int start = 0; start < rows.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, rows.size());
            int currentBatch = ++batchNumber;
            List<T> batch = rows.subList(start, end);
            Counts batchCounts = runBatchWithRetry(stage, currentBatch, batch, worker);
            total.add(batchCounts);
            log.info("ClientHubAI clinical extras batch complete: stage={} batch={} batch_size={} processed={}/{}",
                    stage, currentBatch, batch.size(), end, rows.size());
        }
        return total;
    }

    private <T> Counts runBatchWithRetry(
            String stage,
            int batchNumber,
            List<T> batch,
            java.util.function.Function<List<T>, Counts> worker) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return worker.apply(batch);
            } catch (RuntimeException ex) {
                lastFailure = ex;
                if (attempt >= 3 || !isTransientDbFailure(ex)) {
                    throw ex;
                }
                log.warn("ClientHubAI clinical extras batch failed (stage={} batch={} attempt={}/3); retrying. cause={}",
                        stage, batchNumber, attempt, ex.getMessage());
                try {
                    Thread.sleep(3_000L * attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }
        throw lastFailure;
    }

    private boolean isTransientDbFailure(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof org.springframework.dao.DataAccessResourceFailureException
                    || current instanceof org.springframework.dao.TransientDataAccessException
                    || current instanceof java.net.SocketException
                    || current instanceof java.net.SocketTimeoutException
                    || current instanceof java.io.IOException
                    || (current.getMessage() != null && (
                    current.getMessage().contains("I/O error")
                            || current.getMessage().contains("Connection is closed")
                            || current.getMessage().contains("This connection has been closed")))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private Map<String, Long> loadMappingsOutsideTenant(Long organisationId, String entityName) {
        return loadMappings(organisationId, entityName);
    }

    private Counts upsertConsents(
            List<SourcePatientConsentRecord> consents,
            TargetInventory target,
            Map<String, Long> clientMappings,
            Map<String, Long> consentMappings,
            Long defaultStaffUserId) {
        if (defaultStaffUserId == null && !consents.isEmpty()) {
            throw new IllegalStateException("Cannot migrate consents without at least one mapped staff user");
        }
        Counts counts = new Counts();
        for (SourcePatientConsentRecord source : consents) {
            Optional<Long> mappedId = Optional.ofNullable(consentMappings.get(source.legacyConsentPk()));
            if (mappedId.isPresent()) {
                counts.record(true, true);
                continue;
            }
            PatientConsent consent = new PatientConsent();
            boolean existing = false;

            consent.setClient(clientRepository.getReferenceById(
                    requiredMapping(clientMappings, source.clientLegacyId(), "clients")));
            consent.setCreatedByUser(userRepository.getReferenceById(defaultStaffUserId));
            consent.setConsentType(mapConsentType(source.consentType()));
            consent.setGranted(source.granted());
            consent.setConsentStatus(source.granted() ? ConsentStatus.GRANTED : ConsentStatus.WITHDRAWN);
            consent.setGrantedAt(source.grantedAt());
            consent.setConsentDate(source.grantedAt().atZone(ZoneOffset.UTC).toLocalDate());
            consent.setWithdrawnAt(source.withdrawnAt());
            consent.setIpAddress(trimTo(source.ipAddress(), 45));
            consent.setUserAgent(trim(source.userAgent()));
            consent.setNotes(trim(source.notes()));
            consent.setConsentFormVersion(trimTo(source.consentVersion(), 50));
            consent.setIsDeleted(false);
            consent.setDeletedAt(null);
            consent.setCreatedBy(0L);
            consent.setUpdatedBy(0L);

            PatientConsent saved = patientConsentRepository.save(consent);
            consentMappings.put(source.legacyConsentPk(), saved.getId());
            upsertLegacyMapping(target, "patient_consents", "patient_consents",
                    source.legacyConsentPk(), saved.getId(), consentChecksum(source));
            counts.record(false, false);
        }
        return counts;
    }

    private Counts upsertSupervisors(
            List<SourceSupervisorAssignmentRecord> supervisors,
            TargetInventory target,
            Map<String, Long> userMappings,
            Map<String, Long> supervisorMappings) {
        Counts counts = new Counts();
        for (SourceSupervisorAssignmentRecord source : supervisors) {
            Optional<Long> mappedId = Optional.ofNullable(supervisorMappings.get(source.legacyAssignmentPk()));
            if (mappedId.isPresent()) {
                counts.record(true, true);
                continue;
            }
            SupervisorAssignment assignment = new SupervisorAssignment();
            boolean existing = false;

            LocalDate startDate = source.assignedDate().atZone(ZoneOffset.UTC).toLocalDate();
            assignment.setSupervisor(userRepository.getReferenceById(
                    requiredMapping(userMappings, source.supervisorLegacyId(), "users")));
            assignment.setTherapist(userRepository.getReferenceById(
                    requiredMapping(userMappings, source.therapistLegacyId(), "users")));
            assignment.setAssignmentType(AssignmentType.PRIMARY);
            assignment.setStartDate(startDate);
            assignment.setAssignedDate(source.assignedDate());
            assignment.setNotes(trim(source.notes()));
            assignment.setRequiredMeetingFrequency(mapMeetingFrequency(source.requiredMeetingFrequency()));
            assignment.setNextMeetingDate(source.nextMeetingDate());
            assignment.setLastMeetingDate(source.lastMeetingDate());
            if (!source.active()) {
                assignment.setEndDate(LocalDate.now(ZoneOffset.UTC).minusDays(1));
            } else {
                assignment.setEndDate(null);
            }
            assignment.setIsDeleted(false);
            assignment.setDeletedAt(null);
            assignment.setCreatedBy(0L);
            assignment.setUpdatedBy(0L);

            SupervisorAssignment saved = supervisorAssignmentRepository.save(assignment);
            supervisorMappings.put(source.legacyAssignmentPk(), saved.getId());
            upsertLegacyMapping(target, "supervisor_assignments", "supervisor_assignments",
                    source.legacyAssignmentPk(), saved.getId(), supervisorChecksum(source));
            counts.record(false, false);
        }
        return counts;
    }

    private Counts upsertPortals(
            List<SourceClientPortalRecord> portals,
            TargetInventory target,
            Map<String, Long> clientMappings,
            Map<String, Long> portalMappings) {
        Counts counts = new Counts();
        Long clientRoleId = requireRoleId("CLIENT");
        String clientsTable = ClientHubIdentifier.qualified(target.schemaName(), "clients");
        Map<String, Long> authIdsByEmail = new HashMap<>();
        for (SourceClientPortalRecord source : portals) {
            if (source.missingRequiredFields()) {
                continue;
            }
            Long targetClientId = clientMappings.get(source.legacyClientPk());
            if (targetClientId == null) {
                continue;
            }
            if (portalMappings.containsKey(source.legacyClientPk())) {
                counts.record(true, true);
                continue;
            }
            // Avoid Client.findById — encrypted DOB fields require the production key ring and would
            // fail under config-provider migration runs. Use JDBC + getReferenceById instead.
            Long existingClientAuthId = jdbcTemplate.query(
                    "SELECT auth_id FROM " + clientsTable + " WHERE id = ?",
                    rs -> rs.next() ? rs.getObject(1, Long.class) : null,
                    targetClientId);
            Optional<Long> mappedAuthId = Optional.empty();
            boolean existing = existingClientAuthId != null;
            if (!source.bcryptCompatiblePassword()) {
                counts.passwordResetRequired++;
            }

            Long preferredAuthId = existingClientAuthId;
            Long authId = upsertClientAuthIdentity(
                    source, target, targetClientId, clientsTable, preferredAuthId, authIdsByEmail);
            if (authId == null) {
                // Unrecoverable email/auth conflict — skip portal link.
                continue;
            }
            ensureRoleAssignment(authId, clientRoleId, target.organisationId());
            linkClientAuthIdentity(target.schemaName(), targetClientId, authId);

            Client clientRef = clientRepository.getReferenceById(targetClientId);
            ClientPortalSettings settings = clientPortalSettingsRepository.findByClientId(targetClientId)
                    .orElseGet(() -> ClientPortalSettings.builder().client(clientRef).build());
            settings.setHasPortalAccess(source.hasPortalAccess());
            boolean activated = source.lastLogin() != null;
            settings.setIsActivated(activated);
            if (activated && settings.getActivatedAt() == null) {
                settings.setActivatedAt(source.lastLogin());
            }
            settings.setIsDeleted(false);
            settings.setDeletedAt(null);
            settings.setCreatedBy(0L);
            settings.setUpdatedBy(0L);
            clientPortalSettingsRepository.save(settings);

            portalMappings.put(source.legacyClientPk(), authId);
            upsertLegacyMapping(target, "client_portal", "auth_identities",
                    source.legacyClientPk(), authId, portalChecksum(source));
            counts.record(existing, false);
        }
        return counts;
    }

    private Long upsertClientAuthIdentity(
            SourceClientPortalRecord source,
            TargetInventory target,
            Long targetClientId,
            String clientsTable,
            Long existingAuthId,
            Map<String, Long> authIdsByEmail) {
        String email = source.resolvedPortalEmail();
        String normalisedEmail = email.trim().toLowerCase(Locale.ROOT);
        boolean resetRequired = !source.bcryptCompatiblePassword();
        String passwordHash = resetRequired ? null : source.portalPassword();
        String fullName = trimTo(nonBlank(source.fullName(), "ClientHub Client " + source.legacyClientPk()), 150);
        boolean emailVerified = source.lastLogin() != null;
        String activationToken = trim(source.activationToken());

        Long resolvedAuthId = existingAuthId;
        // clients.auth_id is unique — never reuse one CLIENT identity for two clients.
        if (resolvedAuthId == null && authIdsByEmail.containsKey(normalisedEmail)) {
            normalisedEmail = uniquePortalEmail(source.legacyClientPk());
            email = normalisedEmail;
        }
        if (resolvedAuthId == null) {
            resolvedAuthId = jdbcTemplate.query(
                    """
                    SELECT id, identity_type
                    FROM public.auth_identities
                    WHERE organisation_id = ?
                      AND normalised_email = ?
                      AND is_deleted = false
                    LIMIT 1
                    """,
                    rs -> {
                        if (!rs.next()) {
                            return null;
                        }
                        String identityType = rs.getString("identity_type");
                        if (identityType != null && !"CLIENT".equalsIgnoreCase(identityType)) {
                            return -1L;
                        }
                        return rs.getLong("id");
                    },
                    target.organisationId(),
                    normalisedEmail);
            if (resolvedAuthId != null && resolvedAuthId < 0) {
                normalisedEmail = uniquePortalEmail(source.legacyClientPk());
                email = normalisedEmail;
                resolvedAuthId = null;
            } else if (resolvedAuthId != null) {
                Long linkedClientId = jdbcTemplate.query(
                        "SELECT id FROM " + clientsTable + " WHERE auth_id = ? LIMIT 1",
                        rs -> rs.next() ? rs.getObject(1, Long.class) : null,
                        resolvedAuthId);
                if (linkedClientId != null && !linkedClientId.equals(targetClientId)) {
                    normalisedEmail = uniquePortalEmail(source.legacyClientPk());
                    email = normalisedEmail;
                    resolvedAuthId = null;
                }
            }
        }

        if (resolvedAuthId != null) {
            jdbcTemplate.update("""
                    UPDATE public.auth_identities
                    SET email = ?,
                        normalised_email = ?,
                        login_identifier = ?,
                        normalised_login_identifier = ?,
                        full_name = ?,
                        is_active = ?,
                        email_verified = ?,
                        password_hash = COALESCE(?, password_hash),
                        password_changed_at = CASE WHEN ?::varchar IS NULL THEN password_changed_at ELSE CURRENT_TIMESTAMP END,
                        must_change_password = ?,
                        email_verification_token = COALESCE(?, email_verification_token),
                        last_successful_login = COALESCE(?, last_successful_login),
                        updated_at = CURRENT_TIMESTAMP,
                        updated_by = 0,
                        is_deleted = false,
                        deleted_at = NULL
                    WHERE id = ?
                      AND organisation_id = ?
                      AND identity_type = 'CLIENT'
                    """,
                    trimTo(email, 255),
                    normalisedEmail,
                    trimTo(email, 255),
                    normalisedEmail,
                    fullName,
                    source.hasPortalAccess(),
                    emailVerified,
                    passwordHash,
                    passwordHash,
                    resetRequired,
                    activationToken,
                    source.lastLogin() != null ? java.sql.Timestamp.from(source.lastLogin()) : null,
                    resolvedAuthId,
                    target.organisationId());
            authIdsByEmail.put(normalisedEmail, resolvedAuthId);
            return resolvedAuthId;
        }

        Long createdId = jdbcTemplate.queryForObject("""
                INSERT INTO public.auth_identities (
                    account_locked, auth_provider, created_at, created_by,
                    email_verification_expiry, email_verification_token, email_verified,
                    failed_login_attempts, identity_type, is_active,
                    last_failed_login, last_password_change_by, last_successful_login,
                    locked_reason, locked_until, login_identifier, login_identifier_changed_at,
                    normalised_login_identifier, password_changed_at, password_hash,
                    password_reset_expiry, password_reset_token, updated_at, version,
                    provider_user_id, sso_enabled, organisation_id, updated_by,
                    is_deleted, deleted_at, full_name, phone, must_change_password,
                    email, normalised_email, username, normalised_username
                )
                VALUES (
                    false, 'LOCAL', CURRENT_TIMESTAMP, 0,
                    NULL, ?, ?, 0, 'CLIENT', ?,
                    NULL, NULL, ?, NULL, NULL, ?, CURRENT_TIMESTAMP,
                    ?, CASE WHEN ?::varchar IS NULL THEN NULL ELSE CURRENT_TIMESTAMP END, ?,
                    NULL, NULL, CURRENT_TIMESTAMP, 0,
                    NULL, false, ?, 0,
                    false, NULL, ?, NULL, ?,
                    ?, ?, NULL, NULL
                )
                RETURNING id
                """,
                Long.class,
                activationToken,
                emailVerified,
                source.hasPortalAccess(),
                source.lastLogin() != null ? java.sql.Timestamp.from(source.lastLogin()) : null,
                trimTo(email, 255),
                normalisedEmail,
                passwordHash,
                passwordHash,
                target.organisationId(),
                fullName,
                resetRequired,
                trimTo(email, 255),
                normalisedEmail);
        authIdsByEmail.put(normalisedEmail, createdId);
        return createdId;
    }

    private String uniquePortalEmail(String legacyClientPk) {
        return ("clienthub-portal-" + legacyClientPk + "@migrated.invalid").toLowerCase(Locale.ROOT);
    }

    private void linkClientAuthIdentity(String schemaName, Long clientId, Long authId) {
        String clientsTable = ClientHubIdentifier.qualified(schemaName, "clients");
        jdbcTemplate.update("""
                UPDATE %s
                SET auth_id = ?,
                    updatedat = CURRENT_TIMESTAMP,
                    updated_by = 0
                WHERE id = ?
                  AND (auth_id IS NULL OR auth_id = ?)
                """.formatted(clientsTable),
                authId,
                clientId,
                authId);
    }

    private Counts upsertChecklistTemplates(
            List<SourceChecklistTemplateRecord> templates,
            TargetInventory target,
            Map<String, Long> templateMappings,
            Map<String, Long> templatesByName) {
        Counts counts = new Counts();
        for (SourceChecklistTemplateRecord source : templates) {
            Optional<Long> mappedId = Optional.ofNullable(templateMappings.get(source.legacyTemplatePk()));
            ChecklistTemplate template = mappedId.flatMap(checklistTemplateRepository::findById)
                    .orElseGet(() -> templatesByName.containsKey(source.name())
                            ? checklistTemplateRepository.findById(templatesByName.get(source.name()))
                            .orElseGet(ChecklistTemplate::new)
                            : new ChecklistTemplate());
            boolean existing = template.getId() != null;

            template.setName(source.name().trim());
            template.setDescription(trim(source.description()));
            template.setClientType(trimTo(source.clientType(), 20));
            template.setIsActive(source.active());
            template.setSortOrder(source.sortOrder() != null ? source.sortOrder() : 0);
            template.setIsSystem(false);
            template.setIsDeleted(false);
            template.setDeletedAt(null);
            template.setCreatedBy(0L);
            template.setUpdatedBy(0L);

            ChecklistTemplate saved = checklistTemplateRepository.save(template);
            templateMappings.put(source.legacyTemplatePk(), saved.getId());
            templatesByName.put(source.name(), saved.getId());
            upsertLegacyMapping(target, "checklist_templates", "checklist_templates",
                    source.legacyTemplatePk(), saved.getId(), checklistTemplateChecksum(source));
            counts.record(existing, mappedId.isPresent());
        }
        return counts;
    }

    private Counts upsertChecklistItems(
            List<SourceChecklistItemRecord> items,
            TargetInventory target,
            Map<String, Long> templateMappings,
            Map<String, Long> itemMappings) {
        Counts counts = new Counts();
        for (SourceChecklistItemRecord source : items) {
            Optional<Long> mappedId = Optional.ofNullable(itemMappings.get(source.legacyItemPk()));
            ChecklistItem item = mappedId.flatMap(checklistItemRepository::findById).orElseGet(ChecklistItem::new);
            boolean existing = item.getId() != null;

            item.setTemplate(checklistTemplateRepository.getReferenceById(
                    requiredMapping(templateMappings, source.templateLegacyId(), "checklist_templates")));
            String title = source.title().trim();
            item.setTitle(trimTo(title, 200));
            item.setItemText(title);
            item.setDescription(trim(source.description()));
            item.setCategory(mapChecklistCategory(source.category()));
            item.setIsRequired(source.required());
            item.setDaysFromStart(source.daysFromStart());
            item.setItemOrder(source.daysFromStart());
            item.setSortOrder(source.sortOrder() != null ? source.sortOrder() : 0);
            item.setIsDeleted(false);
            item.setDeletedAt(null);
            item.setCreatedBy(0L);
            item.setUpdatedBy(0L);

            ChecklistItem saved = checklistItemRepository.save(item);
            itemMappings.put(source.legacyItemPk(), saved.getId());
            upsertLegacyMapping(target, "checklist_items", "checklist_items",
                    source.legacyItemPk(), saved.getId(), checklistItemChecksum(source));
            counts.record(existing, mappedId.isPresent());
        }
        return counts;
    }

    private Counts upsertClientChecklists(
            List<SourceClientChecklistRecord> checklists,
            TargetInventory target,
            Map<String, Long> clientMappings,
            Map<String, Long> userMappings,
            Map<String, Long> templateMappings,
            Map<String, Long> checklistMappings) {
        Counts counts = new Counts();
        for (SourceClientChecklistRecord source : checklists) {
            Optional<Long> mappedId = Optional.ofNullable(checklistMappings.get(source.legacyChecklistPk()));
            ClientChecklist checklist = mappedId.flatMap(clientChecklistRepository::findById)
                    .orElseGet(ClientChecklist::new);
            boolean existing = checklist.getId() != null;

            checklist.setClient(clientRepository.getReferenceById(
                    requiredMapping(clientMappings, source.clientLegacyId(), "clients")));
            checklist.setTemplate(checklistTemplateRepository.getReferenceById(
                    requiredMapping(templateMappings, source.templateLegacyId(), "checklist_templates")));
            checklist.setIsCompleted(source.completed());
            checklist.setCompletedAt(source.completedAt());
            if (source.completedByLegacyId() != null && !source.completedByLegacyId().isBlank()) {
                checklist.setCompletedBy(userRepository.getReferenceById(
                        requiredMapping(userMappings, source.completedByLegacyId(), "users")));
            } else {
                checklist.setCompletedBy(null);
            }
            checklist.setNotes(trim(source.notes()));
            checklist.setDueDate(source.dueDate());
            checklist.setIsDeleted(false);
            checklist.setDeletedAt(null);
            checklist.setCreatedBy(0L);
            checklist.setUpdatedBy(0L);

            ClientChecklist saved = clientChecklistRepository.save(checklist);
            checklistMappings.put(source.legacyChecklistPk(), saved.getId());
            upsertLegacyMapping(target, "client_checklists", "client_checklists",
                    source.legacyChecklistPk(), saved.getId(), clientChecklistChecksum(source));
            counts.record(existing, mappedId.isPresent());
        }
        return counts;
    }

    private Counts upsertClientChecklistItems(
            List<SourceClientChecklistItemRecord> items,
            TargetInventory target,
            Map<String, Long> userMappings,
            Map<String, Long> itemMappings,
            Map<String, Long> clientChecklistMappings,
            Map<String, Long> clientChecklistItemMappings) {
        Counts counts = new Counts();
        for (SourceClientChecklistItemRecord source : items) {
            Optional<Long> mappedId = Optional.ofNullable(
                    clientChecklistItemMappings.get(source.legacyChecklistItemPk()));
            ClientChecklistItem item = mappedId.flatMap(clientChecklistItemRepository::findById)
                    .orElseGet(ClientChecklistItem::new);
            boolean existing = item.getId() != null;

            item.setClientChecklist(clientChecklistRepository.getReferenceById(
                    requiredMapping(clientChecklistMappings, source.clientChecklistLegacyId(), "client_checklists")));
            item.setChecklistItem(checklistItemRepository.getReferenceById(
                    requiredMapping(itemMappings, source.checklistItemLegacyId(), "checklist_items")));
            item.setIsCompleted(source.completed());
            item.setCompletedAt(source.completedAt());
            if (source.completedByLegacyId() != null && !source.completedByLegacyId().isBlank()) {
                item.setCompletedBy(userRepository.getReferenceById(
                        requiredMapping(userMappings, source.completedByLegacyId(), "users")));
            } else {
                item.setCompletedBy(null);
            }
            item.setNotes(trim(source.notes()));
            item.setIsDeleted(false);
            item.setDeletedAt(null);
            item.setCreatedBy(0L);
            item.setUpdatedBy(0L);

            ClientChecklistItem saved = clientChecklistItemRepository.save(item);
            clientChecklistItemMappings.put(source.legacyChecklistItemPk(), saved.getId());
            upsertLegacyMapping(target, "client_checklist_items", "client_checklist_items",
                    source.legacyChecklistItemPk(), saved.getId(), clientChecklistItemChecksum(source));
            counts.record(existing, mappedId.isPresent());
        }
        return counts;
    }

    private Counts upsertZoomIntegrations(
            List<SourceUserZoomIntegrationRecord> zoomIntegrations,
            TargetInventory target,
            Map<String, Long> userMappings,
            Map<String, Long> zoomMappings) {
        Counts counts = new Counts();
        for (SourceUserZoomIntegrationRecord source : zoomIntegrations) {
            Long targetUserId = requiredMapping(userMappings, source.userLegacyId(), "users");
            Optional<Long> mappedId = Optional.ofNullable(zoomMappings.get(source.userLegacyId()));
            UserIntegration integration = mappedId.flatMap(userIntegrationRepository::findById)
                    .or(() -> userIntegrationRepository.findByUserIdAndIntegrationType(targetUserId, "zoom"))
                    .orElseGet(UserIntegration::new);
            boolean existing = integration.getId() != null;

            integration.setUser(userRepository.getReferenceById(targetUserId));
            integration.setIntegrationType("zoom");
            String externalId = trim(source.zoomAccountId());
            if (externalId == null) {
                externalId = trim(source.zoomClientId());
            }
            if (externalId == null) {
                externalId = "legacy-user-" + source.userLegacyId();
            }
            integration.setExternalUserId(trimTo(externalId, 255));
            integration.setAccessToken(trim(source.zoomAccessToken()));
            integration.setTokenExpiresAt(source.zoomTokenExpiry());
            integration.setTokenType("Bearer");
            integration.setSettings(zoomSettings(source.zoomClientId(), source.zoomClientSecret()));
            integration.setIsActive(true);
            integration.setIsDeleted(false);
            integration.setDeletedAt(null);
            integration.setCreatedBy(0L);
            integration.setUpdatedBy(0L);

            UserIntegration saved = userIntegrationRepository.save(integration);
            zoomMappings.put(source.userLegacyId(), saved.getId());
            upsertLegacyMapping(target, "user_integrations_zoom", "user_integrations",
                    source.userLegacyId(), saved.getId(), zoomChecksum(source));
            counts.record(existing, mappedId.isPresent());
        }
        return counts;
    }

    static ConsentType mapConsentType(String value) {
        if (value == null || value.isBlank()) {
            return ConsentType.OTHER;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (normalized) {
            case "treatment", "consent_for_treatment" -> ConsentType.TREATMENT;
            case "telehealth", "teletherapy" -> ConsentType.TELEHEALTH;
            case "hipaa", "hipaa_privacy", "hipaa_notice", "privacy" -> ConsentType.HIPAA_PRIVACY;
            case "hipaa_authorization", "hipaa_auth" -> ConsentType.HIPAA_AUTHORIZATION;
            case "ai_processing", "ai", "ai_consent" -> ConsentType.AI_PROCESSING;
            case "electronic_records", "ehr" -> ConsentType.ELECTRONIC_RECORDS;
            case "insurance_sharing", "insurance" -> ConsentType.INSURANCE_SHARING;
            case "payment_authorization", "payment" -> ConsentType.PAYMENT_AUTHORIZATION;
            case "research" -> ConsentType.RESEARCH;
            case "training", "supervision" -> ConsentType.TRAINING;
            case "data_sharing" -> ConsentType.DATA_SHARING;
            case "marketing" -> ConsentType.MARKETING;
            case "photography", "photo" -> ConsentType.PHOTOGRAPHY;
            case "audio_recording", "audio" -> ConsentType.AUDIO_RECORDING;
            case "video_recording", "video" -> ConsentType.VIDEO_RECORDING;
            case "email_communication", "email" -> ConsentType.EMAIL_COMMUNICATION;
            case "sms_communication", "sms" -> ConsentType.SMS_COMMUNICATION;
            case "parental_consent", "parental", "guardian" -> ConsentType.PARENTAL_CONSENT;
            case "emergency_contact" -> ConsentType.EMERGENCY_CONTACT;
            default -> {
                try {
                    yield ConsentType.valueOf(normalized.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    yield ConsentType.OTHER;
                }
            }
        };
    }

    static RequiredMeetingFrequency mapMeetingFrequency(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "daily" -> RequiredMeetingFrequency.DAILY;
            case "weekly" -> RequiredMeetingFrequency.WEEKLY;
            case "bi_weekly", "biweekly", "biweekly_" -> RequiredMeetingFrequency.BIWEEKLY;
            case "monthly" -> RequiredMeetingFrequency.MONTHLY;
            case "yearly", "annually", "annual" -> RequiredMeetingFrequency.YEARLY;
            default -> null;
        };
    }

    static CheckListCategory mapChecklistCategory(String value) {
        if (value == null || value.isBlank()) {
            return CheckListCategory.INTAKE;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (normalized) {
            case "assessment", "assessments" -> CheckListCategory.ASSESSMENT;
            case "ongoing", "ongoing_care" -> CheckListCategory.ONGOING;
            case "discharge", "discharged" -> CheckListCategory.DISCHARGE;
            default -> CheckListCategory.INTAKE;
        };
    }

    private ObjectNode zoomSettings(String clientId, String clientSecret) {
        ObjectNode node = OBJECT_MAPPER.createObjectNode();
        if (clientId != null && !clientId.isBlank()) {
            node.put("clientId", clientId.trim());
        }
        if (clientSecret != null && !clientSecret.isBlank()) {
            node.put("clientSecret", clientSecret.trim());
        }
        return node;
    }

    private Long resolveLowestMappedStaffUserId(Map<String, Long> userMappings) {
        return userMappings.entrySet().stream()
                .min(Comparator.comparingLong(entry -> {
                    try {
                        return Long.parseLong(entry.getKey());
                    } catch (NumberFormatException ex) {
                        return Long.MAX_VALUE;
                    }
                }))
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    private Map<String, Long> loadChecklistTemplateIdsByName() {
        Map<String, Long> byName = new HashMap<>();
        for (ChecklistTemplate template : checklistTemplateRepository.findAll()) {
            if (template.getName() != null) {
                byName.putIfAbsent(template.getName(), template.getId());
            }
        }
        return byName;
    }

    private void ensureRoleAssignment(Long authId, Long roleId, Long organisationId) {
        jdbcTemplate.update("""
                INSERT INTO public.auth_identity_roles (
                    created_at, updated_at, version, auth_id, organisation_id, role_id,
                    created_by, updated_by, is_deleted, deleted_at
                )
                SELECT CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, ?, ?, ?, 0, 0, false, NULL
                WHERE NOT EXISTS (
                    SELECT 1
                    FROM public.auth_identity_roles
                    WHERE auth_id = ?
                      AND organisation_id = ?
                      AND role_id = ?
                )
                """, authId, organisationId, roleId, authId, organisationId, roleId);
    }

    private Long requireRoleId(String roleName) {
        List<Long> ids = jdbcTemplate.query("""
                SELECT id
                FROM public.roles
                WHERE name = ?
                  AND is_active = true
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getLong("id"),
                roleName);
        if (ids.isEmpty()) {
            throw new IllegalStateException("Missing active target role: " + roleName);
        }
        return ids.get(0);
    }

    private Map<String, Long> loadMappings(Long organisationId, String entityName) {
        Map<String, Long> mappings = new HashMap<>();
        jdbcTemplate.query("""
                SELECT source_id, target_id
                FROM public.clienthub_legacy_id_mappings
                WHERE organisation_id = ?
                  AND source_system = 'ClientHubAI'
                  AND entity_name = ?
                """,
                (RowCallbackHandler) rs -> mappings.put(rs.getString("source_id"), rs.getLong("target_id")),
                organisationId,
                entityName);
        return mappings;
    }

    private void upsertLegacyMapping(
            TargetInventory target,
            String entityName,
            String targetTable,
            String sourceId,
            Long targetId,
            String checksum) {
        jdbcTemplate.update("""
                INSERT INTO public.clienthub_legacy_id_mappings (
                    organisation_id, source_system, entity_name, source_id,
                    target_schema, target_table, target_id, source_checksum_sha256,
                    first_seen_run_id, last_seen_run_id
                )
                VALUES (?, 'ClientHubAI', ?, ?, ?, ?, ?, ?, NULL, NULL)
                ON CONFLICT (organisation_id, source_system, entity_name, source_id)
                DO UPDATE SET
                    target_schema = EXCLUDED.target_schema,
                    target_table = EXCLUDED.target_table,
                    target_id = EXCLUDED.target_id,
                    source_checksum_sha256 = EXCLUDED.source_checksum_sha256,
                    updated_at = CURRENT_TIMESTAMP
                """,
                target.organisationId(),
                entityName,
                sourceId,
                target.schemaName(),
                targetTable,
                targetId,
                checksum);
    }

    private Long requiredMapping(Map<String, Long> mappings, String sourceId, String entityName) {
        Long targetId = mappings.get(sourceId);
        if (targetId == null) {
            throw new IllegalStateException("Missing ClientHubAI " + entityName + " mapping for source id " + sourceId);
        }
        return targetId;
    }

    private String consentChecksum(SourcePatientConsentRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyConsentPk(),
                source.clientLegacyId(),
                nullToEmpty(source.consentType()),
                nullToEmpty(source.consentVersion()),
                Boolean.toString(source.granted()),
                String.valueOf(source.grantedAt()),
                String.valueOf(source.withdrawnAt())));
    }

    private String supervisorChecksum(SourceSupervisorAssignmentRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyAssignmentPk(),
                source.supervisorLegacyId(),
                source.therapistLegacyId(),
                String.valueOf(source.assignedDate()),
                Boolean.toString(source.active()),
                nullToEmpty(source.requiredMeetingFrequency())));
    }

    private String portalChecksum(SourceClientPortalRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyClientPk(),
                nullToEmpty(source.resolvedPortalEmail()),
                Boolean.toString(source.hasPortalAccess()),
                Boolean.toString(source.bcryptCompatiblePassword()),
                String.valueOf(source.lastLogin()),
                nullToEmpty(source.activationToken())));
    }

    private String checklistTemplateChecksum(SourceChecklistTemplateRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyTemplatePk(),
                nullToEmpty(source.name()),
                nullToEmpty(source.description()),
                nullToEmpty(source.clientType()),
                Boolean.toString(source.active()),
                String.valueOf(source.sortOrder())));
    }

    private String checklistItemChecksum(SourceChecklistItemRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyItemPk(),
                source.templateLegacyId(),
                nullToEmpty(source.title()),
                nullToEmpty(source.category()),
                Boolean.toString(source.required()),
                String.valueOf(source.daysFromStart()),
                String.valueOf(source.sortOrder())));
    }

    private String clientChecklistChecksum(SourceClientChecklistRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyChecklistPk(),
                source.clientLegacyId(),
                source.templateLegacyId(),
                Boolean.toString(source.completed()),
                String.valueOf(source.dueDate())));
    }

    private String clientChecklistItemChecksum(SourceClientChecklistItemRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyChecklistItemPk(),
                source.clientChecklistLegacyId(),
                source.checklistItemLegacyId(),
                Boolean.toString(source.completed())));
    }

    private String zoomChecksum(SourceUserZoomIntegrationRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.userLegacyId(),
                nullToEmpty(source.zoomAccountId()),
                nullToEmpty(source.zoomClientId()),
                String.valueOf(source.zoomTokenExpiry())));
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String trimTo(String value, int maxLength) {
        String trimmed = trim(value);
        if (trimmed == null) {
            return null;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private static String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static final class Counts {
        int created;
        int updated;
        int mappedReruns;
        int passwordResetRequired;

        void record(boolean existing, boolean mappedRerun) {
            if (mappedRerun) {
                mappedReruns++;
            } else if (existing) {
                updated++;
            } else {
                created++;
            }
        }

        void add(Counts other) {
            created += other.created;
            updated += other.updated;
            mappedReruns += other.mappedReruns;
            passwordResetRequired += other.passwordResetRequired;
        }
    }

    record ClinicalExtrasExecuteResult(
            int sourceConsents,
            int consentsCreated,
            int consentsUpdated,
            int consentMappedReruns,
            int sourceSupervisors,
            int supervisorsCreated,
            int supervisorsUpdated,
            int supervisorMappedReruns,
            int sourcePortals,
            int portalsCreated,
            int portalsUpdated,
            int portalMappedReruns,
            int portalPasswordResetRequired,
            int sourceChecklistTemplates,
            int templatesCreated,
            int templatesUpdated,
            int templateMappedReruns,
            int sourceChecklistItems,
            int checklistItemsCreated,
            int checklistItemsUpdated,
            int checklistItemMappedReruns,
            int sourceClientChecklists,
            int clientChecklistsCreated,
            int clientChecklistsUpdated,
            int clientChecklistMappedReruns,
            int sourceClientChecklistItems,
            int clientChecklistItemsCreated,
            int clientChecklistItemsUpdated,
            int clientChecklistItemMappedReruns,
            int sourceZoomIntegrations,
            int zoomCreated,
            int zoomUpdated,
            int zoomMappedReruns) {
    }
}

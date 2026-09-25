package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceStaffAuthInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TableInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.ClientTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceClientInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceClientRef;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SessionTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceSessionInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceSessionNoteRef;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceSessionRef;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.ServiceTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.BillingTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.DocumentTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceDocumentInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceDocumentRef;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceBillingInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceBillingRef;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourcePaymentTransactionRef;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.RoomIntegrationTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceRoomIntegrationInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceRoomRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceSessionIntegrationRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceServiceInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceServiceRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTherapistBlockedTimeRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTherapistScheduleInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceUserProfileScheduleRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TherapistScheduleTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTaskCommentRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTaskInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTaskRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TaskTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.NotificationTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceNotificationInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceNotificationPreferenceRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceNotificationRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceNotificationTemplateRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceNotificationTriggerRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceScheduledNotificationRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourcePatientConsentRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceSupervisorAssignmentRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceClientPortalRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceChecklistTemplateRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceChecklistItemRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceClientChecklistRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceClientChecklistItemRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceUserZoomIntegrationRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceClinicalExtrasInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.ClinicalExtrasTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentsInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentTemplateRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentSectionRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentQuestionRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentQuestionOptionRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentAssignmentRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentResponseRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentReportRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.AssessmentTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTranscriptsInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTranscriptRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TranscriptsTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.DocumentBinariesTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubMigrationReadinessPlanner.ClientHubMigrationReadinessPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubMigrationReadinessPlanner.EntityReadiness;
import com.smart.therapy.flow.migration.clienthub.ClientHubClientMigrationPlanner.ClientMigrationPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubClientExecuteService.ClientExecuteResult;
import com.smart.therapy.flow.migration.clienthub.ClientHubServiceMigrationPlanner.ServiceMigrationPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubServiceExecuteService.ServiceExecuteResult;
import com.smart.therapy.flow.migration.clienthub.ClientHubSessionExecuteService.SessionExecuteResult;
import com.smart.therapy.flow.migration.clienthub.ClientHubSessionMigrationPlanner.SessionMigrationPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubBillingMigrationPlanner.BillingMigrationPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubBillingExecuteService.BillingExecuteResult;
import com.smart.therapy.flow.migration.clienthub.ClientHubDocumentMigrationPlanner.DocumentMigrationPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubDocumentExecuteService.DocumentExecuteResult;
import com.smart.therapy.flow.migration.clienthub.ClientHubRoomIntegrationMigrationPlanner.RoomIntegrationMigrationPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubRoomIntegrationExecuteService.RoomIntegrationExecuteResult;
import com.smart.therapy.flow.migration.clienthub.ClientHubTherapistScheduleMigrationPlanner.TherapistScheduleMigrationPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubTherapistScheduleExecuteService.TherapistScheduleExecuteResult;
import com.smart.therapy.flow.migration.clienthub.ClientHubTaskMigrationPlanner.TaskMigrationPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubTaskExecuteService.TaskExecuteResult;
import com.smart.therapy.flow.migration.clienthub.ClientHubNotificationMigrationPlanner.NotificationMigrationPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubNotificationExecuteService.NotificationExecuteResult;
import com.smart.therapy.flow.migration.clienthub.ClientHubClinicalExtrasMigrationPlanner.ClinicalExtrasMigrationPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubClinicalExtrasExecuteService.ClinicalExtrasExecuteResult;
import com.smart.therapy.flow.migration.clienthub.ClientHubAssessmentsMigrationPlanner.AssessmentsMigrationPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubAssessmentsExecuteService.AssessmentsExecuteResult;
import com.smart.therapy.flow.migration.clienthub.ClientHubTranscriptsMigrationPlanner.TranscriptsMigrationPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubTranscriptsExecuteService.TranscriptsExecuteResult;
import com.smart.therapy.flow.migration.clienthub.ClientHubDocumentBinariesMigrationPlanner.DocumentBinariesMigrationPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubDocumentBinariesExecuteService.DocumentBinariesExecuteResult;
import com.smart.therapy.flow.migration.clienthub.ClientHubSyncScannerService.SyncScanResult;
import com.smart.therapy.flow.migration.clienthub.ClientHubSyncEventProcessorService.SyncProcessResult;
import com.smart.therapy.flow.migration.clienthub.ClientHubMigrationReconciliationService.ReconciliationReport;
import com.smart.therapy.flow.migration.clienthub.ClientHubDemoCleanupPlannerService.DemoCleanupPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubStaffAuthMigrationPlanner.RoleMappingDecision;
import com.smart.therapy.flow.migration.clienthub.ClientHubStaffAuthMigrationPlanner.StaffAuthMigrationPlan;
import com.smart.therapy.flow.migration.clienthub.ClientHubStaffAuthExecuteService.StaffAuthExecuteResult;
import com.smart.therapy.flow.migration.clienthub.ClientHubStaffAuthWritePlanner.StaffAuthWritePlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Order(130)
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "clienthub.migration", name = "enabled", havingValue = "true")
public class ClientHubMigrationRunner implements ApplicationRunner {

    private final ClientHubMigrationProperties properties;
    private final ClientHubSourceInventoryService inventoryService;
    private final ClientHubMigrationReadinessPlanner readinessPlanner;
    private final ClientHubStaffAuthMigrationPlanner staffAuthPlanner;
    private final ClientHubStaffAuthWritePlanner staffAuthWritePlanner;
    private final ClientHubStaffAuthExecuteService staffAuthExecuteService;
    private final ClientHubClientMigrationPlanner clientMigrationPlanner;
    private final ClientHubClientExecuteService clientExecuteService;
    private final ClientHubSessionMigrationPlanner sessionMigrationPlanner;
    private final ClientHubServiceMigrationPlanner serviceMigrationPlanner;
    private final ClientHubServiceExecuteService serviceExecuteService;
    private final ClientHubSessionExecuteService sessionExecuteService;
    private final ClientHubBillingMigrationPlanner billingMigrationPlanner;
    private final ClientHubBillingExecuteService billingExecuteService;
    private final ClientHubDocumentMigrationPlanner documentMigrationPlanner;
    private final ClientHubDocumentExecuteService documentExecuteService;
    private final ClientHubRoomIntegrationMigrationPlanner roomIntegrationMigrationPlanner;
    private final ClientHubRoomIntegrationExecuteService roomIntegrationExecuteService;
    private final ClientHubTherapistScheduleMigrationPlanner therapistScheduleMigrationPlanner;
    private final ClientHubTherapistScheduleExecuteService therapistScheduleExecuteService;
    private final ClientHubTaskMigrationPlanner taskMigrationPlanner;
    private final ClientHubTaskExecuteService taskExecuteService;
    private final ClientHubNotificationMigrationPlanner notificationMigrationPlanner;
    private final ClientHubNotificationExecuteService notificationExecuteService;
    private final ClientHubClinicalExtrasMigrationPlanner clinicalExtrasMigrationPlanner;
    private final ClientHubClinicalExtrasExecuteService clinicalExtrasExecuteService;
    private final ClientHubAssessmentsMigrationPlanner assessmentsMigrationPlanner;
    private final ClientHubAssessmentsExecuteService assessmentsExecuteService;
    private final ClientHubTranscriptsMigrationPlanner transcriptsMigrationPlanner;
    private final ClientHubTranscriptsExecuteService transcriptsExecuteService;
    private final ClientHubDocumentBinariesMigrationPlanner documentBinariesMigrationPlanner;
    private final ClientHubDocumentBinariesExecuteService documentBinariesExecuteService;
    private final ClientHubSyncScannerService syncScannerService;
    private final ClientHubSyncEventProcessorService syncEventProcessorService;
    private final ClientHubMigrationReconciliationService reconciliationService;
    private final ClientHubDemoCleanupPlannerService demoCleanupPlannerService;
    private final ClientHubControlTableReadinessService controlTableReadinessService;
    private final ClientHubMigrationRunRecorder runRecorder;
    private final JdbcTemplate jdbcTemplate;
    private final ConfigurableApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        boolean execute = !properties.isDryRun();
        if (execute && !properties.isExecuteStaffAuth() && !properties.isExecuteClients()
                && !properties.isExecuteServices() && !properties.isExecuteSessions()
                && !properties.isExecuteBilling() && !properties.isExecuteDocuments()
                && !properties.isExecuteRoomIntegrations()
                && !properties.isExecuteTherapistScheduling()
                && !properties.isExecuteTasks()
                && !properties.isExecuteNotifications()
                && !properties.isExecuteClinicalExtras()
                && !properties.isExecuteAssessments()
                && !properties.isExecuteTranscripts()
                && !properties.isExecuteDocumentBinaries()
                && !properties.isSyncEnqueueEvents()
                && !properties.isSyncApplyEvents()) {
            throw new IllegalStateException(
                    "ClientHubAI execute requires at least one scoped execute flag.");
        }

        properties.validateDryRunConfiguration();
        controlTableReadinessService.assertReady();

        SourceInventory source = inventoryService.inspectSource(properties);
        TargetInventory target = inventoryService.inspectTarget(jdbcTemplate, properties);
        ClientHubMigrationReadinessPlan readinessPlan = readinessPlanner.buildPlan(source);
        Long migrationRunId = runRecorder.startRun(source, target, execute);
        boolean completedSuccessfully = false;

        try {
            log.info("ClientHubAI migration dry-run inventory complete: source_schema={} source_tables={} source_rows={} "
                        + "source_schema_fingerprint_sha256={} matching_target_organisations={} demo_tenants_present={} "
                        + "planned_entity_rows={} ready_for_import_design={}",
                source.schemaName(),
                source.tables().size(),
                source.totalRows(),
                source.schemaFingerprintSha256(),
                target.matchingTargetOrganisations(),
                target.demoTenantCount(),
                readinessPlan.totalPlannedRows(),
                readinessPlan.readyForImportDesign());

        for (TableInventory table : source.tables()) {
            log.info("ClientHubAI source table inventory: table={} rows={}", table.tableName(), table.rowCount());
        }

        if (target.matchingTargetOrganisations() != 1) {
            log.warn("ClientHubAI migration dry-run found {} matching target organisation(s); exactly one is required before execution",
                    target.matchingTargetOrganisations());
        }
        if (target.demoTenantCount() > 0) {
            log.warn("ClientHubAI migration dry-run found {} seeded demo tenant(s); cleanup remains approval-gated",
                    target.demoTenantCount());
        }

        for (EntityReadiness entity : readinessPlan.entities()) {
            if (entity.blocked()) {
                log.warn("ClientHubAI migration readiness: entity={} source_table={} target_area={} status=BLOCKED reason={}",
                        entity.entityName(), entity.sourceTable(), entity.targetArea(), entity.reason());
            } else {
                log.info("ClientHubAI migration readiness: entity={} source_table={} target_area={} status=READY rows={}",
                        entity.entityName(), entity.sourceTable(), entity.targetArea(), entity.sourceRows());
            }
            runRecorder.recordOutcome(
                    migrationRunId,
                    target,
                    entity.entityName(),
                    "__readiness__",
                    entity.blocked() ? "BLOCKED" : "NO_OP",
                    entity.blocked() ? "BLOCKED" : "SUCCEEDED",
                    entity.blocked() ? "READINESS_BLOCKED" : null,
                    entity.blocked() ? entity.reason() : null);
        }

        runReconciliationIfReady(source, target);
        runDemoCleanupDryRunIfEnabled();
        runSyncScanIfEnabled(target);

        if (readinessPlan.entities().stream().noneMatch(entity -> "users".equals(entity.entityName()) && !entity.blocked())) {
                log.warn("ClientHubAI staff/auth dry-run skipped because users readiness is blocked");
                completedSuccessfully = true;
                return;
        }

        SourceStaffAuthInventory staffAuthSource = inventoryService.inspectStaffAuthSource(properties);
        Set<String> activeRoleNames = inventoryService.loadPlatformRoleNames(jdbcTemplate);
        StaffAuthMigrationPlan staffAuthPlan = staffAuthPlanner.buildPlan(
                staffAuthSource,
                activeRoleNames);

        log.info("ClientHubAI staff/auth dry-run: users={} bcrypt_compatible_passwords={} reset_required_passwords={} "
                        + "statuses={} blocked={}",
                staffAuthPlan.userRows(),
                staffAuthPlan.bcryptCompatiblePasswordRows(),
                staffAuthPlan.unsupportedPasswordRows(),
                staffAuthPlan.statusCounts(),
                staffAuthPlan.blocked());

        for (RoleMappingDecision mapping : staffAuthPlan.roleMappings()) {
            log.info("ClientHubAI staff/auth role mapping: source_role={} target_role={} rows={} blocked={}",
                    mapping.sourceRole(), mapping.targetRole(), mapping.rows(), mapping.blocked());
        }
        for (String warning : staffAuthPlan.warnings()) {
            log.warn("ClientHubAI staff/auth warning: {}", warning);
        }
        for (String blocker : staffAuthPlan.blockers()) {
            log.warn("ClientHubAI staff/auth blocker: {}", blocker);
        }

        if (!target.resolved()) {
                log.warn("ClientHubAI staff/auth write dry-run skipped because target organisation is not uniquely resolved");
                completedSuccessfully = true;
                return;
        }
        if (staffAuthPlan.blocked()) {
                log.warn("ClientHubAI staff/auth write dry-run skipped because staff/auth readiness has blockers");
                completedSuccessfully = true;
                return;
        }

        var sourceUsers = inventoryService.loadStaffUserRefs(properties);
        StaffAuthWritePlan writePlan = staffAuthWritePlanner.buildPlan(
                sourceUsers,
                inventoryService.loadStaffAuthTargetState(jdbcTemplate, target),
                activeRoleNames);
        log.info("ClientHubAI staff/auth write dry-run: source_users={} would_create={} would_update_existing={} "
                        + "would_update_mapped={} blocked={} password_reset_required={} actionable_rows={}",
                writePlan.sourceUsers(),
                writePlan.wouldCreate(),
                writePlan.wouldUpdateExisting(),
                writePlan.wouldUpdateMapped(),
                writePlan.blocked(),
                writePlan.passwordResetRequired(),
                writePlan.actionableRows());

        if (writePlan.blocked() > 0 && execute && properties.isExecuteStaffAuth()) {
            throw new IllegalStateException("ClientHubAI staff/auth execute blocked by invalid source rows");
        }
        if (!execute || !properties.isExecuteStaffAuth()) {
                runClientPlanIfReady(readinessPlan, target, sourceUsers, execute && properties.isExecuteClients(), !execute);
                completedSuccessfully = true;
                return;
        }

        StaffAuthExecuteResult executeResult = staffAuthExecuteService.execute(sourceUsers, target);
        log.info("ClientHubAI staff/auth execute complete: source_users={} created={} updated={} mapped_reruns={} "
                        + "password_reset_required={}",
                executeResult.sourceUsers(),
                executeResult.created(),
                executeResult.updated(),
                executeResult.mapped(),
                executeResult.passwordResetRequired());
            runClientPlanIfReady(readinessPlan, target, sourceUsers, execute && properties.isExecuteClients(), true);
            completedSuccessfully = true;
        } catch (Exception ex) {
            runRecorder.markFailed(migrationRunId, ex);
            throw ex;
        } finally {
            try {
                if (completedSuccessfully) {
                    if (execute) {
                        runRecorder.linkMappingsToRun(migrationRunId, target);
                    }
                    runRecorder.markSucceeded(migrationRunId, target.resolved()
                            ? reconciliationService.buildReport(source, target)
                            : null);
                }
            } finally {
                applicationContext.close();
            }
        }
    }

    private void runReconciliationIfReady(SourceInventory source, TargetInventory target) {
        if (!target.resolved()) {
            log.warn("ClientHubAI reconciliation skipped because target organisation is not uniquely resolved");
            return;
        }
        ReconciliationReport report = reconciliationService.buildReport(source, target);
        log.info("ClientHubAI reconciliation summary: total_source_rows={} total_mapped_rows={} "
                        + "pending_sync_events={} failed_or_dead_sync_events={}",
                report.totalSourceRows(),
                report.totalMappedRows(),
                report.totalPendingSyncEvents(),
                report.totalFailedSyncEvents());
        for (var entity : report.entities()) {
            log.info("ClientHubAI reconciliation entity: entity={} source_table={} source_rows={} mapped_rows={} "
                            + "unmapped_estimate={} pending_sync={} running_sync={} failed_sync={} dead_letter_sync={}",
                    entity.entityName(),
                    entity.sourceTable(),
                    entity.sourceRows(),
                    entity.mappedRows(),
                    entity.unmappedEstimate(),
                    entity.pendingSyncEvents(),
                    entity.runningSyncEvents(),
                    entity.failedSyncEvents(),
                    entity.deadLetterSyncEvents());
        }
    }

    private void runDemoCleanupDryRunIfEnabled() {
        if (!properties.isDemoCleanupDryRunEnabled()) {
            return;
        }
        DemoCleanupPlan plan = demoCleanupPlannerService.buildPlan();
        log.warn("ClientHubAI demo cleanup dry-run only: seed_auth_identities={} seed_memberships={} "
                        + "total_tenant_rows={}. Actual cleanup remains approval-gated and is not implemented.",
                plan.seedAuthIdentities(),
                plan.seedMemberships(),
                plan.totalTenantRows());
        for (var tenant : plan.tenants()) {
            log.warn("ClientHubAI demo cleanup tenant: slug={} schema={} organisation_rows={} tenant_rows={}",
                    tenant.slug(),
                    tenant.schemaName(),
                    tenant.organisationRows(),
                    tenant.totalTenantRows());
            for (var table : tenant.tableCounts()) {
                if (table.rows() > 0) {
                    log.warn("ClientHubAI demo cleanup table: schema={} table={} rows={}",
                            tenant.schemaName(),
                            table.tableName(),
                            table.rows());
                }
            }
        }
    }

    private void runSyncScanIfEnabled(TargetInventory target) throws Exception {
        if (!properties.isSyncEnabled()) {
            return;
        }
        if (!target.resolved()) {
            log.warn("ClientHubAI sync scan skipped because target organisation is not uniquely resolved");
            return;
        }
        boolean enqueueEvents = !properties.isDryRun() && properties.isSyncEnqueueEvents();
        SyncScanResult result = syncScannerService.scan(properties, target, enqueueEvents);
        log.info("ClientHubAI sync scan complete: total_changed_rows={} events_enqueued={}",
                result.totalChangedRows(),
                result.enqueued());
        for (var entity : result.entities()) {
            log.info("ClientHubAI sync entity scan: entity={} source_table={} cursor_column={} previous_cursor={} "
                            + "changed_rows={} next_cursor={}",
                    entity.entityName(),
                    entity.sourceTable(),
                    entity.cursorColumn(),
                    entity.previousCursor(),
                    entity.changedRows(),
                    entity.nextCursor());
        }
        if (!properties.isDryRun() && properties.isSyncApplyEvents()) {
            SyncProcessResult processResult = syncEventProcessorService.process(properties, target);
            log.info("ClientHubAI sync event processing complete: claimed={} succeeded={} failed={} dead_lettered={}",
                    processResult.claimed(),
                    processResult.succeeded(),
                    processResult.failed(),
                    processResult.deadLettered());
        }
    }

    private void runClientPlanIfReady(
            ClientHubMigrationReadinessPlan readinessPlan,
            TargetInventory target,
            List<ClientHubSourceInventoryService.SourceStaffUserRef> sourceUsers,
            boolean executeClients,
            boolean projectSameRunUserMappings) throws Exception {
        if (!target.resolved()) {
            log.warn("ClientHubAI client dry-run skipped because target organisation is not uniquely resolved");
            return;
        }

        // Scoped-only execute paths do not need the heavy clients inventory load; jump straight to
        // the requested stage so a stalled Azure source read cannot block unrelated migrations.
        boolean executeServices = !properties.isDryRun() && properties.isExecuteServices();
        boolean executeDocuments = !properties.isDryRun() && properties.isExecuteDocuments();
        boolean executeSessions = !properties.isDryRun() && properties.isExecuteSessions();
        boolean executeRoomIntegrations = !properties.isDryRun() && properties.isExecuteRoomIntegrations();
        boolean executeTherapistScheduling = !properties.isDryRun() && properties.isExecuteTherapistScheduling();
        boolean executeBilling = !properties.isDryRun() && properties.isExecuteBilling();
        boolean executeTasks = !properties.isDryRun() && properties.isExecuteTasks();
        boolean executeNotifications = !properties.isDryRun() && properties.isExecuteNotifications();
        boolean executeClinicalExtras = !properties.isDryRun() && properties.isExecuteClinicalExtras();
        boolean executeAssessments = !properties.isDryRun() && properties.isExecuteAssessments();
        boolean executeTranscripts = !properties.isDryRun() && properties.isExecuteTranscripts();
        boolean executeDocumentBinaries = !properties.isDryRun() && properties.isExecuteDocumentBinaries();
        boolean transcriptsOnlyExecute = executeTranscripts && !executeClients && !executeDocuments
                && !executeSessions && !executeServices && !executeRoomIntegrations
                && !executeTherapistScheduling && !executeBilling && !executeTasks
                && !executeNotifications && !executeClinicalExtras && !executeAssessments
                && !executeDocumentBinaries;
        boolean documentBinariesOnlyExecute = executeDocumentBinaries && !executeClients && !executeDocuments
                && !executeSessions && !executeServices && !executeRoomIntegrations
                && !executeTherapistScheduling && !executeBilling && !executeTasks
                && !executeNotifications && !executeClinicalExtras && !executeAssessments
                && !executeTranscripts;
        boolean assessmentsOnlyExecute = executeAssessments && !executeClients && !executeDocuments
                && !executeSessions && !executeServices && !executeRoomIntegrations
                && !executeTherapistScheduling && !executeBilling && !executeTasks
                && !executeNotifications && !executeClinicalExtras
                && !executeTranscripts && !executeDocumentBinaries;
        boolean clinicalExtrasOnlyExecute = executeClinicalExtras && !executeClients && !executeDocuments
                && !executeSessions && !executeServices && !executeRoomIntegrations
                && !executeTherapistScheduling && !executeBilling && !executeTasks
                && !executeNotifications && !executeAssessments && !executeTranscripts && !executeDocumentBinaries;
        boolean therapistSchedulingOnlyExecute = executeTherapistScheduling && !executeClients
                && !executeDocuments && !executeSessions && !executeServices && !executeRoomIntegrations
                && !executeBilling && !executeTasks && !executeNotifications && !executeClinicalExtras
                && !executeAssessments && !executeTranscripts && !executeDocumentBinaries;
        boolean tasksOnlyExecute = executeTasks && !executeClients && !executeDocuments && !executeSessions
                && !executeServices && !executeRoomIntegrations && !executeTherapistScheduling
                && !executeBilling && !executeNotifications && !executeClinicalExtras && !executeAssessments && !executeTranscripts && !executeDocumentBinaries;
        boolean notificationsOnlyExecute = executeNotifications && !executeClients && !executeDocuments
                && !executeSessions && !executeServices && !executeRoomIntegrations
                && !executeTherapistScheduling && !executeBilling && !executeTasks && !executeClinicalExtras
                && !executeAssessments && !executeTranscripts && !executeDocumentBinaries;
        boolean servicesOnlyExecute = executeServices && !executeClients && !executeDocuments
                && !executeSessions && !executeRoomIntegrations && !executeTherapistScheduling
                && !executeBilling && !executeTasks && !executeNotifications && !executeClinicalExtras
                && !executeAssessments && !executeTranscripts && !executeDocumentBinaries;
        if (transcriptsOnlyExecute) {
            runTranscriptsPlanIfReady(readinessPlan, target, true);
            return;
        }
        if (documentBinariesOnlyExecute) {
            runDocumentBinariesPlanIfReady(readinessPlan, target, true);
            return;
        }
        if (assessmentsOnlyExecute) {
            runAssessmentsPlanIfReady(readinessPlan, target, true);
            return;
        }
        if (clinicalExtrasOnlyExecute) {
            runClinicalExtrasPlanIfReady(readinessPlan, target, true);
            return;
        }
        if (therapistSchedulingOnlyExecute) {
            runTherapistSchedulePlanIfReady(readinessPlan, target, true);
            return;
        }
        if (tasksOnlyExecute) {
            runTaskPlanIfReady(readinessPlan, target, true);
            return;
        }
        if (notificationsOnlyExecute) {
            runNotificationPlanIfReady(readinessPlan, target, true);
            return;
        }
        if (servicesOnlyExecute) {
            runServicePlanIfReady(readinessPlan, target, true, true);
            return;
        }

        if (readinessPlan.entities().stream().noneMatch(entity -> "clients".equals(entity.entityName()) && !entity.blocked())) {
            log.warn("ClientHubAI client dry-run skipped because clients readiness is blocked");
            return;
        }

        SourceClientInventory clientInventory = inventoryService.inspectClientSource(properties);
        List<SourceClientRef> sourceClients = inventoryService.loadClientRefs(properties);
        ClientTargetState existingTargetState = inventoryService.loadClientTargetState(jdbcTemplate, target);
        Set<String> availableUserMappings = new HashSet<>(existingTargetState.mappedUserIds());
        if (projectSameRunUserMappings) {
            for (var sourceUser : sourceUsers) {
                availableUserMappings.add(sourceUser.legacyUserId());
            }
        }
        ClientTargetState projectedTargetState = new ClientTargetState(
                existingTargetState.mappedClientIds(),
                availableUserMappings,
                existingTargetState.tenantClientRows());

        ClientMigrationPlan clientPlan = clientMigrationPlanner.buildPlan(
                clientInventory,
                sourceClients,
                projectedTargetState);
        log.info("ClientHubAI client dry-run: source_clients={} would_create={} would_update_mapped={} blocked={} "
                        + "email_contacts={} phone_contacts={} emergency_contacts={} addresses={} complete_insurance={} "
                        + "incomplete_insurance={} referrals={} employment_rows={}",
                clientPlan.sourceClients(),
                clientPlan.wouldCreate(),
                clientPlan.wouldUpdateMapped(),
                clientPlan.blockedRows(),
                clientPlan.emailContactRows(),
                clientPlan.phoneContactRows(),
                clientPlan.emergencyContactRows(),
                clientPlan.addressRows(),
                clientPlan.completeInsuranceRows(),
                clientPlan.incompleteInsuranceRows(),
                clientPlan.referralRows(),
                clientPlan.employmentRows());
        for (String warning : clientPlan.warnings()) {
            log.warn("ClientHubAI client warning: {}", warning);
        }
        for (String blocker : clientPlan.blockers()) {
            log.warn("ClientHubAI client blocker: {}", blocker);
        }
        if (executeClients && clientPlan.blocked()) {
            throw new IllegalStateException("ClientHubAI client execute blocked by client plan blockers");
        }

        if (executeClients) {
            ClientExecuteResult executeResult = clientExecuteService.execute(
                    inventoryService.loadClientRecords(properties),
                    target);
            log.info("ClientHubAI client execute complete: source_clients={} created={} updated={} contacts={} "
                            + "addresses={} insurance={} referrals={} employment={}",
                    executeResult.sourceClients(),
                    executeResult.created(),
                    executeResult.updated(),
                    executeResult.contactsUpserted(),
                    executeResult.addressesUpserted(),
                    executeResult.insuranceUpserted(),
                    executeResult.referralsUpserted(),
                    executeResult.employmentUpserted());
            return;
        }

        boolean roomsOnlyExecute = executeRoomIntegrations && !executeClients && !executeDocuments
                && !executeSessions && !executeServices && !executeTherapistScheduling
                && !executeBilling && !executeTasks && !executeNotifications && !executeClinicalExtras
                && !executeAssessments && !executeTranscripts && !executeDocumentBinaries;
        boolean sessionsOnlyExecute = executeSessions && !executeClients && !executeDocuments
                && !executeServices && !executeRoomIntegrations && !executeTherapistScheduling
                && !executeBilling && !executeTasks && !executeNotifications && !executeClinicalExtras
                && !executeAssessments && !executeTranscripts && !executeDocumentBinaries;
        boolean documentsAndBillingOnlyExecute = (executeDocuments || executeBilling) && !executeClients
                && !executeSessions && !executeServices && !executeRoomIntegrations
                && !executeTherapistScheduling && !executeTasks && !executeNotifications
                && !executeClinicalExtras && !executeAssessments && !executeTranscripts && !executeDocumentBinaries;

        if (roomsOnlyExecute) {
            // Rooms may already be mapped; load session mappings so Zoom integrations can run too.
            Set<String> existingSessionMappings = inventoryService
                    .loadRoomIntegrationTargetState(jdbcTemplate, target)
                    .mappedSessionIds();
            runRoomIntegrationPlanIfReady(
                    readinessPlan,
                    target,
                    existingSessionMappings,
                    true,
                    true,
                    !existingSessionMappings.isEmpty());
            return;
        }
        if (sessionsOnlyExecute) {
            Set<String> existingServiceMappings = inventoryService.loadServiceTargetState(jdbcTemplate, target)
                    .mappedServiceIds();
            runSessionPlanIfReady(
                    readinessPlan,
                    target,
                    sourceUsers,
                    sourceClients,
                    clientPlan,
                    existingServiceMappings,
                    false,
                    false,
                    true);
            return;
        }
        if (documentsAndBillingOnlyExecute) {
            if (executeDocuments) {
                runDocumentPlanIfReady(
                        readinessPlan,
                        target,
                        availableUserMappings,
                        existingTargetState.mappedClientIds(),
                        true,
                        true);
            }
            if (executeBilling) {
                Set<String> existingSessionMappings = inventoryService
                        .loadBillingTargetState(jdbcTemplate, target)
                        .mappedSessionIds();
                runBillingPlanIfReady(readinessPlan, target, existingSessionMappings, true);
            }
            return;
        }
        if (transcriptsOnlyExecute) {
            runTranscriptsPlanIfReady(readinessPlan, target, true);
            return;
        }
        if (documentBinariesOnlyExecute) {
            runDocumentBinariesPlanIfReady(readinessPlan, target, true);
            return;
        }
        if (clinicalExtrasOnlyExecute) {
            runClinicalExtrasPlanIfReady(readinessPlan, target, true);
            return;
        }
        if (assessmentsOnlyExecute) {
            runAssessmentsPlanIfReady(readinessPlan, target, true);
            return;
        }
        if (therapistSchedulingOnlyExecute) {
            runTherapistSchedulePlanIfReady(readinessPlan, target, true);
            return;
        }
        if (tasksOnlyExecute) {
            runTaskPlanIfReady(readinessPlan, target, true);
            return;
        }
        if (notificationsOnlyExecute) {
            runNotificationPlanIfReady(readinessPlan, target, true);
            return;
        }
        if (servicesOnlyExecute) {
            runServicePlanIfReady(readinessPlan, target, true, true);
            return;
        }

        Set<String> projectedDocumentClientMappings = new HashSet<>(existingTargetState.mappedClientIds());
        if (properties.isDryRun() && !clientPlan.blocked()) {
            for (SourceClientRef sourceClient : sourceClients) {
                projectedDocumentClientMappings.add(sourceClient.legacyClientPk());
            }
        }
        runDocumentPlanIfReady(
                readinessPlan,
                target,
                availableUserMappings,
                projectedDocumentClientMappings,
                executeDocuments,
                false);

        Set<String> projectedServiceMappings = runServicePlanIfReady(
                readinessPlan,
                target,
                executeServices,
                properties.isDryRun() || executeServices);
        if (!executeClients) {
            if (executeDocuments) {
                runDocumentPlanIfReady(
                        readinessPlan,
                        target,
                        availableUserMappings,
                        projectedDocumentClientMappings,
                        false,
                        true);
            }
            runSessionPlanIfReady(
                    readinessPlan,
                    target,
                    sourceUsers,
                    sourceClients,
                    clientPlan,
                    projectedServiceMappings,
                    projectSameRunUserMappings,
                    properties.isDryRun(),
                    executeSessions);
            return;
        }
    }

    private void runDocumentPlanIfReady(
            ClientHubMigrationReadinessPlan readinessPlan,
            TargetInventory target,
            Set<String> projectedUserMappings,
            Set<String> projectedClientMappings,
            boolean failOnBlockers,
            boolean executeDocuments) throws Exception {
        if (readinessPlan.entities().stream().noneMatch(entity -> "documents".equals(entity.entityName()) && !entity.blocked())) {
            log.warn("ClientHubAI document dry-run skipped because documents readiness is blocked");
            return;
        }
        if (!target.resolved()) {
            log.warn("ClientHubAI document dry-run skipped because target organisation is not uniquely resolved");
            return;
        }

        SourceDocumentInventory documentInventory = inventoryService.inspectDocumentSource(properties);
        List<SourceDocumentRef> sourceDocuments = inventoryService.loadDocumentRefs(properties);
        DocumentTargetState existingTargetState = inventoryService.loadDocumentTargetState(jdbcTemplate, target);
        DocumentTargetState projectedTargetState = new DocumentTargetState(
                projectedClientMappings,
                projectedUserMappings,
                existingTargetState.mappedDocumentIds());
        DocumentMigrationPlan documentPlan = documentMigrationPlanner.buildPlan(
                documentInventory,
                sourceDocuments,
                projectedTargetState);

        log.info("ClientHubAI document dry-run: source_documents={} would_create={} would_update_mapped={} "
                        + "blocked={} uploaded_by_rows={} reviewed_by_rows={} shared_portal_rows={}",
                documentPlan.sourceDocuments(),
                documentPlan.wouldCreate(),
                documentPlan.wouldUpdateMapped(),
                documentPlan.blockedRows(),
                documentPlan.uploadedByRows(),
                documentPlan.reviewedByRows(),
                documentPlan.sharedInPortalRows());
        for (String blocker : documentPlan.blockers()) {
            log.warn("ClientHubAI document blocker: {}", blocker);
        }
        for (String warning : documentPlan.warnings()) {
            log.warn("ClientHubAI document warning: {}", warning);
        }
        if (failOnBlockers && documentPlan.blocked()) {
            throw new IllegalStateException("ClientHubAI document execute blocked by document plan blockers");
        }
        if (!executeDocuments) {
            return;
        }

        DocumentExecuteResult executeResult = documentExecuteService.execute(
                inventoryService.loadDocumentRecords(properties),
                target);
        log.info("ClientHubAI document execute complete: source_documents={} created={} updated={} mapped_reruns={}",
                executeResult.sourceDocuments(),
                executeResult.created(),
                executeResult.updated(),
                executeResult.mappedReruns());
    }

    private void runSessionPlanIfReady(
            ClientHubMigrationReadinessPlan readinessPlan,
            TargetInventory target,
            List<ClientHubSourceInventoryService.SourceStaffUserRef> sourceUsers,
            List<SourceClientRef> sourceClients,
            ClientMigrationPlan clientPlan,
            Set<String> projectedServiceMappings,
            boolean projectSameRunUserMappings,
            boolean projectSameRunClientMappings,
            boolean executeSessions) throws Exception {
        boolean sessionsReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "sessions".equals(entity.entityName()) && !entity.blocked());
        boolean notesReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "session_notes".equals(entity.entityName()) && !entity.blocked());
        if (!sessionsReady || !notesReady) {
            log.warn("ClientHubAI session dry-run skipped because sessions/session_notes readiness is blocked");
            return;
        }
        if (!target.resolved()) {
            log.warn("ClientHubAI session dry-run skipped because target organisation is not uniquely resolved");
            return;
        }

        SourceSessionInventory sessionInventory = inventoryService.inspectSessionSource(properties);
        List<SourceSessionRef> sourceSessions = inventoryService.loadSessionRefs(properties);
        List<SourceSessionNoteRef> sourceNotes = inventoryService.loadSessionNoteRefs(properties);
        SessionTargetState existingTargetState = inventoryService.loadSessionTargetState(jdbcTemplate, target);

        Set<String> projectedUserMappings = new HashSet<>(existingTargetState.mappedUserIds());
        if (projectSameRunUserMappings) {
            for (var sourceUser : sourceUsers) {
                projectedUserMappings.add(sourceUser.legacyUserId());
            }
        }

        Set<String> projectedClientMappings = new HashSet<>(existingTargetState.mappedClientIds());
        if (projectSameRunClientMappings && !clientPlan.blocked()) {
            for (SourceClientRef sourceClient : sourceClients) {
                projectedClientMappings.add(sourceClient.legacyClientPk());
            }
        }

        SessionTargetState projectedTargetState = new SessionTargetState(
                projectedClientMappings,
                projectedUserMappings,
                projectedServiceMappings,
                existingTargetState.mappedSessionIds(),
                existingTargetState.mappedSessionNoteIds());
        SessionMigrationPlan sessionPlan = sessionMigrationPlanner.buildPlan(
                sessionInventory,
                sourceSessions,
                sourceNotes,
                projectedTargetState);
        log.info("ClientHubAI session dry-run: source_sessions={} sessions_would_create={} "
                        + "sessions_would_update_mapped={} sessions_blocked={} source_session_notes={} "
                        + "notes_would_create={} notes_would_update_mapped={} notes_blocked={}",
                sessionPlan.sourceSessions(),
                sessionPlan.sessionsWouldCreate(),
                sessionPlan.sessionsWouldUpdateMapped(),
                sessionPlan.sessionsBlocked(),
                sessionPlan.sourceSessionNotes(),
                sessionPlan.notesWouldCreate(),
                sessionPlan.notesWouldUpdateMapped(),
                sessionPlan.notesBlocked());
        for (String warning : sessionPlan.warnings()) {
            log.warn("ClientHubAI session warning: {}", warning);
        }
        for (String blocker : sessionPlan.blockers()) {
            log.warn("ClientHubAI session blocker: {}", blocker);
        }
        Set<String> projectedSessionMappings = new HashSet<>(existingTargetState.mappedSessionIds());
        if ((properties.isDryRun() || executeSessions) && !sessionPlan.blocked()) {
            for (SourceSessionRef sourceSession : sourceSessions) {
                projectedSessionMappings.add(sourceSession.legacySessionPk());
            }
        }
        boolean executeRoomIntegrations = !properties.isDryRun() && properties.isExecuteRoomIntegrations();
        runRoomIntegrationPlanIfReady(
                readinessPlan,
                target,
                projectedSessionMappings,
                executeRoomIntegrations,
                false,
                false);
        boolean executeTherapistScheduling = !properties.isDryRun() && properties.isExecuteTherapistScheduling();
        runTherapistSchedulePlanIfReady(readinessPlan, target, false);
        if (executeSessions && sessionPlan.blocked()) {
            throw new IllegalStateException("ClientHubAI session execute blocked by session plan blockers");
        }
        if (!executeSessions) {
            if (executeRoomIntegrations) {
                runRoomIntegrationPlanIfReady(
                        readinessPlan,
                        target,
                        projectedSessionMappings,
                        false,
                        true,
                        true);
            }
            if (executeTherapistScheduling) {
                runTherapistSchedulePlanIfReady(readinessPlan, target, true);
            }
            runBillingPlanIfReady(
                    readinessPlan,
                    target,
                    projectedSessionMappings,
                    !properties.isDryRun() && properties.isExecuteBilling());
            runTaskPlanIfReady(
                    readinessPlan,
                    target,
                    !properties.isDryRun() && properties.isExecuteTasks());
            runNotificationPlanIfReady(
                    readinessPlan,
                    target,
                    !properties.isDryRun() && properties.isExecuteNotifications());
            runClinicalExtrasPlanIfReady(
                    readinessPlan,
                    target,
                    !properties.isDryRun() && properties.isExecuteClinicalExtras());
            runAssessmentsPlanIfReady(
                    readinessPlan,
                    target,
                    !properties.isDryRun() && properties.isExecuteAssessments());
            runTranscriptsPlanIfReady(
                    readinessPlan,
                    target,
                    !properties.isDryRun() && properties.isExecuteTranscripts());
            runDocumentBinariesPlanIfReady(
                    readinessPlan,
                    target,
                    !properties.isDryRun() && properties.isExecuteDocumentBinaries());
            return;
        }

        if (executeRoomIntegrations) {
            runRoomIntegrationPlanIfReady(
                    readinessPlan,
                    target,
                    projectedSessionMappings,
                    false,
                    true,
                    false);
        }
        SessionExecuteResult executeResult = sessionExecuteService.execute(
                inventoryService.loadSessionRecords(properties),
                inventoryService.loadSessionNoteRecords(properties),
                target);
        log.info("ClientHubAI session execute complete: source_sessions={} sessions_created={} "
                        + "sessions_updated={} session_mapped_reruns={} source_notes={} notes_created={} "
                        + "notes_updated={} note_mapped_reruns={}",
                executeResult.sourceSessions(),
                executeResult.sessionsCreated(),
                executeResult.sessionsUpdated(),
                executeResult.sessionMappedReruns(),
                executeResult.sourceNotes(),
                executeResult.notesCreated(),
                executeResult.notesUpdated(),
                executeResult.noteMappedReruns());
        if (executeRoomIntegrations) {
            runRoomIntegrationPlanIfReady(
                    readinessPlan,
                    target,
                    projectedSessionMappings,
                    false,
                    false,
                    true);
        }
        if (executeTherapistScheduling) {
            runTherapistSchedulePlanIfReady(readinessPlan, target, true);
        }
        runBillingPlanIfReady(
                readinessPlan,
                target,
                projectedSessionMappings,
                !properties.isDryRun() && properties.isExecuteBilling());
        runTaskPlanIfReady(
                readinessPlan,
                target,
                !properties.isDryRun() && properties.isExecuteTasks());
        runNotificationPlanIfReady(
                readinessPlan,
                target,
                !properties.isDryRun() && properties.isExecuteNotifications());
        runClinicalExtrasPlanIfReady(
                readinessPlan,
                target,
                !properties.isDryRun() && properties.isExecuteClinicalExtras());
        runAssessmentsPlanIfReady(
                readinessPlan,
                target,
                !properties.isDryRun() && properties.isExecuteAssessments());
        runTranscriptsPlanIfReady(
                readinessPlan,
                target,
                !properties.isDryRun() && properties.isExecuteTranscripts());
        runDocumentBinariesPlanIfReady(
                readinessPlan,
                target,
                !properties.isDryRun() && properties.isExecuteDocumentBinaries());
    }

    private void runTherapistSchedulePlanIfReady(
            ClientHubMigrationReadinessPlan readinessPlan,
            TargetInventory target,
            boolean executeTherapistScheduling) throws Exception {
        boolean profilesReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "user_profiles".equals(entity.entityName()) && !entity.blocked());
        boolean blockedReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "therapist_blocked_times".equals(entity.entityName()) && !entity.blocked());
        if (!profilesReady || !blockedReady) {
            log.warn("ClientHubAI therapist schedule dry-run skipped because user_profiles/"
                    + "therapist_blocked_times readiness is blocked");
            return;
        }
        if (!target.resolved()) {
            log.warn("ClientHubAI therapist schedule dry-run skipped because target organisation is not uniquely resolved");
            return;
        }

        SourceTherapistScheduleInventory inventory = inventoryService.inspectTherapistScheduleSource(properties);
        List<SourceUserProfileScheduleRecord> profiles = inventoryService.loadUserProfileScheduleRecords(properties);
        List<SourceTherapistBlockedTimeRecord> blockedTimes =
                inventoryService.loadTherapistBlockedTimeRecords(properties);
        TherapistScheduleTargetState targetState =
                inventoryService.loadTherapistScheduleTargetState(jdbcTemplate, target);
        TherapistScheduleMigrationPlan plan = therapistScheduleMigrationPlanner.buildPlan(
                inventory, profiles, blockedTimes, targetState);

        log.info("ClientHubAI therapist schedule dry-run: source_profiles={} profiles_would_create={} "
                        + "profiles_would_update_existing_user={} profiles_would_update_mapped={} profiles_blocked={} "
                        + "profiles_with_working_hours={} profiles_with_working_days_fallback={} "
                        + "profiles_invalid_working_hours={} profiles_with_license={} "
                        + "profiles_with_specializations={} profiles_with_emergency_contact={} "
                        + "source_blocked_times={} blocked_would_create={} "
                        + "blocked_would_update_mapped={} blocked_blocked={}",
                plan.sourceProfiles(),
                plan.profilesWouldCreate(),
                plan.profilesWouldUpdateExistingUser(),
                plan.profilesWouldUpdateMapped(),
                plan.profilesBlocked(),
                plan.profilesWithWorkingHours(),
                plan.profilesWithWorkingDaysFallback(),
                plan.profilesWithInvalidWorkingHours(),
                plan.profilesWithLicense(),
                plan.profilesWithSpecializations(),
                plan.profilesWithEmergencyContact(),
                plan.sourceBlockedTimes(),
                plan.blockedWouldCreate(),
                plan.blockedWouldUpdateMapped(),
                plan.blockedBlocked());
        for (String warning : plan.warnings()) {
            log.warn("ClientHubAI therapist schedule warning: {}", warning);
        }
        for (String blocker : plan.blockers()) {
            log.warn("ClientHubAI therapist schedule blocker: {}", blocker);
        }
        if (executeTherapistScheduling && plan.blocked()) {
            throw new IllegalStateException("ClientHubAI therapist schedule execute blocked by plan blockers");
        }
        if (!executeTherapistScheduling) {
            return;
        }

        TherapistScheduleExecuteResult result = therapistScheduleExecuteService.execute(
                profiles, blockedTimes, target);
        log.info("ClientHubAI therapist schedule execute complete: source_profiles={} profiles_created={} "
                        + "profiles_updated={} profile_mapped_reruns={} working_hours_upserted={} "
                        + "professional_children_upserted={} emergency_contacts_upserted={} "
                        + "physical_rooms_linked={} physical_rooms_skipped={} virtual_rooms_skipped={} "
                        + "source_blocked_times={} blocked_created={} blocked_updated={} blocked_mapped_reruns={}",
                result.sourceProfiles(),
                result.profilesCreated(),
                result.profilesUpdated(),
                result.profileMappedReruns(),
                result.workingHoursUpserted(),
                result.professionalChildrenUpserted(),
                result.emergencyContactsUpserted(),
                result.physicalRoomsLinked(),
                result.physicalRoomsSkipped(),
                result.virtualRoomsSkipped(),
                result.sourceBlockedTimes(),
                result.blockedCreated(),
                result.blockedUpdated(),
                result.blockedMappedReruns());
    }

    private void runTaskPlanIfReady(
            ClientHubMigrationReadinessPlan readinessPlan,
            TargetInventory target,
            boolean executeTasks) throws Exception {
        boolean tasksReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "tasks".equals(entity.entityName()) && !entity.blocked());
        boolean commentsReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "task_comments".equals(entity.entityName()) && !entity.blocked());
        if (!tasksReady || !commentsReady) {
            log.warn("ClientHubAI task dry-run skipped because tasks or task_comments readiness is blocked");
            return;
        }
        if (!target.resolved()) {
            log.warn("ClientHubAI task dry-run skipped because target organisation is not uniquely resolved");
            return;
        }

        SourceTaskInventory inventory = inventoryService.inspectTaskSource(properties);
        List<SourceTaskRecord> sourceTasks = inventoryService.loadTaskRecords(properties);
        List<SourceTaskCommentRecord> sourceComments = inventoryService.loadTaskCommentRecords(properties);
        TaskTargetState existingTargetState = inventoryService.loadTaskTargetState(jdbcTemplate, target);
        TaskMigrationPlan plan = taskMigrationPlanner.buildPlan(
                inventory,
                sourceTasks,
                sourceComments,
                existingTargetState);

        log.info("ClientHubAI task dry-run: source_tasks={} tasks_would_create={} tasks_would_update_mapped={} "
                        + "tasks_blocked={} source_comments={} comments_would_create={} "
                        + "comments_would_update_mapped={} comments_blocked={}",
                plan.sourceTasks(),
                plan.tasksWouldCreate(),
                plan.tasksWouldUpdateMapped(),
                plan.tasksBlocked(),
                plan.sourceComments(),
                plan.commentsWouldCreate(),
                plan.commentsWouldUpdateMapped(),
                plan.commentsBlocked());
        for (String blocker : plan.blockers()) {
            log.warn("ClientHubAI task blocker: {}", blocker);
        }
        if (executeTasks && plan.blocked()) {
            throw new IllegalStateException("ClientHubAI task execute blocked by task plan blockers");
        }
        if (!executeTasks) {
            return;
        }

        TaskExecuteResult result = taskExecuteService.execute(sourceTasks, sourceComments, target);
        log.info("ClientHubAI task execute complete: source_tasks={} tasks_created={} tasks_updated={} "
                        + "task_mapped_reruns={} source_comments={} comments_created={} "
                        + "comments_updated={} comment_mapped_reruns={}",
                result.sourceTasks(),
                result.tasksCreated(),
                result.tasksUpdated(),
                result.taskMappedReruns(),
                result.sourceComments(),
                result.commentsCreated(),
                result.commentsUpdated(),
                result.commentMappedReruns());
    }

    private void runNotificationPlanIfReady(
            ClientHubMigrationReadinessPlan readinessPlan,
            TargetInventory target,
            boolean executeNotifications) throws Exception {
        boolean templatesReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "notification_templates".equals(entity.entityName()) && !entity.blocked());
        boolean triggersReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "notification_triggers".equals(entity.entityName()) && !entity.blocked());
        boolean preferencesReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "notification_preferences".equals(entity.entityName()) && !entity.blocked());
        boolean notificationsReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "notifications".equals(entity.entityName()) && !entity.blocked());
        boolean scheduledReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "scheduled_notifications".equals(entity.entityName()) && !entity.blocked());
        if (!templatesReady || !triggersReady || !preferencesReady || !notificationsReady || !scheduledReady) {
            log.warn("ClientHubAI notification dry-run skipped because one or more notification tables are blocked");
            return;
        }
        if (!target.resolved()) {
            log.warn("ClientHubAI notification dry-run skipped because target organisation is not uniquely resolved");
            return;
        }

        SourceNotificationInventory inventory = inventoryService.inspectNotificationSource(properties);
        List<SourceNotificationTemplateRecord> templates = inventoryService.loadNotificationTemplateRecords(properties);
        List<SourceNotificationTriggerRecord> triggers = inventoryService.loadNotificationTriggerRecords(properties);
        List<SourceNotificationPreferenceRecord> preferences =
                inventoryService.loadNotificationPreferenceRecords(properties);
        List<SourceNotificationRecord> notifications = inventoryService.loadNotificationRecords(properties);
        List<SourceScheduledNotificationRecord> scheduled =
                inventoryService.loadScheduledNotificationRecords(properties);
        NotificationTargetState targetState = inventoryService.loadNotificationTargetState(jdbcTemplate, target);
        NotificationMigrationPlan plan = notificationMigrationPlanner.buildPlan(
                inventory, templates, triggers, preferences, notifications, scheduled, targetState);

        log.info("ClientHubAI notification dry-run: lookback_months={} templates_create={} templates_update_mapped={} "
                        + "templates_update_name={} templates_blocked={} triggers_create={} "
                        + "triggers_update_mapped={} triggers_update_name={} triggers_blocked={} "
                        + "preferences_create={} preferences_update_mapped={} preferences_blocked={} "
                        + "preferences_skipped={} notifications_create={} notifications_update_mapped={} "
                        + "notifications_blocked={} scheduled_create={} scheduled_update_mapped={} "
                        + "scheduled_blocked={} scheduled_skipped={}",
                properties.getNotificationLookbackMonths(),
                plan.templatesWouldCreate(),
                plan.templatesWouldUpdateMapped(),
                plan.templatesWouldUpdateExistingName(),
                plan.templatesBlocked(),
                plan.triggersWouldCreate(),
                plan.triggersWouldUpdateMapped(),
                plan.triggersWouldUpdateExistingName(),
                plan.triggersBlocked(),
                plan.preferencesWouldCreate(),
                plan.preferencesWouldUpdateMapped(),
                plan.preferencesBlocked(),
                plan.preferencesSkipped(),
                plan.notificationsWouldCreate(),
                plan.notificationsWouldUpdateMapped(),
                plan.notificationsBlocked(),
                plan.scheduledWouldCreate(),
                plan.scheduledWouldUpdateMapped(),
                plan.scheduledBlocked(),
                plan.scheduledSkipped());
        for (String blocker : plan.blockers()) {
            log.warn("ClientHubAI notification blocker: {}", blocker);
        }
        for (String warning : plan.warnings()) {
            log.warn("ClientHubAI notification warning: {}", warning);
        }
        if (executeNotifications && plan.blocked()) {
            throw new IllegalStateException("ClientHubAI notification execute blocked by notification plan blockers");
        }
        if (!executeNotifications) {
            return;
        }

        NotificationExecuteResult result = notificationExecuteService.execute(
                templates, triggers, preferences, notifications, scheduled, target);
        log.info("ClientHubAI notification execute complete: templates_created={} templates_updated={} "
                        + "triggers_created={} triggers_updated={} preferences_created={} preferences_updated={} "
                        + "preferences_skipped={} notifications_created={} notifications_updated={} "
                        + "scheduled_created={} scheduled_updated={} scheduled_skipped={}",
                result.templatesCreated(),
                result.templatesUpdated(),
                result.triggersCreated(),
                result.triggersUpdated(),
                result.preferencesCreated(),
                result.preferencesUpdated(),
                result.preferencesSkipped(),
                result.notificationsCreated(),
                result.notificationsUpdated(),
                result.scheduledCreated(),
                result.scheduledUpdated(),
                result.scheduledSkipped());
    }

    private void runClinicalExtrasPlanIfReady(
            ClientHubMigrationReadinessPlan readinessPlan,
            TargetInventory target,
            boolean executeClinicalExtras) throws Exception {
        boolean consentsReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "patient_consents".equals(entity.entityName()) && !entity.blocked());
        boolean supervisorsReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "supervisor_assignments".equals(entity.entityName()) && !entity.blocked());
        boolean portalReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "client_portal".equals(entity.entityName()) && !entity.blocked());
        boolean templatesReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "checklist_templates".equals(entity.entityName()) && !entity.blocked());
        boolean itemsReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "checklist_items".equals(entity.entityName()) && !entity.blocked());
        boolean clientChecklistsReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "client_checklists".equals(entity.entityName()) && !entity.blocked());
        boolean clientChecklistItemsReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "client_checklist_items".equals(entity.entityName()) && !entity.blocked());
        boolean zoomReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "user_integrations_zoom".equals(entity.entityName()) && !entity.blocked());
        if (!consentsReady || !supervisorsReady || !portalReady || !templatesReady || !itemsReady
                || !clientChecklistsReady || !clientChecklistItemsReady || !zoomReady) {
            log.warn("ClientHubAI clinical extras dry-run skipped because one or more clinical extras "
                    + "tables are blocked");
            return;
        }
        if (!target.resolved()) {
            log.warn("ClientHubAI clinical extras dry-run skipped because target organisation is not uniquely resolved");
            return;
        }

        SourceClinicalExtrasInventory inventory = inventoryService.inspectClinicalExtrasSource(properties);
        List<SourcePatientConsentRecord> consents = inventoryService.loadPatientConsentRecords(properties);
        List<SourceSupervisorAssignmentRecord> supervisors =
                inventoryService.loadSupervisorAssignmentRecords(properties);
        List<SourceClientPortalRecord> portals = inventoryService.loadClientPortalRecords(properties);
        List<SourceChecklistTemplateRecord> templates = inventoryService.loadChecklistTemplateRecords(properties);
        List<SourceChecklistItemRecord> checklistItems = inventoryService.loadChecklistItemRecords(properties);
        List<SourceClientChecklistRecord> clientChecklists =
                inventoryService.loadClientChecklistRecords(properties);
        List<SourceClientChecklistItemRecord> clientChecklistItems =
                inventoryService.loadClientChecklistItemRecords(properties);
        List<SourceUserZoomIntegrationRecord> zoomIntegrations =
                inventoryService.loadUserZoomIntegrationRecords(properties);
        ClinicalExtrasTargetState targetState = inventoryService.loadClinicalExtrasTargetState(jdbcTemplate, target);
        ClinicalExtrasMigrationPlan plan = clinicalExtrasMigrationPlanner.buildPlan(
                inventory, consents, supervisors, portals, templates, checklistItems,
                clientChecklists, clientChecklistItems, zoomIntegrations, targetState);

        log.info("ClientHubAI clinical extras dry-run: consents_create={} consents_update_mapped={} "
                        + "consents_blocked={} supervisors_create={} supervisors_update_mapped={} "
                        + "supervisors_blocked={} portals_create={} portals_update_mapped={} portals_blocked={} "
                        + "portal_password_reset_required={} templates_create={} templates_update_mapped={} "
                        + "templates_update_name={} checklist_items_create={} client_checklists_create={} "
                        + "client_checklist_items_create={} zoom_create={} zoom_update_mapped={} zoom_blocked={}",
                plan.consentsWouldCreate(),
                plan.consentsWouldUpdateMapped(),
                plan.consentsBlocked(),
                plan.supervisorsWouldCreate(),
                plan.supervisorsWouldUpdateMapped(),
                plan.supervisorsBlocked(),
                plan.portalsWouldCreate(),
                plan.portalsWouldUpdateMapped(),
                plan.portalsBlocked(),
                plan.portalsPasswordResetRequired(),
                plan.templatesWouldCreate(),
                plan.templatesWouldUpdateMapped(),
                plan.templatesWouldUpdateExistingName(),
                plan.checklistItemsWouldCreate(),
                plan.clientChecklistsWouldCreate(),
                plan.clientChecklistItemsWouldCreate(),
                plan.zoomWouldCreate(),
                plan.zoomWouldUpdateMapped(),
                plan.zoomBlocked());
        for (String blocker : plan.blockers()) {
            log.warn("ClientHubAI clinical extras blocker: {}", blocker);
        }
        for (String warning : plan.warnings()) {
            log.warn("ClientHubAI clinical extras warning: {}", warning);
        }
        if (executeClinicalExtras && plan.blocked()) {
            throw new IllegalStateException("ClientHubAI clinical extras execute blocked by plan blockers");
        }
        if (!executeClinicalExtras) {
            return;
        }

        ClinicalExtrasExecuteResult result = clinicalExtrasExecuteService.execute(
                consents, supervisors, portals, templates, checklistItems,
                clientChecklists, clientChecklistItems, zoomIntegrations, target);
        log.info("ClientHubAI clinical extras execute complete: consents_created={} consents_updated={} "
                        + "supervisors_created={} supervisors_updated={} portals_created={} portals_updated={} "
                        + "portal_password_reset_required={} templates_created={} checklist_items_created={} "
                        + "client_checklists_created={} client_checklist_items_created={} "
                        + "zoom_created={} zoom_updated={}",
                result.consentsCreated(),
                result.consentsUpdated(),
                result.supervisorsCreated(),
                result.supervisorsUpdated(),
                result.portalsCreated(),
                result.portalsUpdated(),
                result.portalPasswordResetRequired(),
                result.templatesCreated(),
                result.checklistItemsCreated(),
                result.clientChecklistsCreated(),
                result.clientChecklistItemsCreated(),
                result.zoomCreated(),
                result.zoomUpdated());
    }

    private void runAssessmentsPlanIfReady(
            ClientHubMigrationReadinessPlan readinessPlan,
            TargetInventory target,
            boolean executeAssessments) throws Exception {
        boolean templatesReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "assessment_templates".equals(entity.entityName()) && !entity.blocked());
        boolean sectionsReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "assessment_sections".equals(entity.entityName()) && !entity.blocked());
        boolean questionsReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "assessment_questions".equals(entity.entityName()) && !entity.blocked());
        boolean optionsReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "assessment_question_options".equals(entity.entityName()) && !entity.blocked());
        boolean assignmentsReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "assessment_assignments".equals(entity.entityName()) && !entity.blocked());
        boolean responsesReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "assessment_responses".equals(entity.entityName()) && !entity.blocked());
        boolean reportsReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "assessment_reports".equals(entity.entityName()) && !entity.blocked());
        if (!templatesReady || !sectionsReady || !questionsReady || !optionsReady
                || !assignmentsReady || !responsesReady || !reportsReady) {
            log.warn("ClientHubAI assessments dry-run skipped because one or more assessment tables are blocked");
            return;
        }
        if (!target.resolved()) {
            log.warn("ClientHubAI assessments dry-run skipped because target organisation is not uniquely resolved");
            return;
        }

        SourceAssessmentsInventory inventory = inventoryService.inspectAssessmentsSource(properties);
        List<SourceAssessmentTemplateRecord> templates =
                inventoryService.loadAssessmentTemplateRecords(properties);
        List<SourceAssessmentSectionRecord> sections =
                inventoryService.loadAssessmentSectionRecords(properties);
        List<SourceAssessmentQuestionRecord> questions =
                inventoryService.loadAssessmentQuestionRecords(properties);
        List<SourceAssessmentQuestionOptionRecord> options =
                inventoryService.loadAssessmentQuestionOptionRecords(properties);
        List<SourceAssessmentAssignmentRecord> assignments =
                inventoryService.loadAssessmentAssignmentRecords(properties);
        List<SourceAssessmentResponseRecord> responses =
                inventoryService.loadAssessmentResponseRecords(properties);
        List<SourceAssessmentReportRecord> reports =
                inventoryService.loadAssessmentReportRecords(properties);
        AssessmentTargetState targetState = inventoryService.loadAssessmentsTargetState(jdbcTemplate, target);
        AssessmentsMigrationPlan plan = assessmentsMigrationPlanner.buildPlan(
                inventory, templates, sections, questions, options, assignments, responses, reports, targetState);

        log.info("ClientHubAI assessments dry-run: templates_create={} templates_update_mapped={} "
                        + "templates_blocked={} sections_create={} questions_create={} options_create={} "
                        + "assignments_create={} assignments_blocked={} responses_create={} "
                        + "responses_responder_fallback={} reports_create={} reports_blocked={}",
                plan.templatesWouldCreate(),
                plan.templatesWouldUpdateMapped(),
                plan.templatesBlocked(),
                plan.sectionsWouldCreate(),
                plan.questionsWouldCreate(),
                plan.optionsWouldCreate(),
                plan.assignmentsWouldCreate(),
                plan.assignmentsBlocked(),
                plan.responsesWouldCreate(),
                plan.responsesResponderFallbackOrSkip(),
                plan.reportsWouldCreate(),
                plan.reportsBlocked());
        for (String blocker : plan.blockers()) {
            log.warn("ClientHubAI assessments blocker: {}", blocker);
        }
        for (String warning : plan.warnings()) {
            log.warn("ClientHubAI assessments warning: {}", warning);
        }
        if (executeAssessments && plan.blocked()) {
            throw new IllegalStateException("ClientHubAI assessments execute blocked by plan blockers");
        }
        if (!executeAssessments) {
            return;
        }

        AssessmentsExecuteResult result = assessmentsExecuteService.execute(
                templates, sections, questions, options, assignments, responses, reports, target);
        log.info("ClientHubAI assessments execute complete: templates_created={} sections_created={} "
                        + "questions_created={} options_created={} assignments_created={} "
                        + "responses_created={} responses_skipped={} reports_created={}",
                result.templatesCreated(),
                result.sectionsCreated(),
                result.questionsCreated(),
                result.optionsCreated(),
                result.assignmentsCreated(),
                result.responsesCreated(),
                result.responsesSkipped(),
                result.reportsCreated());
    }

    private void runTranscriptsPlanIfReady(
            ClientHubMigrationReadinessPlan readinessPlan,
            TargetInventory target,
            boolean executeTranscripts) throws Exception {
        boolean transcriptsReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "session_transcripts".equals(entity.entityName()) && !entity.blocked());
        if (!transcriptsReady) {
            log.warn("ClientHubAI transcripts dry-run skipped because session_transcripts readiness is blocked");
            return;
        }
        if (!target.resolved()) {
            log.warn("ClientHubAI transcripts dry-run skipped because target organisation is not uniquely resolved");
            return;
        }

        SourceTranscriptsInventory inventory = inventoryService.inspectTranscriptsSource(properties);
        List<SourceTranscriptRecord> transcripts = inventoryService.loadTranscriptRecords(properties);
        TranscriptsTargetState targetState = inventoryService.loadTranscriptsTargetState(jdbcTemplate, target);
        TranscriptsMigrationPlan plan = transcriptsMigrationPlanner.buildPlan(inventory, transcripts, targetState);

        log.info("ClientHubAI transcripts dry-run: source_transcripts={} would_create={} would_update_mapped={} blocked={}",
                plan.sourceTranscripts(),
                plan.wouldCreate(),
                plan.wouldUpdateMapped(),
                plan.blockedRows());
        for (String blocker : plan.blockers()) {
            log.warn("ClientHubAI transcripts blocker: {}", blocker);
        }
        if (executeTranscripts && plan.blocked()) {
            throw new IllegalStateException("ClientHubAI transcripts execute blocked by plan blockers");
        }
        if (!executeTranscripts) {
            return;
        }

        TranscriptsExecuteResult result = transcriptsExecuteService.execute(transcripts, target);
        log.info("ClientHubAI transcripts execute complete: source_transcripts={} created={} updated={} mapped_reruns={}",
                result.sourceTranscripts(),
                result.created(),
                result.updated(),
                result.mappedReruns());
    }

    private void runDocumentBinariesPlanIfReady(
            ClientHubMigrationReadinessPlan readinessPlan,
            TargetInventory target,
            boolean executeDocumentBinaries) {
        boolean documentsReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "documents".equals(entity.entityName()) && !entity.blocked());
        if (!documentsReady) {
            log.warn("ClientHubAI document binaries dry-run skipped because documents readiness is blocked");
            return;
        }
        if (!target.resolved()) {
            log.warn("ClientHubAI document binaries dry-run skipped because target organisation is not uniquely resolved");
            return;
        }

        DocumentBinariesTargetState targetState =
                inventoryService.loadDocumentBinariesTargetState(jdbcTemplate, target);
        DocumentBinariesMigrationPlan plan = documentBinariesMigrationPlanner.buildPlan(targetState);

        log.info("ClientHubAI document binaries dry-run: mapped_documents={} would_copy={} already_clean={} blocked={}",
                plan.mappedDocuments(),
                plan.wouldCopy(),
                plan.alreadyClean(),
                plan.blockedRows());
        for (String blocker : plan.blockers()) {
            log.warn("ClientHubAI document binaries blocker: {}", blocker);
        }
        for (String warning : plan.warnings()) {
            log.warn("ClientHubAI document binaries warning: {}", warning);
        }
        if (executeDocumentBinaries && plan.blocked()) {
            throw new IllegalStateException("ClientHubAI document binaries execute blocked by plan blockers");
        }
        if (!executeDocumentBinaries) {
            return;
        }

        DocumentBinariesExecuteResult result =
                documentBinariesExecuteService.execute(targetState.mappedDocuments(), target);
        log.info("ClientHubAI document binaries execute complete: mapped_documents={} copied={} already_clean={} "
                        + "skipped_missing_blob={} skipped_blocked={} failed={}",
                result.mappedDocuments(),
                result.copied(),
                result.alreadyClean(),
                result.skippedMissingBlob(),
                result.skippedBlocked(),
                result.failed());
    }

    private void runRoomIntegrationPlanIfReady(
            ClientHubMigrationReadinessPlan readinessPlan,
            TargetInventory target,
            Set<String> projectedSessionMappings,
            boolean failOnBlockers,
            boolean executeRooms,
            boolean executeIntegrations) throws Exception {
        boolean roomsReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "rooms".equals(entity.entityName()) && !entity.blocked());
        boolean integrationsReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "session_integrations".equals(entity.entityName()) && !entity.blocked());
        if (!roomsReady || !integrationsReady) {
            log.warn("ClientHubAI room/Zoom dry-run skipped because rooms or session integration readiness is blocked");
            return;
        }
        if (!target.resolved()) {
            log.warn("ClientHubAI room/Zoom dry-run skipped because target organisation is not uniquely resolved");
            return;
        }

        SourceRoomIntegrationInventory inventory = inventoryService.inspectRoomIntegrationSource(properties);
        List<SourceRoomRecord> sourceRooms = inventoryService.loadRoomRecords(properties);
        // Skip loading Zoom integration rows when this run only executes rooms (sessions not mapped yet).
        List<SourceSessionIntegrationRecord> sourceIntegrations = executeIntegrations || (!executeRooms && !executeIntegrations)
                ? inventoryService.loadSessionIntegrationRecords(properties)
                : List.of();
        RoomIntegrationTargetState existingTargetState = inventoryService.loadRoomIntegrationTargetState(jdbcTemplate, target);
        RoomIntegrationTargetState projectedTargetState = new RoomIntegrationTargetState(
                existingTargetState.mappedRoomIds(),
                projectedSessionMappings,
                existingTargetState.mappedSessionIntegrationIds(),
                existingTargetState.existingRoomNumbers());
        RoomIntegrationMigrationPlan plan = roomIntegrationMigrationPlanner.buildPlan(
                inventory,
                sourceRooms,
                sourceIntegrations,
                projectedTargetState);

        log.info("ClientHubAI room/Zoom dry-run: source_rooms={} rooms_would_create={} "
                        + "rooms_would_update_existing_number={} rooms_would_update_mapped={} rooms_blocked={} "
                        + "sessions_with_room={} source_zoom_integrations={} integrations_would_create={} "
                        + "integrations_would_update_mapped={} integrations_blocked={} zoom_missing_meeting_id={}",
                plan.sourceRooms(),
                plan.roomsWouldCreate(),
                plan.roomsWouldUpdateExistingNumber(),
                plan.roomsWouldUpdateMapped(),
                plan.roomsBlocked(),
                plan.sessionsWithRoomRows(),
                plan.sourceZoomIntegrations(),
                plan.integrationsWouldCreate(),
                plan.integrationsWouldUpdateMapped(),
                plan.integrationsBlocked(),
                plan.zoomEnabledMissingMeetingRows());
        for (String blocker : plan.blockers()) {
            log.warn("ClientHubAI room/Zoom blocker: {}", blocker);
        }
        if (failOnBlockers && plan.blocked()) {
            throw new IllegalStateException("ClientHubAI room/Zoom execute blocked by room/Zoom plan blockers");
        }
        if (!executeRooms && !executeIntegrations) {
            return;
        }

        RoomIntegrationExecuteResult result = roomIntegrationExecuteService.execute(
                executeRooms ? sourceRooms : List.of(),
                executeIntegrations ? sourceIntegrations : List.of(),
                target);
        log.info("ClientHubAI room/Zoom execute complete: source_rooms={} rooms_created={} rooms_updated={} "
                        + "room_mapped_reruns={} source_integrations={} integrations_created={} "
                        + "integrations_updated={} integration_mapped_reruns={}",
                result.sourceRooms(),
                result.roomsCreated(),
                result.roomsUpdated(),
                result.roomMappedReruns(),
                result.sourceIntegrations(),
                result.integrationsCreated(),
                result.integrationsUpdated(),
                result.integrationMappedReruns());
    }

    private void runBillingPlanIfReady(
            ClientHubMigrationReadinessPlan readinessPlan,
            TargetInventory target,
            Set<String> projectedSessionMappings,
            boolean executeBilling) throws Exception {
        boolean billingReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "session_billing".equals(entity.entityName()) && !entity.blocked());
        boolean paymentsReady = readinessPlan.entities().stream()
                .anyMatch(entity -> "payment_transactions".equals(entity.entityName()) && !entity.blocked());
        if (!billingReady || !paymentsReady) {
            log.warn("ClientHubAI billing/payment dry-run skipped because billing/payment readiness is blocked");
            return;
        }
        if (!target.resolved()) {
            log.warn("ClientHubAI billing/payment dry-run skipped because target organisation is not uniquely resolved");
            return;
        }

        SourceBillingInventory billingInventory = inventoryService.inspectBillingSource(properties);
        List<SourceBillingRef> sourceBillings = inventoryService.loadBillingRefs(properties);
        List<SourcePaymentTransactionRef> sourcePayments = inventoryService.loadPaymentTransactionRefs(properties);
        BillingTargetState existingTargetState = inventoryService.loadBillingTargetState(jdbcTemplate, target);
        BillingTargetState projectedTargetState = new BillingTargetState(
                projectedSessionMappings,
                existingTargetState.mappedBillingIds(),
                existingTargetState.mappedPaymentTransactionIds());
        BillingMigrationPlan billingPlan = billingMigrationPlanner.buildPlan(
                billingInventory,
                sourceBillings,
                sourcePayments,
                projectedTargetState);

        log.info("ClientHubAI billing/payment dry-run: source_billings={} billings_would_create={} "
                        + "billings_would_update_mapped={} billings_blocked={} source_payment_transactions={} "
                        + "payments_would_create={} payments_would_update_mapped={} payments_blocked={}",
                billingPlan.sourceBillings(),
                billingPlan.billingsWouldCreate(),
                billingPlan.billingsWouldUpdateMapped(),
                billingPlan.billingsBlocked(),
                billingPlan.sourcePaymentTransactions(),
                billingPlan.paymentsWouldCreate(),
                billingPlan.paymentsWouldUpdateMapped(),
                billingPlan.paymentsBlocked());
        for (String blocker : billingPlan.blockers()) {
            log.warn("ClientHubAI billing/payment blocker: {}", blocker);
        }
        for (String warning : billingPlan.warnings()) {
            log.warn("ClientHubAI billing/payment warning: {}", warning);
        }
        if (executeBilling && billingPlan.blocked()) {
            throw new IllegalStateException("ClientHubAI billing/payment execute blocked by billing/payment plan blockers");
        }
        if (!executeBilling) {
            return;
        }

        BillingExecuteResult executeResult = billingExecuteService.execute(
                inventoryService.loadBillingRecords(
                        properties,
                        ClientHubSourceInventoryService.billingDateZone(target, properties)),
                inventoryService.loadPaymentTransactionRecords(properties),
                target);
        log.info("ClientHubAI billing/payment execute complete: source_billings={} billings_created={} "
                        + "billings_updated={} billing_mapped_reruns={} source_payment_transactions={} "
                        + "payments_created={} payments_updated={} transaction_mapped_reruns={}",
                executeResult.sourceBillings(),
                executeResult.billingsCreated(),
                executeResult.billingsUpdated(),
                executeResult.billingMappedReruns(),
                executeResult.sourcePaymentTransactions(),
                executeResult.paymentsCreated(),
                executeResult.paymentsUpdated(),
                executeResult.transactionMappedReruns());
    }

    private Set<String> runServicePlanIfReady(
            ClientHubMigrationReadinessPlan readinessPlan,
            TargetInventory target,
            boolean executeServices,
            boolean projectSameRunServiceMappings) throws Exception {
        if (readinessPlan.entities().stream().noneMatch(entity -> "services".equals(entity.entityName()) && !entity.blocked())) {
            log.warn("ClientHubAI service dry-run skipped because services readiness is blocked");
            return Set.of();
        }
        if (!target.resolved()) {
            log.warn("ClientHubAI service dry-run skipped because target organisation is not uniquely resolved");
            return Set.of();
        }

        SourceServiceInventory serviceInventory = inventoryService.inspectServiceSource(properties);
        List<SourceServiceRecord> sourceServices = inventoryService.loadServiceRecords(properties);
        ServiceTargetState targetState = inventoryService.loadServiceTargetState(jdbcTemplate, target);
        ServiceMigrationPlan servicePlan = serviceMigrationPlanner.buildPlan(serviceInventory, sourceServices, targetState);

        log.info("ClientHubAI service dry-run: source_services={} would_create={} would_update_existing_code={} "
                        + "would_update_mapped={} blocked={}",
                servicePlan.sourceServices(),
                servicePlan.wouldCreate(),
                servicePlan.wouldUpdateExistingCode(),
                servicePlan.wouldUpdateMapped(),
                servicePlan.blockedRows());
        for (String blocker : servicePlan.blockers()) {
            log.warn("ClientHubAI service blocker: {}", blocker);
        }
        for (String warning : servicePlan.warnings()) {
            log.warn("ClientHubAI service warning: {}", warning);
        }
        if (executeServices && servicePlan.blocked()) {
            throw new IllegalStateException("ClientHubAI service execute blocked by service plan blockers");
        }

        Set<String> projectedServiceMappings = new HashSet<>(targetState.mappedServiceIds());
        if (projectSameRunServiceMappings && !servicePlan.blocked()) {
            for (SourceServiceRecord sourceService : sourceServices) {
                projectedServiceMappings.add(sourceService.legacyServicePk());
            }
        }

        if (!executeServices) {
            return projectedServiceMappings;
        }

        ServiceExecuteResult executeResult = serviceExecuteService.execute(sourceServices, target);
        log.info("ClientHubAI service execute complete: source_services={} created={} updated={} mapped_reruns={}",
                executeResult.sourceServices(),
                executeResult.created(),
                executeResult.updated(),
                executeResult.mapped());
        return projectedServiceMappings;
    }
}

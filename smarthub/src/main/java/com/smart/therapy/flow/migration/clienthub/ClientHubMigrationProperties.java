package com.smart.therapy.flow.migration.clienthub;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "clienthub.migration")
public class ClientHubMigrationProperties {

    private boolean enabled = false;
    private boolean dryRun = true;
    private boolean executeStaffAuth = false;
    private boolean executeClients = false;
    private boolean executeServices = false;
    private boolean executeSessions = false;
    private boolean executeBilling = false;
    private boolean executeDocuments = false;
    private boolean executeRoomIntegrations = false;
    private boolean executeTherapistScheduling = false;
    private boolean executeTasks = false;
    private boolean executeNotifications = false;
    private boolean executeClinicalExtras = false;
    private boolean executeAssessments = false;
    private boolean executeTranscripts = false;
    private boolean executeDocumentBinaries = false;
    /** How many months of in-app/scheduled notification history to import. 0 = all history. */
    private int notificationLookbackMonths = 1;
    private boolean syncEnabled = false;
    private boolean syncEnqueueEvents = false;
    private boolean syncApplyEvents = false;
    private int syncBatchSize = 500;
    private boolean demoCleanupDryRunEnabled = false;
    private String sourceUrl;
    private String sourceUsername;
    private String sourcePassword;
    private String sourceSchema = "public";
    private String targetOrganisationSlug;
    private String targetSchemaName;
    private String sourceTimezone = "UTC";
    private int connectTimeoutSeconds = 15;
    private String sourceAzureConnectionString;
    private String sourceAzureContainerName = "documents";

    void validateDryRunConfiguration() {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalStateException("clienthub.migration.source-url is required");
        }
        if (sourceUsername == null || sourceUsername.isBlank()) {
            throw new IllegalStateException("clienthub.migration.source-username is required");
        }
        if (sourceSchema == null || sourceSchema.isBlank()) {
            throw new IllegalStateException("clienthub.migration.source-schema is required");
        }
        if ((targetOrganisationSlug == null || targetOrganisationSlug.isBlank())
                && (targetSchemaName == null || targetSchemaName.isBlank())) {
            throw new IllegalStateException(
                    "clienthub.migration.target-organisation-slug or target-schema-name is required");
        }
        if (connectTimeoutSeconds < 1 || connectTimeoutSeconds > 120) {
            throw new IllegalStateException("clienthub.migration.connect-timeout-seconds must be between 1 and 120");
        }
        if (notificationLookbackMonths < 0 || notificationLookbackMonths > 120) {
            throw new IllegalStateException(
                    "clienthub.migration.notification-lookback-months must be between 0 and 120");
        }
    }
}

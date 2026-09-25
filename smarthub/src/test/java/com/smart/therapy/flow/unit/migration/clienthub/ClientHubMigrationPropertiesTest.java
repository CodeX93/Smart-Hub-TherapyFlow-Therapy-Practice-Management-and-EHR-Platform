package com.smart.therapy.flow.migration.clienthub;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientHubMigrationPropertiesTest {

    @Test
    void requiresSourceUrlForDryRun() {
        ClientHubMigrationProperties properties = validProperties();
        properties.setSourceUrl("");

        assertThatThrownBy(properties::validateDryRunConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("source-url");
    }

    @Test
    void requiresTargetOrganisationOrSchema() {
        ClientHubMigrationProperties properties = validProperties();
        properties.setTargetOrganisationSlug("");
        properties.setTargetSchemaName("");

        assertThatThrownBy(properties::validateDryRunConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("target-organisation-slug");
    }

    @Test
    void acceptsMinimalDryRunConfiguration() {
        assertThatCode(validProperties()::validateDryRunConfiguration)
                .doesNotThrowAnyException();
    }

    @Test
    void executeStaffAuthDefaultsToFalse() {
        ClientHubMigrationProperties properties = new ClientHubMigrationProperties();

        assertThat(properties.isExecuteStaffAuth()).isFalse();
    }

    @Test
    void executeClientsDefaultsToFalse() {
        ClientHubMigrationProperties properties = new ClientHubMigrationProperties();

        assertThat(properties.isExecuteClients()).isFalse();
    }

    @Test
    void executeServicesDefaultsToFalse() {
        ClientHubMigrationProperties properties = new ClientHubMigrationProperties();

        assertThat(properties.isExecuteServices()).isFalse();
    }

    @Test
    void executeSessionsDefaultsToFalse() {
        ClientHubMigrationProperties properties = new ClientHubMigrationProperties();

        assertThat(properties.isExecuteSessions()).isFalse();
    }

    @Test
    void executeBillingDefaultsToFalse() {
        ClientHubMigrationProperties properties = new ClientHubMigrationProperties();

        assertThat(properties.isExecuteBilling()).isFalse();
    }

    @Test
    void executeDocumentsDefaultsToFalse() {
        ClientHubMigrationProperties properties = new ClientHubMigrationProperties();

        assertThat(properties.isExecuteDocuments()).isFalse();
    }

    @Test
    void executeRoomIntegrationsDefaultsToFalse() {
        ClientHubMigrationProperties properties = new ClientHubMigrationProperties();

        assertThat(properties.isExecuteRoomIntegrations()).isFalse();
    }

    @Test
    void executeTherapistSchedulingDefaultsToFalse() {
        ClientHubMigrationProperties properties = new ClientHubMigrationProperties();

        assertThat(properties.isExecuteTherapistScheduling()).isFalse();
    }

    @Test
    void executeNotificationsDefaultsToFalse() {
        ClientHubMigrationProperties properties = new ClientHubMigrationProperties();

        assertThat(properties.isExecuteNotifications()).isFalse();
    }

    @Test
    void executeClinicalExtrasDefaultsToFalse() {
        ClientHubMigrationProperties properties = new ClientHubMigrationProperties();

        assertThat(properties.isExecuteClinicalExtras()).isFalse();
    }

    @Test
    void executeAssessmentsDefaultsToFalse() {
        ClientHubMigrationProperties properties = new ClientHubMigrationProperties();

        assertThat(properties.isExecuteAssessments()).isFalse();
    }

    @Test
    void executeTranscriptsDefaultsToFalse() {
        ClientHubMigrationProperties properties = new ClientHubMigrationProperties();

        assertThat(properties.isExecuteTranscripts()).isFalse();
    }

    @Test
    void executeDocumentBinariesDefaultsToFalse() {
        ClientHubMigrationProperties properties = new ClientHubMigrationProperties();

        assertThat(properties.isExecuteDocumentBinaries()).isFalse();
    }

    @Test
    void sourceAzureContainerDefaultsToDocuments() {
        ClientHubMigrationProperties properties = new ClientHubMigrationProperties();

        assertThat(properties.getSourceAzureContainerName()).isEqualTo("documents");
    }

    @Test
    void notificationLookbackMonthsDefaultsToOne() {
        ClientHubMigrationProperties properties = new ClientHubMigrationProperties();

        assertThat(properties.getNotificationLookbackMonths()).isEqualTo(1);
    }

    @Test
    void syncFlagsDefaultToSafeValues() {
        ClientHubMigrationProperties properties = new ClientHubMigrationProperties();

        assertThat(properties.isSyncEnabled()).isFalse();
        assertThat(properties.isSyncEnqueueEvents()).isFalse();
        assertThat(properties.isSyncApplyEvents()).isFalse();
        assertThat(properties.getSyncBatchSize()).isEqualTo(500);
    }

    @Test
    void demoCleanupDryRunDefaultsToFalse() {
        ClientHubMigrationProperties properties = new ClientHubMigrationProperties();

        assertThat(properties.isDemoCleanupDryRunEnabled()).isFalse();
    }

    private ClientHubMigrationProperties validProperties() {
        ClientHubMigrationProperties properties = new ClientHubMigrationProperties();
        properties.setSourceUrl("jdbc:postgresql://legacy.example.com:5432/clienthub");
        properties.setSourceUsername("readonly");
        properties.setSourceSchema("public");
        properties.setTargetOrganisationSlug("real-practice");
        return properties;
    }
}

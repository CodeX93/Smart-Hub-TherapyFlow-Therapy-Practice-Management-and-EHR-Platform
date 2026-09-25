package com.smart.therapy.flow.common.schema;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToFile;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaCreator;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Map;

/**
 * Generates DDL script from JPA entities for Flyway V1__schema.sql.
 * Uses Hibernate 6 schema management SPI only: SchemaManagementTool,
 * SchemaCreator, TargetDescriptor, ScriptTargetOutputToFile. Does not use
 * the legacy SchemaExport (hbm2ddl) API.
 * <p>
 * Run: {@code mvn compile exec:java}
 */
public final class SchemaGenerator {

    /** Written outside Flyway locations to avoid duplicate version conflicts with V1__platform_schema.sql */
    private static final String OUTPUT_PATH = "target/generated/schema-from-entities.sql";

    @SuppressWarnings("unchecked")
    private static Class<?>[] entityClasses() {
        return new Class<?>[] {
            // auth
            com.smart.therapy.flow.auth.entity.AuditLog.class,
            com.smart.therapy.flow.auth.entity.AuthIdentity.class,
            com.smart.therapy.flow.auth.entity.AuthIdentityRole.class,
            com.smart.therapy.flow.auth.entity.AuthSession.class,
            com.smart.therapy.flow.auth.entity.LoginAttempt.class,
            com.smart.therapy.flow.auth.entity.Permission.class,
            com.smart.therapy.flow.auth.entity.Role.class,
            com.smart.therapy.flow.auth.entity.RolePermission.class,
            com.smart.therapy.flow.auth.entity.User.class,
            com.smart.therapy.flow.auth.entity.UserActivityLog.class,
            com.smart.therapy.flow.auth.entity.UserSession.class,
            // organisation (public schema)
            com.smart.therapy.flow.organisation.entity.Organisation.class,
            com.smart.therapy.flow.organisation.entity.UserOrganisation.class,
            // subscription (SaaS billing; org never stores plan)
            com.smart.therapy.flow.subscription.entity.AppFeature.class,
            com.smart.therapy.flow.subscription.entity.SubscriptionPlan.class,
            com.smart.therapy.flow.subscription.entity.PlanFeatureVersion.class,
            com.smart.therapy.flow.subscription.entity.OrgSubscription.class,
            com.smart.therapy.flow.subscription.entity.OrgFeaturePurchase.class,
            com.smart.therapy.flow.subscription.entity.FeatureUsage.class,
            com.smart.therapy.flow.subscription.entity.FeaturePricing.class,
            com.smart.therapy.flow.subscription.entity.Invoice.class,
            // client
            com.smart.therapy.flow.client.entity.Client.class,
            com.smart.therapy.flow.client.entity.ClientAddress.class,
            com.smart.therapy.flow.client.entity.ClientContact.class,
            com.smart.therapy.flow.client.entity.ClientEmployment.class,
            com.smart.therapy.flow.client.entity.ClientHistory.class,
            com.smart.therapy.flow.client.entity.ClientInsurance.class,
            com.smart.therapy.flow.client.entity.ClientPortalSettings.class,
            com.smart.therapy.flow.client.entity.ClientPortalSession.class,
            com.smart.therapy.flow.client.entity.ClientReferral.class,
            com.smart.therapy.flow.client.entity.IdempotencyKey.class,
            com.smart.therapy.flow.client.entity.PatientConsent.class,
            // session
            com.smart.therapy.flow.session.entity.AudioFile.class,
            com.smart.therapy.flow.session.entity.Room.class,
            com.smart.therapy.flow.session.entity.RoomBooking.class,
            com.smart.therapy.flow.session.entity.Session.class,
            com.smart.therapy.flow.session.entity.SessionIntegration.class,
            com.smart.therapy.flow.session.entity.SessionNote.class,
            // assessment
            com.smart.therapy.flow.assessment.entity.AssessmentAssignment.class,
            com.smart.therapy.flow.assessment.entity.AssessmentQuestion.class,
            com.smart.therapy.flow.assessment.entity.AssessmentQuestionOption.class,
            com.smart.therapy.flow.assessment.entity.AssessmentQuestionRatingLabel.class,
            com.smart.therapy.flow.assessment.entity.AssessmentReport.class,
            com.smart.therapy.flow.assessment.entity.AssessmentReportVersion.class,
            com.smart.therapy.flow.assessment.entity.AssessmentResponse.class,
            com.smart.therapy.flow.assessment.entity.AssessmentResponseOption.class,
            com.smart.therapy.flow.assessment.entity.AssessmentSection.class,
            com.smart.therapy.flow.assessment.entity.AssessmentTemplate.class,
            // document
            com.smart.therapy.flow.document.entity.Document.class,
            com.smart.therapy.flow.document.entity.FormAssignment.class,
            com.smart.therapy.flow.document.entity.FormAssignmentField.class,
            com.smart.therapy.flow.document.entity.FormField.class,
            com.smart.therapy.flow.document.entity.FormFieldOption.class,
            com.smart.therapy.flow.document.entity.FormResponse.class,
            com.smart.therapy.flow.document.entity.FormSection.class,
            com.smart.therapy.flow.document.entity.FormSignature.class,
            com.smart.therapy.flow.document.entity.FormTemplate.class,
            com.smart.therapy.flow.document.entity.FormTemplateVersion.class,
            com.smart.therapy.flow.document.entity.LibraryCategory.class,
            com.smart.therapy.flow.document.entity.LibraryEntry.class,
            com.smart.therapy.flow.document.entity.LibraryEntryConnection.class,
            com.smart.therapy.flow.document.entity.LibraryEntryTag.class,
            com.smart.therapy.flow.document.entity.LibraryTag.class,
            com.smart.therapy.flow.document.entity.Note.class,
            // user
            com.smart.therapy.flow.user.entity.SupervisorAssignment.class,
            com.smart.therapy.flow.user.entity.TherapistBlockedTime.class,
            com.smart.therapy.flow.user.entity.UserContact.class,
            com.smart.therapy.flow.user.entity.UserIdempotencyKey.class,
            com.smart.therapy.flow.user.entity.UserIntegration.class,
            com.smart.therapy.flow.user.entity.UserProfile.class,
            com.smart.therapy.flow.user.entity.UserProfileAgeGroup.class,
            com.smart.therapy.flow.user.entity.UserProfileAward.class,
            com.smart.therapy.flow.user.entity.UserProfileCertification.class,
            com.smart.therapy.flow.user.entity.UserProfileContinuingEducation.class,
            com.smart.therapy.flow.user.entity.UserProfileEducation.class,
            com.smart.therapy.flow.user.entity.UserProfileLanguage.class,
            com.smart.therapy.flow.user.entity.UserProfileMembership.class,
            com.smart.therapy.flow.user.entity.UserProfilePhysicalRoom.class,
            com.smart.therapy.flow.user.entity.UserProfilePreviousPosition.class,
            com.smart.therapy.flow.user.entity.UserProfilePublication.class,
            com.smart.therapy.flow.user.entity.UserProfileReference.class,
            com.smart.therapy.flow.user.entity.UserProfileSpecialization.class,
            com.smart.therapy.flow.user.entity.UserProfileTreatmentApproach.class,
            com.smart.therapy.flow.user.entity.UserProfileWorkingHours.class,
            // notification
            com.smart.therapy.flow.notification.entity.NotificationActionMetadata.class,
            com.smart.therapy.flow.notification.entity.Notification.class,
            com.smart.therapy.flow.notification.entity.NotificationDeliveryLog.class,
            com.smart.therapy.flow.notification.entity.NotificationPreference.class,
            com.smart.therapy.flow.notification.entity.NotificationTemplate.class,
            com.smart.therapy.flow.notification.entity.NotificationTrigger.class,
            com.smart.therapy.flow.notification.entity.ScheduledNotification.class,
            // system
            com.smart.therapy.flow.system.entity.OptionCategory.class,
            com.smart.therapy.flow.system.entity.PracticeConfiguration.class,
            com.smart.therapy.flow.system.entity.RecentItem.class,
            com.smart.therapy.flow.system.entity.SystemOption.class,
            // task
            com.smart.therapy.flow.task.entity.ChecklistItem.class,
            com.smart.therapy.flow.task.entity.ChecklistTemplate.class,
            com.smart.therapy.flow.task.entity.ClientChecklist.class,
            com.smart.therapy.flow.task.entity.ClientChecklistItem.class,
            com.smart.therapy.flow.task.entity.Task.class,
            com.smart.therapy.flow.task.entity.TaskComment.class,
            // billing
            com.smart.therapy.flow.billing.entity.InvoicePolicy.class,
            com.smart.therapy.flow.billing.entity.Payment.class,
            com.smart.therapy.flow.billing.entity.PaymentTransaction.class,
            com.smart.therapy.flow.billing.entity.SessionBilling.class,
            com.smart.therapy.flow.billing.entity.Service.class,
        };
    }

    public static void main(String[] args) throws Exception {
        ServiceRegistry registry = new StandardServiceRegistryBuilder()
            .applySetting(AvailableSettings.DIALECT, PostgreSQLDialect.class.getName())
            .applySetting(AvailableSettings.HBM2DDL_CHARSET_NAME, StandardCharsets.UTF_8.name())
            .build();

        MetadataSources sources = new MetadataSources(registry);
        for (Class<?> entityClass : entityClasses()) {
            sources.addAnnotatedClass((Class<?>) entityClass);
        }

        var metadata = sources.buildMetadata();
        File outputFile = new File(OUTPUT_PATH);
        outputFile.getParentFile().mkdirs();

        ScriptTargetOutput scriptOutput = new ScriptTargetOutputToFile(outputFile, StandardCharsets.UTF_8.name(), false);
        TargetDescriptor scriptTarget = new TargetDescriptor() {
            @Override
            public EnumSet<TargetType> getTargetTypes() {
                return EnumSet.of(TargetType.SCRIPT);
            }
            @Override
            public ScriptTargetOutput getScriptTargetOutput() {
                return scriptOutput;
            }
        };

        ExecutionOptions executionOptions = new ExecutionOptions() {
            @Override
            public Map<String, Object> getConfigurationValues() { return Map.of(); }
            @Override
            public boolean shouldManageNamespaces() { return false; }
            @Override
            public ExceptionHandler getExceptionHandler() {
                return exception -> { };
            }
        };
        SourceDescriptor sourceDescriptor = new SourceDescriptor() {
            @Override
            public SourceType getSourceType() { return SourceType.METADATA; }
            @Override
            public org.hibernate.tool.schema.spi.ScriptSourceInput getScriptSourceInput() { return null; }
        };
        ContributableMatcher contributableMatcher = ContributableMatcher.ALL;

        SchemaManagementTool tool = registry.getService(SchemaManagementTool.class);
        SchemaCreator creator = tool.getSchemaCreator(Map.of());
        creator.doCreation(metadata, executionOptions, contributableMatcher, sourceDescriptor, scriptTarget);

        System.out.println("Schema written to " + outputFile.getAbsolutePath());
    }
}

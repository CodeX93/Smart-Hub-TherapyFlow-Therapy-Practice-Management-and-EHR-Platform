package com.smart.therapy.flow.integration.repository;

import com.smart.therapy.flow.common.BaseTenantRepositoryTest;
import com.smart.therapy.flow.notification.entity.ScheduledNotification;
import com.smart.therapy.flow.notification.enums.ScheduledNotificationStatus;
import com.smart.therapy.flow.notification.repository.ScheduledNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * scheduled_notifications.entity_data is a plain text column; rows are written by whichever
 * build scheduled them and must stay readable from the scheduler's poll. Mapping the field
 * with @Lob made Hibernate read it as a CLOB, which the Postgres driver refuses on a text
 * column ("Unable to access lob stream"), so every DELAYED notification silently never sent.
 */
class ScheduledNotificationRepositoryIntegrationTest extends BaseTenantRepositoryTest {
    @Autowired private TestEntityManager entityManager;
    @Autowired private ScheduledNotificationRepository scheduledNotifications;

    private String largePayload;

    @BeforeEach
    void fixtures() {
        persistTherapist();
        largePayload = "{\"therapistId\":1,\"sessionId\":42,\"note\":\""
                + "reminder-details-".repeat(20_000) + "\"}";
        insertRow(ScheduledNotificationStatus.PENDING, largePayload, null);
        insertRow(ScheduledNotificationStatus.FAILED, "{\"therapistId\":1}",
                "No recipients resolved from trigger rules");
        entityManager.clear();
    }

    /** Rows persisted as plain text (as production wrote them) must come back through the poll query. */
    private void insertRow(ScheduledNotificationStatus status, String entityData, String lastError) {
        entityManager.getEntityManager().createNativeQuery(
                        "insert into scheduled_notifications "
                                + "(status, execute_at, retry_count, entity_data, last_error, "
                                + " createdat, updatedat, created_by, updated_by, is_deleted) "
                                + "values (:status, :executeAt, :retryCount, :entityData, :lastError, "
                                + " :now, :now, 1, 1, false)")
                .setParameter("status", status.name())
                .setParameter("executeAt", Instant.now().minusSeconds(3600))
                .setParameter("retryCount", lastError == null ? 0 : 1)
                .setParameter("entityData", entityData)
                .setParameter("lastError", lastError)
                .setParameter("now", Instant.now())
                .executeUpdate();
    }

    @Test
    void pollReadsLargePlainTextEntityData() {
        List<ScheduledNotification> due = scheduledNotifications
                .findByStatusAndExecuteAtBefore(ScheduledNotificationStatus.PENDING, Instant.now());

        assertThat(due).hasSize(1);
        assertThat(due.get(0).getEntityData()).isEqualTo(largePayload);
        assertThat(due.get(0).getEntityData().length()).isGreaterThan(300_000);
    }

    @Test
    void pollLeavesFailedRowsAlone() {
        List<ScheduledNotification> due = scheduledNotifications
                .findByStatusAndExecuteAtBefore(ScheduledNotificationStatus.PENDING, Instant.now());

        assertThat(due).extracting(ScheduledNotification::getStatus)
                .containsOnly(ScheduledNotificationStatus.PENDING);
    }
}

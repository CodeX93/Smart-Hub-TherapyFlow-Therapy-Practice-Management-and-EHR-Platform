package com.smart.therapy.flow.audit.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Defers audit writes until after the current transaction commits.
 * <p>
 * This is the <b>success-path</b> half of the audit write contract implemented by
 * {@link com.smart.therapy.flow.audit.service.AuditLogService#write}:
 * <ul>
 *   <li>PostgreSQL marks connections read-only for {@code @Transactional(readOnly = true)} —
 *       an INSERT mid-flight aborts the connection and breaks subsequent reads.</li>
 *   <li>Private {@code @Transactional(REQUIRES_NEW)} methods never open a new TX
 *       (Spring proxy self-invocation).</li>
 *   <li>Deferring until after commit also means rolled-back business work is not audited,
 *       matching “audit completed operations”.</li>
 * </ul>
 * Security events that must survive rollback use {@code AuditLogService#writeImmediate}.
 */
public final class AfterCommitAudit {

    private static final Logger log = LoggerFactory.getLogger(AfterCommitAudit.class);

    private AfterCommitAudit() {
    }

    /**
     * Run {@code write} after the current transaction commits, or immediately when no
     * transaction synchronization is active.
     */
    public static void run(Runnable write) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        write.run();
                    } catch (Exception e) {
                        log.error("Deferred audit write failed after commit", e);
                    }
                }
            });
        } else {
            write.run();
        }
    }
}

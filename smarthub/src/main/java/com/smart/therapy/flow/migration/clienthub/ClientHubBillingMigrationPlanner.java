package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.BillingTargetState;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceBillingInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceBillingRef;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourcePaymentTransactionRef;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ClientHubBillingMigrationPlanner {

    BillingMigrationPlan buildPlan(
            SourceBillingInventory inventory,
            List<SourceBillingRef> sourceBillings,
            List<SourcePaymentTransactionRef> sourcePayments,
            BillingTargetState targetState) {
        int billingsWouldCreate = 0;
        int billingsWouldUpdateMapped = 0;
        int billingsBlocked = 0;
        int billingsWithUnmappedSession = 0;
        int billingsSkippedDuplicateSession = 0;
        Set<String> mappedOrProjectedBillings = new java.util.HashSet<>(targetState.mappedBillingIds());
        // TherapyFlow enforces one session_billing per session_id; keep first source billing per session.
        Set<String> claimedSessions = new java.util.HashSet<>();

        for (SourceBillingRef source : sourceBillings) {
            if (source.missingRequiredFields()) {
                billingsBlocked++;
                continue;
            }
            if (!targetState.mappedSessionIds().contains(source.sessionLegacyId())) {
                // Live source can add sessions after the session execute; skip rather than block the rest.
                billingsWithUnmappedSession++;
                continue;
            }
            if (targetState.mappedBillingIds().contains(source.legacyBillingPk())) {
                mappedOrProjectedBillings.add(source.legacyBillingPk());
                claimedSessions.add(source.sessionLegacyId());
                billingsWouldUpdateMapped++;
                continue;
            }
            if (!claimedSessions.add(source.sessionLegacyId())) {
                billingsSkippedDuplicateSession++;
                continue;
            }
            mappedOrProjectedBillings.add(source.legacyBillingPk());
            billingsWouldCreate++;
        }

        int paymentsWouldCreate = 0;
        int paymentsWouldUpdateMapped = 0;
        int paymentsBlocked = 0;
        int paymentsWithUnmappedBilling = 0;
        for (SourcePaymentTransactionRef source : sourcePayments) {
            if (source.missingRequiredFields()) {
                paymentsBlocked++;
                continue;
            }
            if (!mappedOrProjectedBillings.contains(source.billingLegacyId())) {
                paymentsWithUnmappedBilling++;
                continue;
            }
            if (targetState.mappedPaymentTransactionIds().contains(source.legacyPaymentTransactionPk())) {
                paymentsWouldUpdateMapped++;
            } else {
                paymentsWouldCreate++;
            }
        }

        List<String> blockers = new ArrayList<>();
        addMessage(blockers, inventory.billingMissingSessionRows(), "Billing rows missing session_id");
        addMessage(blockers, inventory.billingBlankServiceCodeRows(), "Billing rows missing service_code");
        addMessage(blockers, inventory.billingMissingRateRows(), "Billing rows missing rate_per_unit");
        addMessage(blockers, inventory.billingMissingTotalRows(), "Billing rows missing total_amount");
        addMessage(blockers, inventory.paymentMissingBillingRows(), "Payment transaction rows missing session_billing_id");
        addMessage(blockers, inventory.paymentBlankSourceRows(), "Payment transaction rows missing source");
        addMessage(blockers, inventory.paymentMissingAmountRows(), "Payment transaction rows missing amount");

        List<String> warnings = new ArrayList<>();
        addMessage(warnings, billingsWithUnmappedSession, "Billing rows skipped because session is not mapped yet");
        addMessage(warnings, billingsSkippedDuplicateSession,
                "Billing rows skipped because TherapyFlow allows only one billing per session");
        addMessage(warnings, paymentsWithUnmappedBilling, "Payment transactions skipped because billing is not mapped yet");

        return new BillingMigrationPlan(
                sourceBillings.size(),
                billingsWouldCreate,
                billingsWouldUpdateMapped,
                billingsBlocked,
                sourcePayments.size(),
                paymentsWouldCreate,
                paymentsWouldUpdateMapped,
                paymentsBlocked,
                blockers,
                warnings);
    }

    private void addMessage(List<String> messages, long count, String label) {
        if (count > 0) {
            messages.add(label + ": " + count);
        }
    }

    record BillingMigrationPlan(
            int sourceBillings,
            int billingsWouldCreate,
            int billingsWouldUpdateMapped,
            int billingsBlocked,
            int sourcePaymentTransactions,
            int paymentsWouldCreate,
            int paymentsWouldUpdateMapped,
            int paymentsBlocked,
            List<String> blockers,
            List<String> warnings) {

        boolean blocked() {
            return billingsBlocked > 0 || paymentsBlocked > 0 || !blockers.isEmpty();
        }
    }
}

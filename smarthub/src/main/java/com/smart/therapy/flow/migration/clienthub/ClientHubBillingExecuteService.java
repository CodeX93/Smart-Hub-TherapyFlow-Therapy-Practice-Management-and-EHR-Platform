package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.billing.entity.Payment;
import com.smart.therapy.flow.billing.entity.PaymentTransaction;
import com.smart.therapy.flow.billing.entity.SessionBilling;
import com.smart.therapy.flow.billing.enums.BillingStatus;
import com.smart.therapy.flow.billing.enums.DiscountType;
import com.smart.therapy.flow.billing.enums.PaymentMethod;
import com.smart.therapy.flow.billing.enums.PaymentSource;
import com.smart.therapy.flow.billing.enums.PaymentStatus;
import com.smart.therapy.flow.billing.enums.TransactionType;
import com.smart.therapy.flow.billing.repository.PaymentRepository;
import com.smart.therapy.flow.billing.repository.PaymentTransactionRepository;
import com.smart.therapy.flow.billing.repository.SessionBillingRepository;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceBillingRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourcePaymentTransactionRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import com.smart.therapy.flow.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class ClientHubBillingExecuteService {

    // Azure DBs stall on long tenant writes; keep billing/payment batches small.
    private static final int BATCH_SIZE = 25;

    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final JdbcTemplate jdbcTemplate;
    private final SessionBillingRepository sessionBillingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SessionRepository sessionRepository;

    public BillingExecuteResult execute(
            List<SourceBillingRecord> sourceBillings,
            List<SourcePaymentTransactionRecord> sourcePaymentTransactions,
            TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException("Exactly one target organisation is required for billing/payment execution");
        }

        // Process every source row: create new mappings or update existing ones in place (no duplicates).
        BillingExecuteResult total = BillingExecuteResult.empty();
        int billingBatchNumber = 0;
        for (int start = 0; start < sourceBillings.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, sourceBillings.size());
            int currentBatch = ++billingBatchNumber;
            List<SourceBillingRecord> batch = sourceBillings.subList(start, end);
            BillingExecuteResult batchResult = tenantTransactionExecutor.executeWrite(
                    target.organisationId(), target.schemaName(),
                    () -> executeBillingBatch(batch, target));
            total = total.plus(batchResult);
            log.info("ClientHubAI billing execute batch complete: batch={} batch_size={} processed={} total={}",
                    currentBatch, batch.size(), total.sourceBillings(), sourceBillings.size());
        }

        int paymentBatchNumber = 0;
        for (int start = 0; start < sourcePaymentTransactions.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, sourcePaymentTransactions.size());
            int currentBatch = ++paymentBatchNumber;
            List<SourcePaymentTransactionRecord> batch = sourcePaymentTransactions.subList(start, end);
            BillingExecuteResult batchResult = tenantTransactionExecutor.executeWrite(
                    target.organisationId(), target.schemaName(),
                    () -> executePaymentBatch(batch, target));
            total = total.plus(batchResult);
            log.info("ClientHubAI payment execute batch complete: batch={} batch_size={} processed={} total={}",
                    currentBatch, batch.size(), total.sourcePaymentTransactions(), sourcePaymentTransactions.size());
        }

        return new BillingExecuteResult(
                sourceBillings.size(),
                total.billingsCreated(),
                total.billingsUpdated(),
                total.billingMappedReruns(),
                sourcePaymentTransactions.size(),
                total.paymentsCreated(),
                total.paymentsUpdated(),
                total.transactionMappedReruns());
    }

    private BillingExecuteResult executeBillingBatch(List<SourceBillingRecord> sourceBillings, TargetInventory target) {
        Map<String, Long> sessionMappings = loadMappings(target.organisationId(), "sessions");
        Map<String, Long> billingMappings = loadMappings(target.organisationId(), "session_billing");

        int billingsCreated = 0;
        int billingsUpdated = 0;
        int billingMappedReruns = 0;
        for (SourceBillingRecord source : sourceBillings) {
            Optional<Long> mappedBillingId = Optional.ofNullable(billingMappings.get(source.legacyBillingPk()));
            if (!sessionMappings.containsKey(source.sessionLegacyId())) {
                log.warn("ClientHubAI billing skipped; session is not mapped yet: billing_id={} session_id={}",
                        source.legacyBillingPk(), source.sessionLegacyId());
                continue;
            }

            Long targetSessionId = requiredMapping(sessionMappings, source.sessionLegacyId(), "sessions");
            SessionBilling billing;
            boolean mappedRerun = false;
            if (mappedBillingId.isPresent()) {
                billing = sessionBillingRepository.findById(mappedBillingId.get()).orElseGet(SessionBilling::new);
                mappedRerun = billing.getId() != null;
            } else {
                Optional<SessionBilling> existingBySession = sessionBillingRepository.findBySessionId(targetSessionId);
                if (existingBySession.isPresent()) {
                    Long existingId = existingBySession.get().getId();
                    boolean alreadyClaimedByOtherSource = billingMappings.containsValue(existingId);
                    if (alreadyClaimedByOtherSource) {
                        log.warn("ClientHubAI billing skipped; session already has a mapped billing: "
                                        + "billing_id={} session_id={} existing_target_billing_id={}",
                                source.legacyBillingPk(), source.sessionLegacyId(), existingId);
                        continue;
                    }
                }
                billing = existingBySession.orElseGet(SessionBilling::new);
            }

            boolean existing = billing.getId() != null;
            applyBillingFields(billing, source, targetSessionId);
            SessionBilling saved = sessionBillingRepository.save(billing);
            billingMappings.put(source.legacyBillingPk(), saved.getId());
            upsertLegacyMapping(target, "session_billing", "session_billing",
                    source.legacyBillingPk(), saved.getId(), billingChecksum(source));

            if (existing) {
                billingsUpdated++;
                if (mappedRerun) {
                    billingMappedReruns++;
                }
            } else {
                billingsCreated++;
            }
        }

        return new BillingExecuteResult(
                sourceBillings.size(),
                billingsCreated,
                billingsUpdated,
                billingMappedReruns,
                0,
                0,
                0,
                0);
    }

    private BillingExecuteResult executePaymentBatch(
            List<SourcePaymentTransactionRecord> sourcePaymentTransactions,
            TargetInventory target) {
        Map<String, Long> billingMappings = loadMappings(target.organisationId(), "session_billing");
        Map<String, Long> transactionMappings = loadMappings(target.organisationId(), "payment_transactions");
        ZoneId practiceZone = target.zoneIdOrUtc();

        int paymentsCreated = 0;
        int paymentsUpdated = 0;
        int transactionMappedReruns = 0;
        for (SourcePaymentTransactionRecord source : sourcePaymentTransactions) {
            Optional<Long> mappedTransactionId = Optional.ofNullable(
                    transactionMappings.get(source.legacyPaymentTransactionPk()));
            if (!billingMappings.containsKey(source.billingLegacyId())) {
                log.warn("ClientHubAI payment skipped; billing is not mapped yet: payment_id={} billing_id={}",
                        source.legacyPaymentTransactionPk(), source.billingLegacyId());
                continue;
            }

            Long targetBillingId = requiredMapping(billingMappings, source.billingLegacyId(), "session_billing");
            PaymentTransaction transaction;
            Payment payment;
            boolean mappedRerun = false;
            if (mappedTransactionId.isPresent()) {
                transaction = paymentTransactionRepository.findById(mappedTransactionId.get())
                        .orElseGet(PaymentTransaction::new);
                if (transaction.getId() != null && transaction.getPayment() != null) {
                    payment = transaction.getPayment();
                    mappedRerun = true;
                } else {
                    payment = new Payment();
                }
            } else {
                transaction = new PaymentTransaction();
                payment = new Payment();
            }

            applyPaymentFields(payment, source, targetBillingId, practiceZone);
            Payment savedPayment = paymentRepository.save(payment);
            applyTransactionFields(transaction, source, savedPayment);
            PaymentTransaction savedTransaction = paymentTransactionRepository.save(transaction);
            transactionMappings.put(source.legacyPaymentTransactionPk(), savedTransaction.getId());
            upsertLegacyMapping(target, "payment_transactions", "payment_transactions",
                    source.legacyPaymentTransactionPk(), savedTransaction.getId(), paymentChecksum(source));

            if (mappedRerun) {
                paymentsUpdated++;
                transactionMappedReruns++;
            } else if (mappedTransactionId.isPresent()) {
                // Mapping existed but row was missing — recreated without duplicating the mapping key.
                paymentsCreated++;
                transactionMappedReruns++;
            } else {
                paymentsCreated++;
            }
        }

        return new BillingExecuteResult(
                0,
                0,
                0,
                0,
                sourcePaymentTransactions.size(),
                paymentsCreated,
                paymentsUpdated,
                transactionMappedReruns);
    }

    private void applyBillingFields(SessionBilling billing, SourceBillingRecord source, Long targetSessionId) {
        BigDecimal paidAmount = coalesce(source.clientPaidAmount()).add(coalesce(source.insurancePaidAmount()));
        if (paidAmount.compareTo(BigDecimal.ZERO) == 0 && source.paymentAmount() != null) {
            paidAmount = source.paymentAmount();
        }
        BigDecimal subtotal = source.ratePerUnit().multiply(BigDecimal.valueOf(units(source)));
        BigDecimal outstanding = source.totalAmount().subtract(paidAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        billing.setSession(sessionRepository.getReferenceById(targetSessionId));
        billing.setServiceCode(trimTo(source.serviceCode(), 20));
        billing.setUnits(units(source));
        billing.setRatePerUnit(scale(source.ratePerUnit()));
        billing.setOriginalRatePerUnit(scale(source.ratePerUnit()));
        billing.setSubtotalAmount(scale(subtotal));
        billing.setOriginalSubtotalAmount(scale(subtotal));
        billing.setInsuranceCovered(source.insuranceCovered());
        billing.setInsuranceAmount(source.insuranceCovered() ? scale(source.totalAmount().subtract(coalesce(source.copayAmount()))) : null);
        billing.setCopayAmount(scale(source.copayAmount()));
        billing.setTotalAmount(scale(source.totalAmount()));
        billing.setPaidAmount(scale(paidAmount));
        billing.setClientPaidAmount(scale(coalesce(source.clientPaidAmount())));
        billing.setInsurancePaidAmount(scale(coalesce(source.insurancePaidAmount())));
        billing.setOutstandingAmount(outstanding);
        billing.setBillingStatus(mapBillingStatus(source.paymentStatus(), paidAmount, source.totalAmount()));
        billing.setBillingDate(source.billingDate());
        billing.setDueDate(source.billingDate());
        billing.setDiscountType(mapDiscountType(source.discountType()));
        billing.setDiscountValue(scale(source.discountValue()));
        billing.setDiscountAmount(scale(source.discountAmount()));
        billing.setIsDeleted(false);
        billing.setDeletedAt(null);
        billing.setCreatedBy(0L);
        billing.setUpdatedBy(0L);
    }

    private void applyPaymentFields(
            Payment payment,
            SourcePaymentTransactionRecord source,
            Long targetBillingId,
            ZoneId practiceZone) {
        payment.setSessionBilling(sessionBillingRepository.getReferenceById(targetBillingId));
        payment.setAmount(scale(source.amount()));
        payment.setPaymentMethod(mapPaymentMethod(source.paymentMethod(), source.source()));
        payment.setPaymentSource(mapPaymentSource(source.source()));
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaymentDate(toInstant(source.paymentDate(), source.recordedAt(), practiceZone));
        payment.setReference(trimTo(source.referenceNumber(), 100));
        payment.setNotes(trim(source.notes()));
        payment.setIsDeleted(false);
        payment.setDeletedAt(null);
        payment.setCreatedBy(0L);
        payment.setUpdatedBy(0L);
    }

    private void applyTransactionFields(PaymentTransaction transaction, SourcePaymentTransactionRecord source, Payment payment) {
        transaction.setPayment(payment);
        transaction.setProvider(mapPaymentSource(source.source()));
        transaction.setTransactionType(TransactionType.CHARGE);
        transaction.setAmount(scale(source.amount()));
        transaction.setProviderChargeId(trimTo(source.referenceNumber(), 255));
        transaction.setStatus(PaymentStatus.PAID);
        transaction.setFailureReason(null);
        transaction.setIsVoided(false);
        transaction.setVoidedAt(null);
        transaction.setVoidedBy(null);
        transaction.setVoidReason(null);
        if (transaction.getCreatedAt() == null) {
            transaction.setCreatedAt(source.recordedAt() == null ? Instant.now() : source.recordedAt());
        }
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

    private BillingStatus mapBillingStatus(String sourceStatus, BigDecimal paidAmount, BigDecimal totalAmount) {
        String normalized = normalize(sourceStatus);
        if ("paid".equals(normalized) || paidAmount.compareTo(totalAmount) >= 0) {
            return BillingStatus.PAID;
        }
        if ("failed".equals(normalized) || "denied".equals(normalized)) {
            return BillingStatus.DENIED;
        }
        if ("cancelled".equals(normalized) || "canceled".equals(normalized)) {
            return BillingStatus.CANCELLED;
        }
        if ("billed".equals(normalized) || "partial".equals(normalized) || paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            return BillingStatus.BILLED;
        }
        return BillingStatus.PENDING;
    }

    private DiscountType mapDiscountType(String value) {
        String normalized = normalize(value);
        if (normalized == null || normalized.isBlank() || "none".equals(normalized)) {
            return null;
        }
        if ("percentage".equals(normalized) || "percent".equals(normalized)) {
            return DiscountType.PERCENTAGE;
        }
        return DiscountType.FIXED_AMOUNT;
    }

    private PaymentSource mapPaymentSource(String value) {
        String normalized = normalize(value);
        if ("stripe".equals(normalized) || "card".equals(normalized) || "online".equals(normalized)) {
            return PaymentSource.STRIPE;
        }
        if ("insurance".equals(normalized) || "insurer".equals(normalized)) {
            return PaymentSource.INSURANCE_PORTAL;
        }
        return PaymentSource.MANUAL;
    }

    private PaymentMethod mapPaymentMethod(String method, String source) {
        String normalized = normalize(method);
        if ("cash".equals(normalized)) {
            return PaymentMethod.CASH;
        }
        if ("check".equals(normalized) || "cheque".equals(normalized)) {
            return PaymentMethod.CHECK;
        }
        if ("debitcard".equals(normalized) || "debit".equals(normalized)) {
            return PaymentMethod.DEBIT_CARD;
        }
        if ("banktransfer".equals(normalized) || "ach".equals(normalized) || "etransfer".equals(normalized)) {
            return PaymentMethod.BANK_TRANSFER;
        }
        if ("insurance".equals(normalize(source))) {
            return PaymentMethod.INSURANCE;
        }
        if ("creditbalance".equals(normalized)) {
            return PaymentMethod.CREDIT_BALANCE;
        }
        if ("online".equals(normalized) || "stripe".equals(normalized)) {
            return PaymentMethod.ONLINE_PAYMENT;
        }
        return PaymentMethod.CREDIT_CARD;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
    }

    private int units(SourceBillingRecord source) {
        return source.units() == null || source.units() <= 0 ? 1 : source.units();
    }

    private BigDecimal coalesce(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    public static Instant toInstant(LocalDate date, Instant fallback, ZoneId practiceZone) {
        if (date == null) {
            return fallback;
        }
        ZoneId zone = practiceZone == null ? ZoneId.of("UTC") : practiceZone;
        return date.atStartOfDay(zone).toInstant();
    }

    private String billingChecksum(SourceBillingRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyBillingPk(),
                source.sessionLegacyId(),
                source.serviceCode(),
                source.ratePerUnit().toPlainString(),
                source.totalAmount().toPlainString(),
                String.valueOf(source.billingDate()),
                String.valueOf(source.clientPaidAmount()),
                String.valueOf(source.insurancePaidAmount()),
                String.valueOf(source.paymentAmount()),
                String.valueOf(source.paymentStatus())));
    }

    private String paymentChecksum(SourcePaymentTransactionRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyPaymentTransactionPk(),
                source.billingLegacyId(),
                source.source(),
                source.amount().toPlainString(),
                String.valueOf(source.paymentDate()),
                String.valueOf(source.referenceNumber())));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimTo(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    public record BillingExecuteResult(
            int sourceBillings,
            int billingsCreated,
            int billingsUpdated,
            int billingMappedReruns,
            int sourcePaymentTransactions,
            int paymentsCreated,
            int paymentsUpdated,
            int transactionMappedReruns) {

        static BillingExecuteResult empty() {
            return new BillingExecuteResult(0, 0, 0, 0, 0, 0, 0, 0);
        }

        BillingExecuteResult plus(BillingExecuteResult other) {
            return new BillingExecuteResult(
                    sourceBillings + other.sourceBillings,
                    billingsCreated + other.billingsCreated,
                    billingsUpdated + other.billingsUpdated,
                    billingMappedReruns + other.billingMappedReruns,
                    sourcePaymentTransactions + other.sourcePaymentTransactions,
                    paymentsCreated + other.paymentsCreated,
                    paymentsUpdated + other.paymentsUpdated,
                    transactionMappedReruns + other.transactionMappedReruns);
        }
    }
}

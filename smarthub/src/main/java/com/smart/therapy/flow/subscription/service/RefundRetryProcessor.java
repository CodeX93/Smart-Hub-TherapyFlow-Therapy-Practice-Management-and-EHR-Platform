package com.smart.therapy.flow.subscription.service;

import com.smart.therapy.flow.payment.service.StripeRefundService;
import com.smart.therapy.flow.subscription.entity.InvoiceAdjustment;
import com.smart.therapy.flow.subscription.entity.RefundRetryTask;
import com.smart.therapy.flow.subscription.enums.InvoiceAdjustmentStatus;
import com.smart.therapy.flow.subscription.enums.InvoiceAdjustmentType;
import com.smart.therapy.flow.subscription.enums.RefundRetryStatus;
import com.smart.therapy.flow.subscription.repository.InvoiceAdjustmentRepository;
import com.smart.therapy.flow.subscription.repository.RefundRetryTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefundRetryProcessor {

    private final RefundRetryTaskRepository refundRetryTaskRepository;
    private final StripeRefundService stripeRefundService;
    private final InvoiceAdjustmentRepository invoiceAdjustmentRepository;

    @Scheduled(cron = "${subscription.lifecycle.refund-retry-cron:0 */15 * * * *}")
    @Transactional
    public void process() {
        List<RefundRetryTask> tasks = refundRetryTaskRepository.findReadyForRetry(RefundRetryStatus.PENDING, Instant.now());
        for (RefundRetryTask task : tasks) {
            try {
                String providerRef = stripeRefundService.createRefund(task.getInvoice(), task.getAmountUsd(), task.getReason());
                task.setStatus(RefundRetryStatus.SUBMITTED);
                task.setUpdatedAt(Instant.now());
                task.setLastError(null);
                refundRetryTaskRepository.save(task);

                InvoiceAdjustment adjustment = new InvoiceAdjustment();
                adjustment.setInvoice(task.getInvoice());
                adjustment.setAdjustmentType(InvoiceAdjustmentType.REFUND);
                adjustment.setAmount(task.getAmountUsd());
                adjustment.setReason(task.getReason());
                adjustment.setStatus(InvoiceAdjustmentStatus.SUBMITTED);
                adjustment.setProviderRefId(providerRef);
                adjustment.setCreatedAt(Instant.now());
                adjustment.setUpdatedAt(Instant.now());
                adjustment.setCreatedBy(task.getCreatedBy());
                invoiceAdjustmentRepository.save(adjustment);
            } catch (Exception ex) {
                int retryCount = (task.getRetryCount() != null ? task.getRetryCount() : 0) + 1;
                task.setRetryCount(retryCount);
                task.setLastError(ex.getMessage());
                task.setUpdatedAt(Instant.now());
                task.setNextRetryAt(Instant.now().plus(Math.min(60, retryCount * 5L), ChronoUnit.MINUTES));
                if (retryCount >= 12) {
                    task.setStatus(RefundRetryStatus.FAILED);
                }
                refundRetryTaskRepository.save(task);
                log.warn("Refund retry failed for taskId={} retryCount={} error={}", task.getId(), retryCount, ex.getMessage());
            }
        }
    }
}

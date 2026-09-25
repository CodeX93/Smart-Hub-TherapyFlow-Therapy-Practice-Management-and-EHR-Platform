package com.smart.therapy.flow.client.portal.util;

import com.smart.therapy.flow.billing.entity.Payment;
import com.smart.therapy.flow.billing.entity.SessionBilling;
import com.smart.therapy.flow.billing.enums.BillingStatus;
import com.smart.therapy.flow.billing.enums.PaymentStatus;
import com.smart.therapy.flow.client.portal.dto.PortalInvoiceResponse;
import com.smart.therapy.flow.session.entity.Session;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class PortalInvoiceMapper {

    private PortalInvoiceMapper() {
    }

    public static PortalInvoiceResponse toResponse(
            SessionBilling billing,
            Map<String, com.smart.therapy.flow.billing.entity.Service> serviceByCode,
            Payment latestPayment,
            ZoneId displayZone) {
        com.smart.therapy.flow.billing.entity.Service service = serviceByCode.get(billing.getServiceCode());
        Session session = billing.getSession();

        BigDecimal totalAmount = safeAmount(billing.getTotalAmount());
        BigDecimal paidAmount = safeAmount(billing.getPaidAmount());
        BigDecimal amountDue = calculateAmountDue(billing);
        BigDecimal outstanding = amountDue.subtract(paidAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal originalSubtotal = resolveOriginalSubtotal(billing, service);

        String billingStatus = billing.getBillingStatus() != null ? billing.getBillingStatus().getValue() : null;
        String paymentStatus = derivePaymentStatus(billing, paidAmount, amountDue);
        LocalDate paymentDate = resolvePaymentDate(latestPayment, displayZone);
        String paymentMethod = latestPayment != null && latestPayment.getPaymentMethod() != null
                ? latestPayment.getPaymentMethod().getValue()
                : null;

        return PortalInvoiceResponse.builder()
                .id(billing.getId())
                .sessionId(session != null ? session.getId() : null)
                .serviceCode(billing.getServiceCode())
                .serviceName(service != null ? service.getServiceName() : null)
                .sessionType(session != null ? resolveClinicalSessionType(session) : null)
                .sessionMode(session != null ? resolveSessionMode(session) : null)
                .sessionDate(session != null ? session.getSessionDate() : null)
                .units(billing.getUnits())
                .ratePerUnit(billing.getRatePerUnit())
                .originalSubtotalAmount(originalSubtotal)
                .totalAmount(totalAmount)
                .insuranceCovered(billing.getInsuranceCovered())
                .copayAmount(billing.getCopayAmount())
                .billingDate(billing.getBillingDate())
                .billingStatus(billingStatus)
                .paymentStatus(paymentStatus)
                .paymentAmount(paidAmount.compareTo(BigDecimal.ZERO) > 0 ? paidAmount : null)
                .amountDue(amountDue)
                .outstandingAmount(outstanding)
                .paymentDate(paymentDate)
                .paymentMethod(paymentMethod)
                .discountType(billing.getDiscountType() != null ? billing.getDiscountType().getValue() : null)
                .discountValue(billing.getDiscountValue())
                .discountAmount(billing.getDiscountAmount())
                .createdAt(billing.getCreatedAt())
                .build();
    }

    public static Payment resolveLatestPayment(List<Payment> payments) {
        if (payments == null || payments.isEmpty()) {
            return null;
        }
        return payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID || p.getStatus() == PaymentStatus.PARTIAL)
                .max(Comparator
                        .comparing(Payment::getPaymentDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Payment::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    public static String derivePaymentStatus(SessionBilling billing, BigDecimal paidAmount, BigDecimal amountDue) {
        BillingStatus billingStatus = billing.getBillingStatus();
        if (billingStatus == BillingStatus.CANCELLED) {
            return "cancelled";
        }
        if (billingStatus == BillingStatus.DENIED) {
            return "denied";
        }
        BigDecimal due = amountDue != null ? amountDue : BigDecimal.ZERO;
        BigDecimal paid = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        if (billingStatus == BillingStatus.PAID
                || (due.compareTo(BigDecimal.ZERO) > 0 && paid.compareTo(due) >= 0)) {
            return "paid";
        }
        if (paid.compareTo(BigDecimal.ZERO) > 0 && paid.compareTo(due) < 0) {
            return "partial";
        }
        return "unpaid";
    }

    public static BigDecimal calculateAmountDue(SessionBilling billing) {
        BigDecimal amountDue = safeAmount(billing.getTotalAmount());
        if (billing.getDiscountAmount() != null) {
            amountDue = amountDue.subtract(billing.getDiscountAmount());
        }
        if (amountDue.compareTo(BigDecimal.ZERO) < 0) {
            amountDue = BigDecimal.ZERO;
        }
        return amountDue.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal resolveOriginalSubtotal(
            SessionBilling billing,
            com.smart.therapy.flow.billing.entity.Service service) {
        if (billing.getOriginalSubtotalAmount() != null) {
            return billing.getOriginalSubtotalAmount().setScale(2, RoundingMode.HALF_UP);
        }
        if (billing.getOriginalRatePerUnit() != null) {
            int units = billing.getUnits() != null ? billing.getUnits() : 1;
            return billing.getOriginalRatePerUnit()
                    .multiply(BigDecimal.valueOf(units))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        if (billing.getInvoicePolicyId() != null && service != null && service.getBaseRate() != null) {
            int units = billing.getUnits() != null ? billing.getUnits() : 1;
            return service.getBaseRate()
                    .multiply(BigDecimal.valueOf(units))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return safeAmount(billing.getTotalAmount()).setScale(2, RoundingMode.HALF_UP);
    }

    private static LocalDate resolvePaymentDate(Payment latestPayment, ZoneId displayZone) {
        if (latestPayment == null || latestPayment.getPaymentDate() == null) {
            return null;
        }
        Instant paymentInstant = latestPayment.getPaymentDate();
        ZoneId zone = displayZone != null ? displayZone : ZoneId.of("UTC");
        return paymentInstant.atZone(zone).toLocalDate();
    }

    private static BigDecimal safeAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static String resolveClinicalSessionType(Session session) {
        if (session.getClinicalSessionType() != null && StringUtils.hasText(session.getClinicalSessionType())) {
            return session.getClinicalSessionType();
        }
        if (session.getService() != null) {
            if (StringUtils.hasText(session.getService().getCategory())) {
                return session.getService().getCategory();
            }
            return session.getService().getServiceName();
        }
        return null;
    }

    private static String resolveSessionMode(Session session) {
        return session.getSessionType() != null ? session.getSessionType() : null;
    }
}

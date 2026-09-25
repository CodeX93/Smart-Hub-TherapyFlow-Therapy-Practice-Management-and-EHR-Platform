package com.smart.therapy.flow.billing.service;

import com.smart.therapy.flow.billing.entity.SessionBilling;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CaseloadScopeService;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.session.entity.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class BillingGuard {

    private final CaseloadScopeService caseloadScopeService;

    public void requireTenantContext() {
        Long orgId = TenantContext.getOrganisationId();
        if (orgId == null) {
            throw new BadRequestException("Tenant context is required for billing operations");
        }
    }

    public void requireSessionForBilling(Session session) {
        if (session == null) {
            throw new BadRequestException("Session is required to create billing");
        }
        if (session.getId() == null) {
            throw new BadRequestException("Session must be persisted before billing can be created");
        }
        if (!com.smart.therapy.flow.system.service.SystemOptionKeyMatcher.matchesAny(
                session.getStatus(), "completed", "no-show", "no_show")) {
            throw new BadRequestException("Billing is allowed only for completed or no-show sessions");
        }
        if (session.getSessionDate() == null || !Instant.now().isAfter(session.getSessionDate())) {
            throw new BadRequestException("Billing is allowed only after the session's scheduled time");
        }
    }

    public void requireServiceConfigured(com.smart.therapy.flow.billing.entity.Service service, String serviceCode) {
        if (!StringUtils.hasText(serviceCode)) {
            throw new BadRequestException("Service or rate is required before billing can be created");
        }
        if (service == null && !StringUtils.hasText(serviceCode)) {
            throw new BadRequestException("Service or rate is required before billing can be created");
        }
    }

    public void requireRateConfigured(BigDecimal unitRate) {
        if (unitRate == null) {
            throw new BadRequestException("Service base rate is required before billing can be created");
        }
    }

    public void assertStaffBillingAccess(SessionBilling billing, AuthPrincipal requester) {
        Objects.requireNonNull(billing, "Billing is required");
        Objects.requireNonNull(requester, "Requester is required");
        if (billing.getSession() == null || billing.getSession().getClient() == null) {
            throw new BadRequestException("Billing record is missing client context");
        }
        var scope = caseloadScopeService.resolve(requester);
        // Match billing list visibility, including custom roles and team restrictions.
        boolean allowed = switch (scope.scope()) {
            case ALL -> true;
            case NONE -> false;
            case OWN -> billing.getSession().getTherapist() != null
                    && scope.includesTherapist(billing.getSession().getTherapist().getId());
            case TEAM, TEAM_AND_OWN -> billing.getSession().getClient().getAssignedTherapist() != null
                    && scope.includesTherapist(billing.getSession().getClient().getAssignedTherapist().getId());
        };
        if (!allowed) {
            throw new ForbiddenException("You do not have access to manage billing for this client");
        }
    }

    public void assertClientOwnsInvoice(SessionBilling billing, Long clientId) {
        if (clientId == null) {
            return;
        }
        if (billing.getSession() == null || billing.getSession().getClient() == null
                || !Objects.equals(billing.getSession().getClient().getId(), clientId)) {
            throw new ForbiddenException("Invoice does not belong to the specified client");
        }
    }

    public void assertInvoiceNotPaid(SessionBilling billing) {
        if (billing.getBillingStatus() == com.smart.therapy.flow.billing.enums.BillingStatus.PAID) {
            throw new BadRequestException("Invoice is already paid");
        }
    }

    public void rejectNegativePaymentDelta(BigDecimal delta) {
        if (delta != null && delta.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Invalid payment amount: cumulative total cannot be less than amount already recorded");
        }
    }

    public void rejectPaymentExceedingOutstanding(BigDecimal paymentDelta, BigDecimal outstandingBalance) {
        if (paymentDelta == null || paymentDelta.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal outstanding = outstandingBalance != null
                ? outstandingBalance.max(BigDecimal.ZERO)
                : BigDecimal.ZERO;
        if (paymentDelta.compareTo(outstanding) > 0) {
            throw new BadRequestException(String.format(
                    "Payment amount $%s exceeds the outstanding balance of $%s. Enter an amount less than or equal to the outstanding invoice total.",
                    paymentDelta.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                    outstanding.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()));
        }
    }

    public void assertValidPaymentDelta(BigDecimal delta) {
        if (delta == null || delta.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Invalid payment amount: payment delta must be greater than zero");
        }
    }

    public void assertTenantOrganisationMatches(Long metadataOrganisationId) {
        Long tenantOrganisationId = TenantContext.getOrganisationId();
        if (tenantOrganisationId != null && metadataOrganisationId != null
                && !Objects.equals(tenantOrganisationId, metadataOrganisationId)) {
            throw new ForbiddenException("Invoice belongs to another organisation");
        }
    }

    public void assertConnectedAccountMatches(String expectedAccountId, String metadataAccountId) {
        if (StringUtils.hasText(expectedAccountId) && StringUtils.hasText(metadataAccountId)
                && !expectedAccountId.equals(metadataAccountId)) {
            throw new BadRequestException("Stripe webhook connected account mismatch");
        }
    }

}

package com.smart.therapy.flow.session.service;

import com.smart.therapy.flow.billing.dto.CreateSessionBillingRequest;
import com.smart.therapy.flow.billing.dto.SessionBillingResponse;
import com.smart.therapy.flow.billing.service.BillingService;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.system.service.SystemOptionKeyMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

/**
 * Owns the transaction boundaries for billable session outcomes.
 *
 * The caller first persists the required failure fallback ({@code scheduled}), then invokes
 * {@link #applyOutcomeAndCreateBilling}. If billing fails, this second transaction rolls back
 * as a unit and the separately committed scheduled fallback remains durable.
 */
@Service
@RequiredArgsConstructor
public class SessionOutcomeTransactionService {

    private final SessionRepository sessionRepository;
    private final BillingService billingService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistScheduledFallback(Long sessionId) {
        Session session = sessionRepository.findByIdWithRelations(sessionId)
                .orElseThrow(() -> new BadRequestException("Session not found"));
        session.setStatus("scheduled");
        sessionRepository.saveAndFlush(session);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OutcomeResult applyOutcomeAndCreateBilling(
            Long sessionId,
            String outcomeStatus,
            AuthPrincipal requester,
            String ipAddress) {
        Session session = sessionRepository.findByIdWithRelations(sessionId)
                .orElseThrow(() -> new BadRequestException("Session not found"));
        requireEligibleOutcomeTime(session, outcomeStatus);

        session.setStatus(outcomeStatus);
        Session updated = sessionRepository.saveAndFlush(session);

        SessionBillingResponse billing = null;
        if (!billingService.hasSessionBilling(sessionId)) {
            CreateSessionBillingRequest request = new CreateSessionBillingRequest();
            request.setSessionId(sessionId);
            billing = billingService.createSessionBilling(request, requester, ipAddress);
        }
        return new OutcomeResult(updated, billing);
    }

    static void requireEligibleOutcomeTime(Session session, String outcomeStatus) {
        if (!SystemOptionKeyMatcher.matchesAny(outcomeStatus, "completed", "no-show", "no_show")) {
            throw new BadRequestException("Only completed or no-show are billable session outcomes");
        }
        Instant scheduledAt = session.getSessionDate();
        if (scheduledAt == null) {
            throw new BadRequestException("Session date is required before marking the session as " + outcomeStatus);
        }
        if (!Instant.now().isAfter(scheduledAt)) {
            throw new BadRequestException(
                    "Cannot mark session as " + outcomeStatus + " until after its scheduled time (" + scheduledAt + ")");
        }
    }

    public record OutcomeResult(Session session, SessionBillingResponse billing) {
        public OutcomeResult {
            Objects.requireNonNull(session, "Session is required");
        }
    }
}

package com.smart.therapy.flow.unit.billing;

import com.smart.therapy.flow.billing.service.BillingGuard;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.session.entity.Session;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class BillingGuardSessionEligibilityTest {

    private final BillingGuard guard = new BillingGuard(
            mock(com.smart.therapy.flow.common.security.CaseloadScopeService.class));

    @Test
    void completedPastSessionIsEligible() {
        Session session = session("completed", Instant.now().minusSeconds(1));

        assertThatCode(() -> guard.requireSessionForBilling(session)).doesNotThrowAnyException();
    }

    @Test
    void noShowPastSessionIsEligible() {
        Session session = session("no-show", Instant.now().minusSeconds(1));

        assertThatCode(() -> guard.requireSessionForBilling(session)).doesNotThrowAnyException();
    }

    @Test
    void scheduledSessionIsNotEligible() {
        Session session = session("scheduled", Instant.now().minusSeconds(1));

        assertThatThrownBy(() -> guard.requireSessionForBilling(session))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("completed or no-show");
    }

    @Test
    void futureOutcomeIsNotEligible() {
        Session session = session("completed", Instant.now().plusSeconds(60));

        assertThatThrownBy(() -> guard.requireSessionForBilling(session))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("scheduled time");
    }

    private static Session session(String status, Instant scheduledAt) {
        Session session = Session.builder()
                .status(status)
                .sessionDate(scheduledAt)
                .build();
        session.setId(1L);
        return session;
    }
}

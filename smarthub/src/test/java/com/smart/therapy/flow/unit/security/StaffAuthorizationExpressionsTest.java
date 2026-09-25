package com.smart.therapy.flow.unit.security;

import com.smart.therapy.flow.common.security.StaffAuthorizationExpressions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StaffAuthorizationExpressionsTest {

    @Test
    void billingReadAllowsCustomStaffWithPermission() {
        assertThat(StaffAuthorizationExpressions.BILLING_READ)
                .contains("hasAnyRole('BILLING_SPECIALIST','ADMIN','SUPERVISOR','THERAPIST')")
                .contains("hasAuthority('BILLING_VIEW')")
                .contains("hasAnyAuthority('BILLING_VIEW','BILLING_MANAGE')")
                .contains("!hasAnyRole('SUPER_ADMIN','ADMIN','THERAPIST','SUPERVISOR','CLIENT')");
    }

    @Test
    void clientReadAllowsCustomStaffWithAnyClientViewPermission() {
        assertThat(StaffAuthorizationExpressions.CLIENT_READ)
                .contains(StaffAuthorizationExpressions.CLIENT_VIEW_ANY);
    }
}

package com.smart.therapy.flow.unit.audit;

import com.smart.therapy.flow.audit.dto.AuditLogResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogResponseTest {

    @Test
    void builderExposesClientMrnNotPatientName() {
        AuditLogResponse response = AuditLogResponse.builder()
                .clientId(42L)
                .clientMrn("CL-2026-0001")
                .build();

        assertThat(response.getClientMrn()).isEqualTo("CL-2026-0001");
        assertThat(response.getClientId()).isEqualTo(42L);
    }
}

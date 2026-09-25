package com.smart.therapy.flow.security;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.common.logging.SensitiveDataMasker;
import com.smart.therapy.flow.common.metrics.AuthAbuseMetrics;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

/** Shared exception-handler dependencies; controller authorization remains real. */
@Import({SensitiveDataMasker.class, com.smart.therapy.flow.common.exception.GlobalExceptionHandler.class})
abstract class BaseControllerContractTest {
    @MockBean protected AuditLogService auditLogService;
    @MockBean protected AuthAbuseMetrics authAbuseMetrics;
}

package com.smart.therapy.flow.common.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.common.service.GlobalActivityAuditService;
import com.smart.therapy.flow.common.tenant.TenantContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class LoggingAspectsPhiSafetyTest {

    private final SensitiveDataMasker masker = new SensitiveDataMasker(new ObjectMapper());

    @AfterEach
    void cleanUp() {
        RequestContextHolder.resetRequestAttributes();
        TenantContext.clear();
    }

    @Test
    void auditDetailsContainIdentifiersButNotClinicalDtoFields() throws Throwable {
        GlobalActivityAuditService service = mock(GlobalActivityAuditService.class);
        GlobalActivityAuditAspect aspect = new GlobalActivityAuditAspect(service, masker);
        ProceedingJoinPoint joinPoint = joinPoint(new ClinicalDto(
                42L, "Alice Patient", "1990-01-02", "alice@example.test", "+1-555-0100",
                "PTSD", "nightmares", "private session notes", "raw therapy transcript",
                "reset-token-value"));
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok("response transcript"));
        setRequest("POST", "/api/v1/clients/42/notes");

        aspect.auditAllControllerActions(joinPoint);

        ArgumentCaptor<String> details = ArgumentCaptor.forClass(String.class);
        verify(service).recordPlatformApiActivity(
                any(), anyString(), anyString(), anyString(), anyInt(), details.capture(), anyLong());
        assertThat(details.getValue()).contains("ClinicalDto", "id=42")
                .doesNotContain(
                        "Alice Patient", "1990-01-02", "alice@example.test", "+1-555-0100",
                        "PTSD", "nightmares", "private session notes", "raw therapy transcript",
                        "reset-token-value", "response transcript");
    }

    @Test
    void auditAndMethodErrorsRecordTypeWithoutExceptionMessage(CapturedOutput output) throws Throwable {
        String clinicalMessage = "diagnosis PTSD in private session notes";
        GlobalActivityAuditService service = mock(GlobalActivityAuditService.class);
        GlobalActivityAuditAspect auditAspect = new GlobalActivityAuditAspect(service, masker);
        ProceedingJoinPoint auditJoinPoint = joinPoint(new ClinicalDto(
                42L, "Alice Patient", "1990-01-02", "alice@example.test", "+1-555-0100",
                "PTSD", "nightmares", "private session notes", "raw therapy transcript",
                "reset-token-value"));
        when(auditJoinPoint.proceed()).thenThrow(new IllegalStateException(clinicalMessage));
        setRequest("POST", "/api/v1/clients/42/notes");

        assertThatThrownBy(() -> auditAspect.auditAllControllerActions(auditJoinPoint))
                .isInstanceOf(IllegalStateException.class);

        ArgumentCaptor<String> details = ArgumentCaptor.forClass(String.class);
        verify(service).recordPlatformApiActivity(
                any(), anyString(), anyString(), anyString(), anyInt(), details.capture(), anyLong());
        assertThat(new ObjectMapper().readTree(details.getValue()).path("error").path("type").asText())
                .isEqualTo("IllegalStateException");
        assertThat(details.getValue()).doesNotContain(clinicalMessage);

        ProceedingJoinPoint methodJoinPoint = joinPoint();
        when(methodJoinPoint.proceed()).thenThrow(new IllegalArgumentException(clinicalMessage));
        MethodLoggingAspect methodAspect = new MethodLoggingAspect(masker);
        assertThatThrownBy(() -> methodAspect.logMethodExecution(methodJoinPoint))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(output.getOut()).contains("errorType=IllegalArgumentException")
                .doesNotContain(clinicalMessage);
    }

    private ProceedingJoinPoint joinPoint(Object... args) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.smart.therapy.flow.client.controller.ClientController");
        when(signature.getName()).thenReturn("update");
        return joinPoint;
    }

    private void setRequest(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private record ClinicalDto(
            Long id,
            String name,
            String dob,
            String email,
            String phone,
            String diagnosis,
            String symptoms,
            String notes,
            String transcript,
            String resetToken) {
    }
}

package com.smart.therapy.flow.common.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataMaskerTest {

    private SensitiveDataMasker masker;

    @BeforeEach
    void setUp() {
        masker = new SensitiveDataMasker(new ObjectMapper());
    }

    @Test
    void argumentAndResultSummariesExposeOnlySafeMetadata() {
        ClinicalDto clinical = clinicalDto();

        String arguments = masker.summarizeArguments(new Object[]{clinical, "raw therapy transcript"}, 2000);
        String result = masker.summarizeResult(ResponseEntity.ok(clinical), 2000);

        assertThat(arguments).contains("ClinicalDto", "id=42", "String{length=22}");
        assertThat(result).contains("status=200 OK", "ClinicalDto", "id=42");
        assertNoPhi(arguments + result);
    }

    @Test
    void bodyRedactionCoversPhiAndCredentials() {
        String body = """
                {"name":"Alice Patient","dob":"1990-01-02","email":"alice@example.test",
                 "phone":"+1-555-0100","diagnosis":"PTSD","symptoms":"nightmares",
                 "notes":"private session notes","transcript":"raw therapy transcript",
                 "password":"SecretPass!","resetToken":"reset-token-value"}
                """;

        String sanitized = masker.sanitizeBody(body, "application/json", 4000);

        assertThat(sanitized).contains("\"name\":\"***\"", "\"resetToken\":\"***\"");
        assertNoPhi(sanitized);
    }

    @Test
    void bodyRedactionMasksFullNameClientNameAndClientMrn() {
        String body = """
                {"fullName":"Alice Patient","clientName":"Alice Patient","clientMrn":"CL-2026-0001",
                 "client_mrn":"CL-2026-0002","id":42}
                """;

        String sanitized = masker.sanitizeBody(body, "application/json", 4000);

        assertThat(sanitized)
                .contains("\"fullName\":\"***\"", "\"clientName\":\"***\"", "\"clientMrn\":\"***\"", "\"client_mrn\":\"***\"")
                .contains("\"id\":42")
                .doesNotContain("Alice Patient", "CL-2026-0001", "CL-2026-0002");
    }

    @Test
    void queryAndHeadersRetainCorrelationButNeverTokensOrArbitraryValues() {
        String query = masker.sanitizeQueryString(
                "clientId=42&page=1&resetToken=reset-token-value&access_token=access-token-value&ticket=ws-ticket-value"
                        + "&email=alice%40example.test&search=Alice%20Patient");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token-value");
        request.addHeader("X-Correlation-ID", "request-123");
        request.addHeader("X-Clinical-Note", "private session notes");

        String headers = masker.maskHeaders(request);

        assertThat(query).contains("clientId=42", "page=1", "resetToken=***", "access_token=***", "ticket=***")
                .contains("email=***", "search=<omitted>");
        assertThat(headers).contains("\"Authorization\":\"***\"", "\"X-Correlation-ID\":\"request-123\"")
                .doesNotContain("X-Clinical-Note");
        assertNoPhi(query + headers);
    }

    @Test
    void maskFieldValueMasksSensitiveFieldNamesRegardlessOfValueShape() {
        assertThat(masker.maskFieldValue("dateOfBirth", "1990-01-02")).isEqualTo("***");
        assertThat(masker.maskFieldValue("fullName", "Alice Patient")).isEqualTo("***");
        assertThat(masker.isSensitiveFieldName("email")).isTrue();
        assertThat(masker.isSensitiveFieldName("capacity")).isFalse();
    }

    @Test
    void maskFieldValueMasksContactLikeValuesEvenWithSafeFieldNames() {
        assertThat(masker.maskFieldValue("someUnrelatedField", "alice@example.test")).isEqualTo("***");
        assertThat(masker.maskFieldValue("someUnrelatedField", "+1-555-0100")).isEqualTo("***");
    }

    @Test
    void maskFieldValuePassesThroughSafeNonPhiValues() {
        assertThat(masker.maskFieldValue("capacity", 42)).isEqualTo(42);
        assertThat(masker.maskFieldValue("status", "ACTIVE")).isEqualTo("ACTIVE");
        assertThat(masker.maskFieldValue("someField", null)).isNull();
    }

    @Test
    void unlabelledStringsAndUnknownFieldsNeverReachLogsOrAudit() throws Exception {
        Object[] args = {"PTSD", "SecretPass!", "reset-token-value",
                java.util.Map.of("unexpectedClinicalField", "raw therapy transcript", "id", 42L),
                java.util.List.of("private session notes"), ResponseEntity.ok("nightmares")};
        String arguments = masker.summarizeArguments(args, 4000);
        String audit = masker.createAuditDetails(args, new IllegalStateException("Alice Patient"), 4000);
        String result = masker.summarizeResult(ResponseEntity.ok("reset-token-value"), 4000);
        assertNoPhi(arguments + audit + result);
        assertThat(arguments).contains("id=42", "String{length=4}");
        var error = new ObjectMapper().readTree(audit).path("error");
        assertThat(error.path("type").asText()).isEqualTo("IllegalStateException");
        assertThat(error.has("message")).isFalse();
    }

    @Test
    void summariesDoNotSerializeUnknownGettersOrConsumeDownloadStreams() {
        Object clinical = new Object() {
            public String getUnexpectedPayload() { throw new AssertionError("Must not serialize payload"); }
            @Override public String toString() { throw new AssertionError("Must not stringify payload"); }
        };
        var stream = new java.io.InputStream() {
            @Override public int read() { throw new AssertionError("Must not read document"); }
        };
        var document = ResponseEntity.ok(new org.springframework.core.io.InputStreamResource(stream));
        assertThat(masker.summarizeArguments(new Object[]{clinical, document}, 2000)).contains("Resource{...}");
        assertThat(masker.summarizeResult(document, 2000)).contains("Resource{...}");
        assertThat(masker.createAuditDetails(new Object[]{clinical}, null, 2000)).contains("args");
    }

    private ClinicalDto clinicalDto() {
        return new ClinicalDto(42L, "Alice Patient", "1990-01-02", "alice@example.test",
                "+1-555-0100", "PTSD", "nightmares", "private session notes",
                "raw therapy transcript", "SecretPass!", "reset-token-value");
    }

    private void assertNoPhi(String text) {
        assertThat(text).doesNotContain(
                "Alice Patient", "1990-01-02", "alice@example.test", "+1-555-0100",
                "PTSD", "nightmares", "private session notes", "raw therapy transcript",
                "SecretPass!", "reset-token-value", "access-token-value");
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
            String password,
            String resetToken) {
    }
}

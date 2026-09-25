package com.smart.therapy.flow.unit.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.common.exception.ErrorResponse;
import com.smart.therapy.flow.common.exception.GlobalExceptionHandler;
import com.smart.therapy.flow.common.logging.SensitiveDataMasker;
import com.smart.therapy.flow.session.dto.RoomRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerRoomJsonTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler(
                    mock(AuditLogService.class),
                    new SensitiveDataMasker(objectMapper),
                    mock(com.smart.therapy.flow.common.metrics.AuthAbuseMetrics.class));

    @Test
    void shouldReturnFriendlyMessageForCapacityOutOfRange() throws Exception {
        String json = """
                {
                  "roomNumber": "101",
                  "roomName": "Room 101",
                  "capacity": 3.332333334443332e+119,
                  "isActive": true,
                  "roomType": "PHYSICAL"
                }
                """;
        Exception mappingException;
        try {
            objectMapper.readValue(json, RoomRequest.class);
            throw new AssertionError("Expected JSON mapping failure");
        } catch (Exception ex) {
            mappingException = ex;
        }
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error",
                mappingException,
                new MockHttpInputMessage(json.getBytes()));

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/rooms");

        ResponseEntity<ErrorResponse> response = handler.handleMalformedJson(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Capacity must be a whole number between 1 and 1000.");
        assertThat(response.getBody().getDetails()).containsKey("errors");
    }

    @Test
    void shouldMaskRejectedValueWhenFieldNameIsSensitive() throws Exception {
        JsonParser parser = objectMapper.createParser("\"1990-01-02\"");
        parser.nextToken();
        InvalidFormatException mappingException = InvalidFormatException.from(
                parser, "Cannot deserialize value", "1990-01-02", String.class);
        mappingException.prependPath(RoomRequest.class, "dateOfBirth");

        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error", mappingException, new MockHttpInputMessage("{}".getBytes()));

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/clients");

        ResponseEntity<ErrorResponse> response = handler.handleMalformedJson(ex, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetails()).containsEntry("rejectedValue", "***");
        assertThat(response.getBody().getMessage()).doesNotContain("1990-01-02");
    }

    @Test
    void shouldMaskRejectedValueWhenItLooksLikeContactInfoRegardlessOfFieldName() throws Exception {
        JsonParser parser = objectMapper.createParser("\"alice@example.test\"");
        parser.nextToken();
        InvalidFormatException mappingException = InvalidFormatException.from(
                parser, "Cannot deserialize value", "alice@example.test", String.class);
        mappingException.prependPath(RoomRequest.class, "someUnrelatedField");

        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error", mappingException, new MockHttpInputMessage("{}".getBytes()));

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/clients");

        ResponseEntity<ErrorResponse> response = handler.handleMalformedJson(ex, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetails()).containsEntry("rejectedValue", "***");
        assertThat(response.getBody().getMessage()).doesNotContain("alice@example.test");
    }
}

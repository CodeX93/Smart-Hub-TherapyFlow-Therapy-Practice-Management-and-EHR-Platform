package com.smart.therapy.flow.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.common.logging.SensitiveDataMasker;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class HttpLoggingFilterTest {
    private final Logger logger = (Logger) LoggerFactory.getLogger(HttpLoggingFilter.class);
    private final ListAppender<ILoggingEvent> events = new ListAppender<>();
    private Level previousLevel;

    @BeforeEach
    void captureFilterEvents() {
        previousLevel = logger.getLevel();
        events.start();
        logger.addAppender(events);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void restoreLogger() {
        logger.detachAppender(events);
        events.stop();
        logger.setLevel(previousLevel);
    }


    @Test
    void logsMetadataAndSanitizedQueryWithoutRequestOrResponseBodies() throws Exception {
        HttpLoggingFilter filter = new HttpLoggingFilter(new SensitiveDataMasker(new ObjectMapper()));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/clients/42/notes");
        request.setQueryString("clientId=42&resetToken=reset-token-value&search=Alice%20Patient");
        request.setContentType("application/json");
        request.setContent("""
                {"name":"Alice Patient","dob":"1990-01-02","email":"alice@example.test",
                 "phone":"+1-555-0100","diagnosis":"PTSD","symptoms":"nightmares",
                 "notes":"private session notes","transcript":"raw therapy transcript"}
                """.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, rawResponse) -> {
            rawResponse.setContentType("application/json");
            rawResponse.getWriter().write(
                    "{\"notes\":\"private response notes\",\"resetToken\":\"response-reset-token\"}");
        });

        assertThat(events.list.stream().map(ILoggingEvent::getFormattedMessage)
                .collect(java.util.stream.Collectors.joining("\n")))
                .contains("method=POST", "path=/api/v1/clients/42/notes", "clientId=42", "resetToken=***")
                .doesNotContain(
                        "reset-token-value", "Alice Patient", "1990-01-02", "alice@example.test",
                        "+1-555-0100", "PTSD", "nightmares", "private session notes",
                        "raw therapy transcript", "private response notes", "response-reset-token");
    }
}

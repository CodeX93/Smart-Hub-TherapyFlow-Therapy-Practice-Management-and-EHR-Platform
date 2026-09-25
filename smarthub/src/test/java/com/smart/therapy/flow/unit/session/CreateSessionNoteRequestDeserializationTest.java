package com.smart.therapy.flow.unit.session;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smart.therapy.flow.session.dto.CreateSessionNoteRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CreateSessionNoteRequestDeserializationTest {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .addModule(new JavaTimeModule())
            .build();

    @Test
    void shouldDeserializeDraftContentHtml() throws Exception {
        String json = """
                {
                  "sessionId": 198,
                  "clientId": 10,
                  "therapistId": 6,
                  "date": "2026-07-08T15:40:00.000Z",
                  "sessionFocus": "focus",
                  "isDraft": true,
                  "isFinalized": false,
                  "draftContent": "<p>Client Information<br>Name: Fahad Jameel</p>",
                  "aiEnabled": false
                }
                """;

        CreateSessionNoteRequest request = objectMapper.readValue(json, CreateSessionNoteRequest.class);

        assertThat(request.getSessionId()).isEqualTo(198L);
        assertThat(request.getDraftContent()).contains("Fahad Jameel");
        assertThat(request.getDate()).isEqualTo(Instant.parse("2026-07-08T15:40:00.000Z"));
    }

    @Test
    void shouldDeserializeGeneratedContent() throws Exception {
        String json = """
                {
                  "sessionId": 1,
                  "clientId": 2,
                  "therapistId": 3,
                  "date": "2026-07-08T15:40:00.000Z",
                  "generatedContent": "<p>Generated note</p>"
                }
                """;

        CreateSessionNoteRequest request = objectMapper.readValue(json, CreateSessionNoteRequest.class);

        assertThat(request.getGeneratedContent()).isEqualTo("<p>Generated note</p>");
    }
}

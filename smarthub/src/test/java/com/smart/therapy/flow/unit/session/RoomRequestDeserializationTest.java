package com.smart.therapy.flow.unit.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.smart.therapy.flow.session.dto.RoomRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomRequestDeserializationTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void shouldFailDeserializingHugeCapacity() {
        String json = """
                {
                  "roomNumber": "h7h7h7hh7h7h7hh7h7h7hh7h7h7hh7h7h7hh7h7h7hh7h7h7hh",
                  "roomName": "h7h7h7hh7h7h7hh7h7h7hh7h7h7hh7h7h7hh7h7h7hh7h7h7hh",
                  "capacity": 3.332333334443332e+119,
                  "equipment": "333233333444",
                  "isActive": true,
                  "roomType": "PHYSICAL"
                }
                """;

        assertThatThrownBy(() -> objectMapper.readValue(json, RoomRequest.class))
                .satisfies(ex -> {
                    Throwable current = ex;
                    while (current != null) {
                        System.out.println(current.getClass().getName() + ": " + current.getMessage());
                        current = current.getCause();
                    }
                });
    }
}

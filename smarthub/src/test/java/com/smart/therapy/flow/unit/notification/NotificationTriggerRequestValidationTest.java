package com.smart.therapy.flow.unit.notification;

import com.smart.therapy.flow.notification.dto.NotificationTriggerRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTriggerRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldRejectNameLongerThan255Characters() {
        NotificationTriggerRequest request = validRequest();
        request.setName("x".repeat(256));

        Set<String> messages = validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertThat(messages).anyMatch(message -> message.contains("Name cannot exceed 255 characters"));
    }

    @Test
    void shouldRejectDescriptionLongerThan10000Characters() {
        NotificationTriggerRequest request = validRequest();
        request.setDescription("x".repeat(10001));

        Set<String> messages = validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertThat(messages).anyMatch(message -> message.contains("Description cannot exceed 10000 characters"));
    }

    @Test
    void shouldRejectInvalidPriority() {
        NotificationTriggerRequest request = validRequest();
        request.setPriority("CRITICAL");

        Set<String> messages = validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertThat(messages).anyMatch(message -> message.contains("Invalid priority"));
    }

    private NotificationTriggerRequest validRequest() {
        NotificationTriggerRequest request = new NotificationTriggerRequest();
        request.setName("Trigger name");
        request.setEventType("session_scheduled");
        request.setPriority("HIGH");
        return request;
    }
}

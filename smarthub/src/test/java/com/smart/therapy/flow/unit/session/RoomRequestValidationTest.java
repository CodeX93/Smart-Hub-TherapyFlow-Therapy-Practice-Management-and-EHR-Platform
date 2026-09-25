package com.smart.therapy.flow.unit.session;

import com.smart.therapy.flow.session.dto.RoomRequest;
import com.smart.therapy.flow.session.enums.RoomType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RoomRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldRejectRoomNumberLongerThan50Characters() {
        RoomRequest request = validRequest();
        request.setRoomNumber("h".repeat(51));

        Set<String> messages = validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertThat(messages).anyMatch(message -> message.contains("Room number cannot exceed 50 characters"));
    }

    @Test
    void shouldRejectEquipmentLongerThan1000Characters() {
        RoomRequest request = validRequest();
        request.setEquipment("x".repeat(1001));

        Set<String> messages = validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertThat(messages).anyMatch(message -> message.contains("Equipment description cannot exceed 1000 characters"));
    }

    @Test
    void shouldRejectCapacityGreaterThan1000() {
        RoomRequest request = validRequest();
        request.setCapacity(1001);

        Set<String> messages = validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertThat(messages).anyMatch(message -> message.contains("Capacity cannot exceed 1000"));
    }

    private RoomRequest validRequest() {
        RoomRequest request = new RoomRequest();
        request.setRoomNumber("101");
        request.setRoomName("Therapy Room 101");
        request.setCapacity(4);
        request.setIsActive(true);
        request.setRoomType(RoomType.PHYSICAL);
        return request;
    }
}

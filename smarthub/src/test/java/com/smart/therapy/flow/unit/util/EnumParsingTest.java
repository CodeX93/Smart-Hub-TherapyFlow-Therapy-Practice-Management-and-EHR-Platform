package com.smart.therapy.flow.unit.util;

import com.smart.therapy.flow.client.dto.CreateClientRequest;
import com.smart.therapy.flow.client.dto.UpdateClientRequest;
import com.smart.therapy.flow.client.enums.EducationLevel;
import com.smart.therapy.flow.client.enums.EmploymentStatus;
import com.smart.therapy.flow.common.util.EnumParsing;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnumParsingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldParseEmploymentStatusByConstantName() {
        assertThat(EmploymentStatus.fromValue("SELF_EMPLOYED")).isEqualTo(EmploymentStatus.SELF_EMPLOYED);
        assertThat(EmploymentStatus.fromValue("Self-Employed")).isEqualTo(EmploymentStatus.SELF_EMPLOYED);
    }

    @Test
    void shouldParseEducationLevelByConstantName() {
        assertThat(EducationLevel.fromValue("SOME_COLLEGE")).isEqualTo(EducationLevel.SOME_COLLEGE);
        assertThat(EducationLevel.fromValue("Some College")).isEqualTo(EducationLevel.SOME_COLLEGE);
    }

    @Test
    void shouldDeserializeUpdateClientRequestWithEnumConstantsAndDependentsAlias() throws Exception {
        UpdateClientRequest request = objectMapper.readValue(
                "{\"employmentStatus\":\"SELF_EMPLOYED\",\"educationLevel\":\"SOME_COLLEGE\",\"numberOfDependents\":32}",
                UpdateClientRequest.class);

        assertThat(request.getEmploymentStatus()).isEqualTo("SELF_EMPLOYED");
        assertThat(request.getEducationLevel()).isEqualTo("SOME_COLLEGE");
        assertThat(request.getDependents()).isEqualTo(32);
    }

    @Test
    void shouldDeserializeNeedsFollowUpOnUpdateClientRequest() throws Exception {
        ObjectMapper lenientMapper = JsonMapper.builder()
                .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
                .addModule(new JavaTimeModule())
                .build();
        UpdateClientRequest request = lenientMapper.readValue(
                "{\"notes\":\"wdwwdwdwd\\n\\nefefefefefefef\\n\\nwdwwdwdwd\","
                        + "\"needsFollowUp\":true,\"priority\":\"low\","
                        + "\"dueDate\":\"2026-06-19\",\"followUpNotes\":\"efefefefefefef\"}",
                UpdateClientRequest.class);

        assertThat(request.getNeedsFollowUp()).isTrue();
        assertThat(request.getPriority()).isEqualTo("low");
        assertThat(request.getFollowUpDate()).hasToString("2026-06-19");
        assertThat(request.getFollowUpNotes()).isEqualTo("efefefefefefef");
    }

    @Test
    void shouldRejectInvalidInsurancePhoneOnUpdateClientRequest() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        UpdateClientRequest request = new UpdateClientRequest();
        request.setInsurancePhone("dwdwdwdwd");

        Set<ConstraintViolation<UpdateClientRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> "insurancePhone".equals(v.getPropertyPath().toString())
                        && v.getMessage().contains("Insurance phone"));
    }

    @Test
    void shouldRejectInvalidInsurancePhoneOnCreateClientRequest() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        CreateClientRequest request = new CreateClientRequest();
        request.setFullName("Jane Doe");
        request.setStatus("active");
        request.setInsurancePhone("dwdwdwdwd");

        Set<ConstraintViolation<CreateClientRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(v -> "insurancePhone".equals(v.getPropertyPath().toString())
                        && v.getMessage().contains("Insurance phone"));
    }

    @Test
    void shouldDeserializeNeedsFollowUpOnCreateClientRequest() throws Exception {
        ObjectMapper lenientMapper = JsonMapper.builder()
                .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
                .addModule(new JavaTimeModule())
                .build();
        CreateClientRequest request = lenientMapper.readValue(
                "{\"fullName\":\"Jane Doe\",\"status\":\"active\",\"needsFollowUp\":true,"
                        + "\"priority\":\"low\",\"dueDate\":\"2026-06-19\"}",
                CreateClientRequest.class);

        assertThat(request.getNeedsFollowUp()).isTrue();
        assertThat(request.getPriority()).isEqualTo("low");
        assertThat(request.getFollowUpDate()).hasToString("2026-06-19");
    }

    @Test
    void shouldIncludeAllowedValuesInEmploymentStatusError() {
        assertThatThrownBy(() -> EmploymentStatus.fromValue("INVALID_STATUS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid employment status")
                .hasMessageContaining("SELF_EMPLOYED");
    }
}

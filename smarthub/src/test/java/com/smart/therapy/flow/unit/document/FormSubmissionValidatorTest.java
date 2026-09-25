package com.smart.therapy.flow.unit.document;

import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.document.entity.*;
import com.smart.therapy.flow.document.service.FormSubmissionValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class FormSubmissionValidatorTest {
    @ParameterizedTest
    @ValueSource(strings = {"Client Full Name", "Date", "Signatures"})
    void portalSignatureMetadataDoesNotNeedSeparateAnswer(String label) {
        var field = field(1L, true);
        field.setFieldLabel(label);
        assertThatCode(() -> FormSubmissionValidator.validateAnswers(List.of(field), List.of(), true))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> FormSubmissionValidator.validateAnswers(List.of(field), List.of(), false))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Signature required");
    }

    @Test
    void oldVersionFlagTakesPrecedenceWithLegacyNullFallback() {
        var template = FormTemplate.builder().requiresSignature(true).build();
        var version = FormTemplateVersion.builder().template(template).requiresSignature(false).build();
        var assignment = FormAssignment.builder().templateVersion(version).build();
        assertThatCode(() -> FormSubmissionValidator.validateSubmission(assignment, List.of(), List.of(), List.of()))
                .doesNotThrowAnyException();
        version.setRequiresSignature(null);
        assertThatThrownBy(() -> FormSubmissionValidator.validateSubmission(assignment, List.of(), List.of(), List.of()))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Signature required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid json", "{\"unknown\":true}",
            "{\"showIf\":{\"fieldId\":999,\"value\":\"yes\"}}"})
    void brokenConditionCannotSilentlySkipRequiredAnswer(String condition) {
        var field = field(1L, true);
        field.setConditionalDisplay(condition);
        assertThatThrownBy(() -> FormSubmissionValidator.validateAnswers(List.of(field), List.of(), true))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("conditional rule");
    }

    @Test
    void cyclicConditionFailsWithoutRecursingIndefinitely() {
        var field = field(1L, true);
        field.setConditionalDisplay("{\"showIf\":{\"fieldId\":1,\"value\":\"yes\"}}");
        assertThatThrownBy(() -> FormSubmissionValidator.validateAnswers(List.of(field), List.of(), true))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("conditional rule");
    }

    @Test
    void deletedAnswerDoesNotSatisfyRequiredField() {
        var field = field(1L, true);
        var answer = FormResponse.builder().assignmentField(field).responseValue("Old answer").build();
        answer.setIsDeleted(true);
        assertThatThrownBy(() -> FormSubmissionValidator.validateAnswers(List.of(field), List.of(answer), true))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("required field");
        field.setIsDeleted(true);
        assertThatCode(() -> FormSubmissionValidator.validateAnswers(List.of(field), List.of(), true))
                .doesNotThrowAnyException();
    }

    private FormAssignmentField field(Long id, boolean required) {
        var original = new FormField();
        original.setId(id);
        var field = FormAssignmentField.builder().field(original).fieldType("TEXT")
                .fieldLabel("Required answer").isRequired(required).build();
        field.setId(id);
        return field;
    }
}

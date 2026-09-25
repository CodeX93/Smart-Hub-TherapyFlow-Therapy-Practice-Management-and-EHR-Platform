package com.smart.therapy.flow.document.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.document.entity.*;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Completion checks use the assigned snapshots; draft persistence deliberately does not call these. */
public final class FormSubmissionValidator {
    private static final ObjectMapper JSON = new ObjectMapper();

    private FormSubmissionValidator() { }

    public static void requireAcceptedTerms(Boolean agreed) {
        if (!Boolean.TRUE.equals(agreed)) {
            throw new BadRequestException("Please agree to the terms before signing");
        }
    }

    public static void validateSubmission(FormAssignment assignment, List<FormAssignmentField> fields,
            List<FormResponse> responses, List<FormSignature> signatures) {
        var activeSignatures = signatures.stream().filter(s -> !Boolean.TRUE.equals(s.getIsDeleted())).toList();
        for (FormSignature signature : activeSignatures) {
            requireAcceptedTerms(signature.getAgreedToTerms());
        }
        boolean signed = activeSignatures.stream().anyMatch(s -> StringUtils.hasText(s.getSignatureData()));
        var version = assignment.getTemplateVersion();
        Boolean required = version != null ? version.getRequiresSignature() : null;
        if (required == null && version != null && version.getTemplate() != null) {
            required = version.getTemplate().getRequiresSignature();
        }
        if (Boolean.TRUE.equals(required) && !signed) {
            throw new BadRequestException("Signature required before submission");
        }
        validateAnswers(fields, responses, signed);
    }

    public static void validateAnswers(List<FormAssignmentField> fields, List<FormResponse> responses,
            boolean signed) {
        Map<Long, String> values = new HashMap<>();
        for (FormResponse response : responses) {
            if (!Boolean.TRUE.equals(response.getIsDeleted()) && response.getAssignmentField() != null) {
                values.put(response.getAssignmentField().getId(), response.getResponseValue());
            }
        }
        List<FormAssignmentField> activeFields = fields.stream()
                .filter(f -> !Boolean.TRUE.equals(f.getIsDeleted())).toList();
        for (FormAssignmentField field : activeFields) {
            if (!Boolean.TRUE.equals(field.getIsRequired())) continue;
            String type = field.getFieldType() == null ? "" : field.getFieldType().toUpperCase(Locale.ROOT);
            if ("HEADING".equals(type) || "INFO_TEXT".equals(type)) continue;
            if (!visible(field, activeFields, values, new HashSet<>())) continue;
            // Existing portal consent rendering groups these labels with the electronic signature.
            String label = field.getFieldLabel() == null ? "" : field.getFieldLabel().trim().toLowerCase(Locale.ROOT);
            boolean signatureField = "SIGNATURE".equals(type)
                    || Set.of("client full name", "date", "signatures").contains(label);
            if (signatureField) {
                if (!signed) throw new BadRequestException("Signature required before submission");
            } else if (!StringUtils.hasText(values.get(field.getId()))) {
                throw new BadRequestException("Please complete every required field before submission");
            }
        }
    }

    /** Supported template contract: {"showIf":{"fieldId": originalFieldId,"value":"yes"}}. */
    private static boolean visible(FormAssignmentField field, List<FormAssignmentField> fields,
            Map<Long, String> values, Set<Long> visiting) {
        if (!StringUtils.hasText(field.getConditionalDisplay())) return true;
        if (!visiting.add(field.getId())) throw invalidCondition();
        try {
            JsonNode condition = JSON.readTree(field.getConditionalDisplay());
            if (condition == null || condition.isNull() || (condition.isObject() && condition.isEmpty())) return true;
            JsonNode rule = condition.path("showIf");
            if (!condition.isObject() || condition.size() != 1 || !rule.isObject() || rule.size() != 2
                    || !rule.hasNonNull("fieldId") || !rule.hasNonNull("value") || !rule.get("value").isValueNode()) {
                throw invalidCondition();
            }
            long originalId = rule.get("fieldId").asLong(-1);
            FormAssignmentField controller = fields.stream()
                    .filter(f -> f.getField() != null && Long.valueOf(originalId).equals(f.getField().getId()))
                    .findFirst().orElseThrow(FormSubmissionValidator::invalidCondition);
            return visible(controller, fields, values, visiting)
                    && rule.get("value").asText().equals(values.get(controller.getId()));
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw invalidCondition();
        } finally {
            visiting.remove(field.getId());
        }
    }

    private static BadRequestException invalidCondition() {
        return new BadRequestException("Invalid conditional rule for required field; please correct the assigned form configuration");
    }
}

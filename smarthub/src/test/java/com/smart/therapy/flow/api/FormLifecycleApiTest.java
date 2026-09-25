package com.smart.therapy.flow.api;

import com.smart.therapy.flow.common.BaseTenantApiTest;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.document.repository.*;
import com.smart.therapy.flow.document.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real HTTP/JWT and PostgreSQL. Rejection tests deliberately expose missing lifecycle guards. */
class FormLifecycleApiTest extends BaseTenantApiTest {
    @Autowired private FormAssignmentRepository assignments;
    @Autowired private FormAssignmentFieldRepository fields;
    @Autowired private FormResponseRepository responses;
    @Autowired private FormSignatureRepository signatures;
    @Autowired private com.smart.therapy.flow.auth.repository.RoleRepository roles;
    @Autowired private com.smart.therapy.flow.auth.repository.PermissionRepository permissions;
    @Autowired private com.smart.therapy.flow.auth.repository.RolePermissionRepository rolePermissions;
    @Autowired private FormTemplateVersionRepository versions;
    @Autowired private FormFieldRepository templateFields;
    private String token;
    private Client client;
    private long templateId;
    private long assignmentId;
    private long fieldId;

    @BeforeEach
    void createForm() throws Exception {
        var admin = persistStaff(uniqueEmail("form-admin"), "password123", "ADMIN");
        var role = roles.findByName("ADMIN").orElseThrow();
        var grant = permissions.findByName("FORM_TEMPLATE_MANAGE").orElseGet(() -> permissions.save(
                com.smart.therapy.flow.auth.entity.Permission.builder().name("FORM_TEMPLATE_MANAGE")
                        .displayName("Manage synthetic form templates").category("qa").isActive(true).build()));
        if (rolePermissions.findAll().stream().noneMatch(link -> link.getRole().getId().equals(role.getId())
                && link.getPermission().getId().equals(grant.getId()))) {
            rolePermissions.save(com.smart.therapy.flow.auth.entity.RolePermission.builder()
                    .role(role).permission(grant).build());
        }
        client = persistClient(admin);
        token = getAuthToken(admin.getEmail(), "password123");
        templateId = id(mockMvc.perform(post("/api/v1/forms/templates").headers(createHeaders(token))
                .content("{\"name\":\"Synthetic lifecycle form\",\"category\":\"custom\",\"requiresSignature\":true,\"instructions\":\"Original instructions\",\"fields\":[{\"label\":\"Original required answer\",\"fieldType\":\"text\",\"isRequired\":true}]}"))
                .andExpect(status().isCreated()).andReturn());
        assignmentId = assign();
        enterFixtureTenant();
        fieldId = fields.findByAssignmentIdAndIsDeletedFalseOrderBySortOrderAsc(assignmentId).get(0).getId();
    }

    @Test
    void templateRevisionPreservesExistingAnswersAndFieldSnapshots() throws Exception {
        answer("Original answer");
        enterFixtureTenant();
        var before = responses.findByAssignmentId(assignmentId).get(0);
        long responseId = before.getId();
        var original = mockMvc.perform(get(url()).headers(createHeaders(token)))
                .andExpect(status().isOk()).andReturn();
        long version = objectMapper.readTree(original.getResponse().getContentAsString()).path("templateVersionId").asLong();
        revise(false);
        long newAssignment = assign();
        mockMvc.perform(get(url()).headers(createHeaders(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.templateVersionId").value(version))
                .andExpect(jsonPath("$.instructions").value("Original instructions"));
        enterFixtureTenant();
        var oldField = fields.findByAssignmentIdAndIsDeletedFalseOrderBySortOrderAsc(assignmentId).get(0);
        assertThat(oldField.getId()).isEqualTo(fieldId);
        assertThat(oldField.getFieldLabel()).isEqualTo("Original required answer");
        assertThat(oldField.getIsRequired()).isTrue();
        assertThat(responses.findByAssignmentId(assignmentId)).singleElement().satisfies(saved -> {
            assertThat(saved.getId()).isEqualTo(responseId);
            assertThat(saved.getResponseValue()).isEqualTo("Original answer");
        });
        assertThat(fields.findByAssignmentIdAndIsDeletedFalseOrderBySortOrderAsc(newAssignment))
                .singleElement().satisfies(f -> {
                    assertThat(f.getId()).isNotEqualTo(fieldId);
                    assertThat(f.getFieldLabel()).isEqualTo("Revised optional answer");
                    assertThat(f.getIsRequired()).isFalse();
                });
    }

    @Test
    void foreignAssignmentFieldIsRejectedAndPreviousAnswersSurviveRollback() throws Exception {
        answer("Retain me");
        long other = assign();
        enterFixtureTenant();
        long foreign = fields.findByAssignmentIdAndIsDeletedFalseOrderBySortOrderAsc(other).get(0).getId();
        mockMvc.perform(post(url() + "/responses").headers(createHeaders(token))
                .content(objectMapper.writeValueAsString(Map.of("assignmentId", assignmentId,
                        "responses", Map.of(foreign, "Wrong field")))))
                .andExpect(status().isNotFound());
        enterFixtureTenant();
        assertThat(responses.findByAssignmentId(assignmentId)).singleElement()
                .satisfies(r -> assertThat(r.getResponseValue()).isEqualTo("Retain me"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"signatureData", "signerName", "signerRole"})
    void blankSignatureMetadataIsRejectedWithoutCompletingForm(String key) throws Exception {
        var payload = new java.util.HashMap<String, Object>(signature(true));
        payload.put(key, " ");
        mockMvc.perform(post(url() + "/signature").headers(createHeaders(token))
                .content(objectMapper.writeValueAsString(payload))).andExpect(status().isBadRequest());
        assertUncompleted();
    }

    @Test
    void validSignaturePersistsCompletionAndTimestamps() throws Exception {
        answer("Complete answer");
        mockMvc.perform(post(url() + "/signature").headers(createHeaders(token))
                .content(objectMapper.writeValueAsString(signature(true)))).andExpect(status().isCreated());
        enterFixtureTenant();
        var saved = assignments.findById(assignmentId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(Status.COMPLETED);
        assertThat(saved.getCompletedAt()).isNotNull();
        assertThat(saved.getSubmittedAt()).isNotNull();
        assertThat(signatures.findByAssignmentId(assignmentId)).singleElement()
                .satisfies(s -> assertThat(s.getAgreedToTerms()).isTrue());
    }

    @Test
    void completedAssignmentReviewPersistsReviewerAndNotes() throws Exception {
        // Completion is seeded separately because the signature endpoint itself is under test.
        enterFixtureTenant();
        var completed = assignments.findById(assignmentId).orElseThrow();
        completed.setStatus(Status.COMPLETED);
        completed.setCompletedAt(java.time.Instant.now());
        completed.setSubmittedAt(completed.getCompletedAt());
        assignments.save(completed);
        mockMvc.perform(patch(url() + "/review").headers(createHeaders(token))
                .content(objectMapper.writeValueAsString(Map.of("assignmentId", assignmentId,
                        "reviewNotes", "Reviewed synthetic answer"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reviewedById").isNumber())
                .andExpect(jsonPath("$.reviewedAt").isNotEmpty());
        enterFixtureTenant();
        var saved = assignments.findById(assignmentId).orElseThrow();
        assertThat(saved.getReviewNotes()).isEqualTo("Reviewed synthetic answer");
        assertThat(saved.getReviewedAt()).isNotNull();
    }

    @Test
    void signatureRejectsMissingRequiredAnswer() throws Exception {
        mockMvc.perform(post(url() + "/signature").headers(createHeaders(token))
                .content(objectMapper.writeValueAsString(signature(true)))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsStringIgnoringCase("required field")));
        assertUncompleted();
    }

    @Test
    void signatureRejectsDeclinedTerms() throws Exception {
        answer("Complete answer");
        mockMvc.perform(post(url() + "/signature").headers(createHeaders(token))
                .content(objectMapper.writeValueAsString(signature(false)))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsStringIgnoringCase("terms")));
        assertUncompleted();
    }

    @Test
    void reviewPreservesAssignedStatusAsInClientHub() throws Exception {
        mockMvc.perform(patch(url() + "/review").headers(createHeaders(token))
                .content(objectMapper.writeValueAsString(Map.of("assignmentId", assignmentId, "reviewNotes", "Draft review"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ASSIGNED"));
        enterFixtureTenant();
        var reviewed = assignments.findById(assignmentId).orElseThrow();
        assertThat(reviewed.getReviewedAt()).isNotNull();
        assertThat(reviewed.getReviewNotes()).isEqualTo("Draft review");
        assertThat(reviewed.getStatus()).isEqualTo(Status.ASSIGNED);
        assertThat(reviewed.getCompletedAt()).isNull();
        assertThat(reviewed.getSubmittedAt()).isNull();
    }

    @Test
    void portalRequiresSignatureFromAssignedVersionAfterTemplateRevision() throws Exception {
        answer("Complete answer");
        enterFixtureTenant();
        String portal = portalToken(client);
        mockMvc.perform(post("/api/v1/portal/forms/submit/" + assignmentId).headers(createHeaders(portal)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Signature required before submission"));
        revise(false);
        mockMvc.perform(post("/api/v1/portal/forms/submit/" + assignmentId).headers(createHeaders(portal)))
                .andExpect(status().isBadRequest());
        assertUncompleted();
    }

    @Test
    void blankDraftCanBeSavedButCannotBeSigned() throws Exception {
        answer("   ");
        mockMvc.perform(post(url() + "/signature").headers(createHeaders(token))
                .content(objectMapper.writeValueAsString(signature(true))))
                .andExpect(status().isBadRequest());
        assertUncompleted();
        assertThat(responses.findByAssignmentId(assignmentId)).singleElement()
                .satisfies(r -> assertThat(r.getResponseValue()).isEqualTo("   "));
    }

    @Test
    void portalDraftSignatureDoesNotBypassRequiredAnswers() throws Exception {
        enterFixtureTenant();
        String portal = portalToken(client);
        mockMvc.perform(post("/api/v1/portal/forms/signature").headers(createHeaders(portal))
                .content(objectMapper.writeValueAsString(Map.of("assignmentId", assignmentId,
                        "signatureData", "c3ludGhldGlj", "agreedToTerms", true))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/portal/forms/submit/" + assignmentId).headers(createHeaders(portal)))
                .andExpect(status().isBadRequest());
        enterFixtureTenant();
        assertThat(assignments.findById(assignmentId).orElseThrow().getCompletedAt()).isNull();
        answer("Complete answer");
        mockMvc.perform(post("/api/v1/portal/forms/submit/" + assignmentId).headers(createHeaders(portal)))
                .andExpect(status().isOk());
        enterFixtureTenant();
        assertThat(assignments.findById(assignmentId).orElseThrow().getStatus()).isEqualTo(Status.COMPLETED);
    }

    @Test
    void portalDeclinedTermsDoesNotSaveSignature() throws Exception {
        enterFixtureTenant();
        String portal = portalToken(client);
        mockMvc.perform(post("/api/v1/portal/forms/signature").headers(createHeaders(portal))
                .content(objectMapper.writeValueAsString(Map.of("assignmentId", assignmentId,
                        "signatureData", "c3ludGhldGlj", "agreedToTerms", false))))
                .andExpect(status().isBadRequest());
        assertUncompleted();
    }

    @ParameterizedTest
    @ValueSource(strings = {"HEADING", "INFO_TEXT", "SIGNATURE"})
    void nonAnswerFieldsDoNotRequireSeparateTextResponse(String type) throws Exception {
        enterFixtureTenant();
        var field = fields.findById(fieldId).orElseThrow();
        field.setFieldType(type);
        fields.save(field);
        mockMvc.perform(post(url() + "/signature").headers(createHeaders(token))
                .content(objectMapper.writeValueAsString(signature(true))))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {"yes", "no"})
    void conditionalRequiredAnswerUsesAssignmentSnapshot(String controlValue) throws Exception {
        // Create an independent controller field in the same assignment snapshot.
        enterFixtureTenant();
        var child = fields.findById(fieldId).orElseThrow();
        var original = templateFields.findById(child.getField().getId()).orElseThrow();
        var controllerField = templateFields.save(com.smart.therapy.flow.document.entity.FormField.builder()
                .templateVersion(original.getTemplateVersion()).section(original.getSection())
                .fieldName("conditional_control").fieldLabel("Controller")
                .fieldType(com.smart.therapy.flow.document.enums.FieldType.TEXT).isRequired(false).sortOrder(1).build());
        var controller = fields.save(com.smart.therapy.flow.document.entity.FormAssignmentField.builder()
                .assignment(assignments.findById(assignmentId).orElseThrow())
                .field(controllerField).fieldLabel("Conditional controller").fieldType("TEXT")
                .isRequired(false).sortOrder(1).build());
        child.setConditionalDisplay(objectMapper.writeValueAsString(Map.of("showIf",
                Map.of("fieldId", controllerField.getId(), "value", "yes"))));
        fields.save(child);
        mockMvc.perform(post(url() + "/responses").headers(createHeaders(token))
                .content(objectMapper.writeValueAsString(Map.of("assignmentId", assignmentId,
                        "responses", Map.of(controller.getId(), controlValue)))))
                .andExpect(status().isCreated());
        mockMvc.perform(post(url() + "/signature").headers(createHeaders(token))
                .content(objectMapper.writeValueAsString(signature(true))))
                .andExpect("yes".equals(controlValue) ? status().isBadRequest() : status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {"submitted", "completed", "reviewed"})
    void staffStatusCannotBypassRequiredAnswers(String target) throws Exception {
        enterFixtureTenant();
        var version = versions.findById(assignments.findById(assignmentId).orElseThrow()
                .getTemplateVersion().getId()).orElseThrow();
        version.setRequiresSignature(false);
        versions.save(version);
        mockMvc.perform(patch(url() + "/status").headers(createHeaders(token))
                .content(objectMapper.writeValueAsString(Map.of("status", target))))
                .andExpect(status().isBadRequest());
        assertUncompleted();
        answer("Complete answer");
        mockMvc.perform(patch(url() + "/status").headers(createHeaders(token))
                .content(objectMapper.writeValueAsString(Map.of("status", target))))
                .andExpect(status().isOk());
        enterFixtureTenant();
        assertThat(assignments.findById(assignmentId).orElseThrow().getStatus().name())
                .isEqualTo(target.toUpperCase(java.util.Locale.ROOT));
    }

    @Test
    void staffCompletionRequiresSignatureWhenVersionRequiresIt() throws Exception {
        answer("Complete answer");
        mockMvc.perform(put(url()).headers(createHeaders(token)).content("{\"status\":\"completed\"}"))
                .andExpect(status().isBadRequest());
        assertUncompleted();
    }

    @ParameterizedTest
    @ValueSource(strings = {"staff", "portal"})
    void finalSubmissionRejectsSavedSignatureWithDeclinedTerms(String actor) throws Exception {
        answer("Complete answer");
        enterFixtureTenant();
        String portal = portalToken(client);
        // Existing ClientHub-compatible callers omit agreedToTerms: signing denotes acceptance.
        mockMvc.perform(post("/api/v1/portal/forms/signature").headers(createHeaders(portal))
                .content(objectMapper.writeValueAsString(Map.of("assignmentId", assignmentId,
                        "signatureData", "c3ludGhldGlj")))).andExpect(status().isOk());
        enterFixtureTenant();
        var signature = signatures.findByAssignmentId(assignmentId).get(0);
        assertThat(signature.getAgreedToTerms()).isTrue();
        signature.setAgreedToTerms(false);
        signatures.save(signature);
        var request = "portal".equals(actor)
                ? post("/api/v1/portal/forms/submit/" + assignmentId).headers(createHeaders(portal))
                : patch(url() + "/status").headers(createHeaders(token)).content("{\"status\":\"completed\"}");
        mockMvc.perform(request).andExpect(status().isBadRequest());
        enterFixtureTenant();
        assertThat(assignments.findById(assignmentId).orElseThrow().getCompletedAt()).isNull();
    }

    private void revise(boolean requiresSignature) throws Exception {
        mockMvc.perform(patch("/api/v1/forms/templates/" + templateId).headers(createHeaders(token))
                .content("{\"requiresSignature\":" + requiresSignature + ",\"instructions\":\"Revised instructions\",\"fields\":[{\"label\":\"Revised optional answer\",\"fieldType\":\"text\",\"isRequired\":false}]}"))
                .andExpect(status().isOk());
    }
    private long assign() throws Exception {
        return id(mockMvc.perform(post("/api/v1/forms/assignments").headers(createHeaders(token))
                .content(objectMapper.writeValueAsString(Map.of("templateId", templateId, "clientId", client.getId()))))
                .andExpect(status().isCreated()).andReturn());
    }
    private void answer(String value) throws Exception {
        mockMvc.perform(post(url() + "/responses").headers(createHeaders(token))
                .content(objectMapper.writeValueAsString(Map.of("assignmentId", assignmentId,
                        "responses", Map.of(fieldId, value))))).andExpect(status().isCreated());
    }
    private Map<String, Object> signature(boolean agreed) {
        return Map.of("assignmentId", assignmentId, "signatureData", "c3ludGhldGlj", "signerName", "Synthetic client",
                "signerRole", "client", "agreedToTerms", agreed);
    }
    private void assertUncompleted() {
        enterFixtureTenant();
        var saved = assignments.findById(assignmentId).orElseThrow();
        assertThat(saved.getCompletedAt()).isNull();
        assertThat(saved.getSubmittedAt()).isNull();
        assertThat(signatures.findByAssignmentId(assignmentId)).isEmpty();
    }
    private String url() { return "/api/v1/forms/assignments/" + assignmentId; }
    private long id(MvcResult result) throws Exception {
        long id = objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
        assertThat(id).isPositive();
        return id;
    }
}

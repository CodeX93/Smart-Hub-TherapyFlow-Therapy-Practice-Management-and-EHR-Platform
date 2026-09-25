package com.smart.therapy.flow.api;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.BaseTenantApiTest;
import com.smart.therapy.flow.document.repository.DocumentRepository;
import com.smart.therapy.flow.report.entity.ReportSupportingFile;
import com.smart.therapy.flow.report.repository.ReportSupportingFileRepository;
import com.smart.therapy.flow.report.service.ReportSupportingFileService;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.entity.SessionNote;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.session.repository.SessionNoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real JWT/HTTP and committed PostgreSQL fixtures; local file storage and no outbound delivery. */
class ClinicalDocumentAccessApiTest extends BaseTenantApiTest {
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=");
    private static final String NOTE_TEXT = "Synthetic original clinical text";

    @Autowired private DocumentRepository documents;
    @Autowired private SessionRepository sessions;
    @Autowired private SessionNoteRepository notes;
    @Autowired private ReportSupportingFileRepository supportingFiles;
    @Autowired private ReportSupportingFileService supportingFileService;

    private User therapist;
    private Client client;
    private Client otherClient;
    private String therapistToken;
    private String otherTherapistToken;

    @BeforeEach
    void createClinicalFixtures() {
        therapist = persistStaff(uniqueEmail("clinical-owner"), "password123", "THERAPIST");
        User other = persistStaff(uniqueEmail("clinical-other"), "password123", "THERAPIST");
        client = persistClient(therapist);
        otherClient = persistClient(other);
        therapistToken = getAuthToken(therapist.getEmail(), "password123");
        otherTherapistToken = getAuthToken(other.getEmail(), "password123");
        enterFixtureTenant();
    }

    @ParameterizedTest
    @ValueSource(strings = {"view", "download"})
    void revokingSharingDeniesPreviouslyWorkingPortalUrl(String endpoint) throws Exception {
        Long id = uploadSharedDocument();
        String token = portalToken(client);
        String url = "/api/v1/portal/documents/" + id + "/" + endpoint;
        assertThat(readBytes(get(url).headers(createHeaders(token)))).isEqualTo(PNG);
        enterFixtureTenant();
        int downloadCount = documents.findById(id).orElseThrow().getDownloadCount();
        mockMvc.perform(patch(documentUrl(client.getId(), id) + "/share")
                        .headers(createHeaders(therapistToken)).content("{\"shareWithClient\":false}"))
                .andExpect(status().isOk());
        // Reuse the same valid portal token and exact URL; no logout or new identity is involved.
        mockMvc.perform(get(url).headers(createHeaders(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Document not found or access denied"));
        enterFixtureTenant();
        var saved = documents.findById(id).orElseThrow();
        assertThat(saved.getIsSharedInPortal()).isFalse();
        assertThat(saved.getDownloadCount()).isEqualTo(downloadCount);
        assertThat(readBytes(get(documentUrl(client.getId(), id) + "/download")
                .headers(createHeaders(therapistToken)))).isEqualTo(PNG);
    }

    @ParameterizedTest
    @ValueSource(strings = {"view", "download"})
    void sharingDoesNotAllowAnotherPortalClientToReadDocument(String endpoint) throws Exception {
        Long id = uploadSharedDocument();
        String ownerToken = portalToken(client);
        assertThat(readBytes(get("/api/v1/portal/documents/" + id + "/" + endpoint)
                .headers(createHeaders(ownerToken)))).isEqualTo(PNG);
        enterFixtureTenant();
        String otherToken = portalToken(otherClient);
        mockMvc.perform(get("/api/v1/portal/documents/" + id + "/" + endpoint)
                        .headers(createHeaders(otherToken)))
                .andExpect(status().isUnauthorized());
        enterFixtureTenant();
        assertThat(documents.findById(id).orElseThrow().getIsSharedInPortal()).isTrue();
    }

    @Test
    void therapistCannotReadOrMutateAnotherTherapistsClinicalRecords() throws Exception {
        Long documentId = uploadSharedDocument();
        Long noteId = createDraftNote();
        mockMvc.perform(get("/api/v1/session-notes/" + noteId).headers(createHeaders(therapistToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sessionFocus").value(NOTE_TEXT));
        mockMvc.perform(get(documentUrl(client.getId(), documentId) + "/download")
                        .headers(createHeaders(otherTherapistToken))).andExpect(status().isForbidden());
        mockMvc.perform(patch(documentUrl(client.getId(), documentId) + "/share")
                        .headers(createHeaders(otherTherapistToken)).content("{\"shareWithClient\":false}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/session-notes/" + noteId).headers(createHeaders(otherTherapistToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/session-notes/" + noteId).headers(createHeaders(otherTherapistToken))
                        .content("{\"sessionFocus\":\"Unauthorized replacement\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/session-notes/" + noteId).headers(createHeaders(otherTherapistToken)))
                .andExpect(status().isNotFound());
        enterFixtureTenant();
        assertThat(notes.findById(noteId).orElseThrow().getSessionFocus()).isEqualTo(NOTE_TEXT);
        assertThat(documents.findById(documentId).orElseThrow().getIsSharedInPortal()).isTrue();
    }

    @Test
    void finalizedNoteRejectsEditingAndDeletionWithoutChangingSavedContent() throws Exception {
        Long id = createDraftNote();
        mockMvc.perform(post("/api/v1/session-notes/" + id + "/finalize").headers(createHeaders(therapistToken)))
                .andExpect(status().isOk());
        enterFixtureTenant();
        var original = notes.findById(id).orElseThrow();
        assertThat(original.getIsFinalized()).isTrue();
        assertThat(original.getFinalizedAt()).isNotNull();
        mockMvc.perform(put("/api/v1/session-notes/" + id).headers(createHeaders(therapistToken))
                        .content("{\"sessionFocus\":\"Replacement forbidden after finalization\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot edit a finalized session note"));
        mockMvc.perform(delete("/api/v1/session-notes/" + id).headers(createHeaders(therapistToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot delete a finalized session note"));
        enterFixtureTenant();
        var saved = notes.findById(id).orElseThrow();
        assertThat(saved.getSessionFocus()).isEqualTo(NOTE_TEXT);
        assertThat(saved.getDraftContent()).isEqualTo(NOTE_TEXT);
        assertThat(saved.getIsFinalized()).isTrue();
        assertThat(saved.getFinalizedAt()).isEqualTo(original.getFinalizedAt());
        assertThat(saved.getUpdatedAt()).isEqualTo(original.getUpdatedAt());
    }

    @Test
    void assignedTherapistCanReopenFinalizedSessionNoteMatchingClientHub() throws Exception {
        Long id = createDraftNote();
        mockMvc.perform(post("/api/v1/session-notes/" + id + "/finalize").headers(createHeaders(therapistToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFinalized").value(true));

        mockMvc.perform(post("/api/v1/session-notes/" + id + "/unfinalize").headers(createHeaders(therapistToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isFinalized").value(false))
                .andExpect(jsonPath("$.isDraft").value(true))
                .andExpect(jsonPath("$.draftContent").value(NOTE_TEXT))
                .andExpect(jsonPath("$.finalContent").value(org.hamcrest.Matchers.nullValue()));

        enterFixtureTenant();
        var reopened = notes.findById(id).orElseThrow();
        assertThat(reopened.getIsFinalized()).isFalse();
        assertThat(reopened.getIsDraft()).isTrue();
        assertThat(reopened.getFinalContent()).isNull();
        assertThat(reopened.getFinalizedAt()).isNull();
        assertThat(reopened.getDraftContent()).isEqualTo(NOTE_TEXT);

        mockMvc.perform(put("/api/v1/session-notes/" + id).headers(createHeaders(therapistToken))
                        .content("{\"sessionFocus\":\"Editable after reopen\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionFocus").value("Editable after reopen"));
    }

    @Test
    void otherTherapistCannotReopenFinalizedSessionNote() throws Exception {
        Long id = createDraftNote();
        mockMvc.perform(post("/api/v1/session-notes/" + id + "/finalize").headers(createHeaders(therapistToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/session-notes/" + id + "/unfinalize")
                        .headers(createHeaders(otherTherapistToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You do not have permission to reopen this session note"));

        enterFixtureTenant();
        var saved = notes.findById(id).orElseThrow();
        assertThat(saved.getIsFinalized()).isTrue();
        assertThat(saved.getFinalizedAt()).isNotNull();
    }

    @Test
    void reportSourceSelectionExcludesForeignClientAndUnknownFileIds() {
        var owned = supportingFile(client, "Synthetic allowed report source");
        var foreign = supportingFile(otherClient, "Synthetic foreign report source");
        var selected = supportingFileService.resolveSupportingFiles(client.getId(),
                List.of(owned.getId(), foreign.getId(), Long.MAX_VALUE));
        assertThat(selected).extracting(ReportSupportingFile::getId).containsExactly(owned.getId());
        assertThat(selected).extracting(ReportSupportingFile::getExtractedText)
                .containsExactly("Synthetic allowed report source");
        assertThat(supportingFileService.resolveSupportingFiles(client.getId(), List.of(foreign.getId()))).isEmpty();
        assertThat(supportingFiles.findById(foreign.getId()).orElseThrow().getExtractedText())
                .isEqualTo("Synthetic foreign report source");
    }

    private Long uploadSharedDocument() throws Exception {
        var headers = createHeaders(therapistToken);
        headers.remove("Content-Type");
        var result = mockMvc.perform(multipart("/api/v1/clients/" + client.getId() + "/documents")
                        .file(new MockMultipartFile("file", "synthetic.png", "image/png", PNG))
                        .headers(headers).param("category", "UPLOADED").param("documentType", "OTHER")
                        .param("shareWithClient", "true").param("needsReview", "false"))
                .andExpect(status().isCreated()).andReturn();
        enterFixtureTenant();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    private Long createDraftNote() {
        enterFixtureTenant();
        var session = sessions.save(Session.builder().client(client).therapist(therapist).service(fixtureService)
                .sessionDate(Instant.parse("2026-09-01T12:00:00Z")).duration(60).status("completed")
                .sessionType("in_person").build());
        return notes.save(SessionNote.builder().session(session).client(client).therapist(therapist)
                .date(session.getSessionDate()).sessionFocus(NOTE_TEXT).draftContent(NOTE_TEXT)
                .isDraft(true).isFinalized(false).build()).getId();
    }

    private ReportSupportingFile supportingFile(Client owner, String text) {
        return supportingFiles.save(ReportSupportingFile.builder().client(owner).createdByUser(therapist)
                .originalName("synthetic.txt").mimeType("text/plain").fileSize(text.length()).extractedText(text).build());
    }

    private String documentUrl(Long clientId, Long documentId) {
        return "/api/v1/clients/" + clientId + "/documents/" + documentId;
    }

    private byte[] readBytes(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        if (result.getRequest().isAsyncStarted()) {
            result.getAsyncResult(15000);
            result = mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn();
        }
        return result.getResponse().getContentAsByteArray();
    }
}

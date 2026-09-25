package com.smart.therapy.flow.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.dto.CreateClientRequest;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.BaseTenantApiTest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Input validation with authenticated tenant requests")
class InputValidationSecurityTest extends BaseTenantApiTest {
    private User therapist;
    private String token;
    @Value("${app.storage.local.path}") private String storagePath;

    @BeforeEach
    void login() {
        therapist = persistStaff(uniqueEmail("validation"), "password123", "THERAPIST");
        token = getAuthToken(therapist.getEmail(), "password123");
    }

    private CreateClientRequest validRequest() {
        CreateClientRequest request = new CreateClientRequest();
        request.setFullName("Synthetic validation client");
        request.setEmail(uniqueEmail("validation-client"));
        request.setStatus("active");
        request.setAssignedTherapistId(therapist.getId());
        return request;
    }

    @Test
    void markupIsReturnedAsJsonData() throws Exception {
        // HTML encoding belongs at the rendering boundary. EmailServiceTest checks HTML escaping;
        // this API assertion does not claim to verify browser rendering.
        CreateClientRequest request = validRequest();
        request.setFullName("<script>alert('XSS')</script>");
        mockMvc.perform(post("/api/v1/clients").headers(createHeaders(token))
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.fullName").value(request.getFullName()));
    }

    @Test
    void sqlSyntaxIsTreatedAsSearchData() throws Exception {
        Client client = persistClient(therapist);
        mockMvc.perform(get("/api/v1/clients").headers(createHeaders(token))
                        .param("search", "'; DROP TABLE clients; --"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        enterFixtureTenant();
        assertThat(clientRepository.existsById(client.getId())).isTrue();
        mockMvc.perform(get("/api/v1/clients/{id}", client.getId()).headers(createHeaders(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(client.getId()));
    }

    @Test
    void malformedEmailHasEmailValidationError() throws Exception {
        CreateClientRequest request = validRequest();
        request.setEmail("invalid-email-format");
        mockMvc.perform(post("/api/v1/clients").headers(createHeaders(token))
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.errors.email").isNotEmpty())
                .andExpect(jsonPath("$.details.errors.status").doesNotExist());
    }

    @Test
    void uploadUsesOpaquePathInsideClientFolder() throws Exception {
        Client client = persistClient(therapist);
        byte[] bytes = "%PDF-1.4\nSynthetic QA document\n%%EOF".getBytes(StandardCharsets.US_ASCII);
        MockMultipartFile file = new MockMultipartFile("file", "../../../etc/passwd.pdf", "application/pdf", bytes);
        var uploadHeaders = createHeaders(token);
        uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        String response = mockMvc.perform(multipart("/api/v1/clients/{id}/documents", client.getId())
                        .file(file).headers(uploadHeaders))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode document = objectMapper.readTree(response);
        Path stored = Path.of(document.path("fileName").asText()).toAbsolutePath().normalize();
        Path clientFolder = Path.of(storagePath, "clients", client.getId().toString()).toAbsolutePath().normalize();
        assertThat(stored.getParent()).isEqualTo(clientFolder);
        assertThat(stored.getFileName().toString()).matches("[a-f0-9-]{36}\\.pdf");
        assertThat(Files.readAllBytes(stored)).isEqualTo(bytes);
    }

    @Test
    void uploadRejectsJsonWithUnsupportedMediaType() throws Exception {
        Client client = persistClient(therapist);
        mockMvc.perform(post("/api/v1/clients/{id}/documents", client.getId())
                        .headers(createHeaders(token)).content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    void excessiveNameHasNameValidationError() throws Exception {
        CreateClientRequest request = validRequest();
        request.setFullName("A".repeat(1000));
        mockMvc.perform(post("/api/v1/clients").headers(createHeaders(token))
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.errors.fullName").isNotEmpty());
    }

    @Test
    void unicodeAndPunctuationRoundTrip() throws Exception {
        CreateClientRequest request = validRequest();
        request.setFullName("Zoë O’Connor & 李");
        request.setPhone("123-456-7890");
        String response = mockMvc.perform(post("/api/v1/clients").headers(createHeaders(token))
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value(request.getFullName()))
                .andExpect(jsonPath("$.phone").value(request.getPhone()))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).path("id").asLong();
        mockMvc.perform(get("/api/v1/clients/{id}", id).headers(createHeaders(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value(request.getFullName()));
    }
}

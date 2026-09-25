package com.smart.therapy.flow.integration.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.dto.ClientResponse;
import com.smart.therapy.flow.client.dto.CreateClientRequest;
import com.smart.therapy.flow.client.dto.UpdateClientRequest;
import com.smart.therapy.flow.client.service.ClientAddressService;
import com.smart.therapy.flow.client.service.ClientService;
import com.smart.therapy.flow.common.BaseTenantApiTest;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ClientService Integration Tests")
class ClientServiceIntegrationTest extends BaseTenantApiTest {

    @Autowired
    private ClientService clientService;

    @Autowired
    private ClientAddressService clientAddressService;

    private AuthPrincipal therapistPrincipal;
    private User therapist;

    @BeforeEach
    void setUp() {
        therapist = persistStaff(uniqueEmail("therapist"), "password123", "THERAPIST");
        therapistPrincipal = fixturePrincipal(therapist, "THERAPIST");
    }

    @Test
    @DisplayName("Should create client successfully with real database")
    void shouldCreateClientSuccessfullyWithRealDatabase() {
        // Arrange
        CreateClientRequest request = new CreateClientRequest();
        request.setStatus("active");
        request.setFullName("Integration Test Client");
        request.setEmail("integration@example.com");
        request.setPhone("123-456-7890");

        // Act
        ClientResponse response = clientService.createClient(request, therapistPrincipal, "127.0.0.1");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getFullName()).isEqualTo("Integration Test Client");
        assertThat(response.getEmail()).isEqualTo("integration@example.com");

        // Verify persisted in database
        assertThat(clientRepository.findById(response.getId())).isPresent();
    }

    @Test
    @DisplayName("Should throw exception when email already exists in database")
    void shouldThrowExceptionWhenEmailAlreadyExistsInDatabase() {
        // Arrange
        CreateClientRequest firstRequest = new CreateClientRequest();
        firstRequest.setStatus("active");
        firstRequest.setFullName("First Client");
        firstRequest.setEmail("duplicate@example.com");
        clientService.createClient(firstRequest, therapistPrincipal, "127.0.0.1");

        CreateClientRequest duplicateRequest = new CreateClientRequest();
        duplicateRequest.setStatus("active");
        duplicateRequest.setFullName("Second Client");
        duplicateRequest.setEmail("duplicate@example.com");

        // Act & Assert
        assertThatThrownBy(() -> clientService.createClient(duplicateRequest, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email already in use");
    }

    @Test
    @DisplayName("Should update client successfully with real database")
    void shouldUpdateClientSuccessfullyWithRealDatabase() {
        // Arrange
        CreateClientRequest createRequest = new CreateClientRequest();
        createRequest.setStatus("active");
        createRequest.setFullName("Original Name");
        createRequest.setEmail("update@example.com");
        ClientResponse created = clientService.createClient(createRequest, therapistPrincipal, "127.0.0.1");

        com.smart.therapy.flow.client.dto.UpdateClientRequest updateRequest = 
                new com.smart.therapy.flow.client.dto.UpdateClientRequest();
        updateRequest.setFullName("Updated Name");
        updateRequest.markFieldPresent("fullName");
        updateRequest.setPhone("999-999-9999");
        updateRequest.markFieldPresent("phone");

        // Act
        ClientResponse updated = clientService.updateClient(created.getId(), updateRequest, therapistPrincipal, "127.0.0.1");

        // Assert
        assertThat(updated).isNotNull();
        assertThat(updated.getFullName()).isEqualTo("Updated Name");
        assertThat(updated.getPhone()).isEqualTo("999-999-9999");

        // Verify persisted in database
        assertThat(clientRepository.findById(created.getId()))
                .isPresent()
                .get()
                .extracting(com.smart.therapy.flow.client.entity.Client::getFullName)
                .isEqualTo("Updated Name");
    }

    @Test
    @DisplayName("Should patch only streetAddress1 and keep other address fields unchanged")
    void shouldPatchSingleAddressFieldWithoutClearingOthers() {
        CreateClientRequest createRequest = new CreateClientRequest();
        createRequest.setStatus("active");
        createRequest.setFullName("Address Patch Client");
        createRequest.setEmail("address-patch@example.com");
        createRequest.setStreetAddress1("123 Main Street");
        createRequest.setStreetAddress2("Suite 100");
        createRequest.setCity("Toronto");
        createRequest.setProvince("ON");
        createRequest.setPostalCode("M5H 2N2");
        createRequest.setCountry("Canada");

        ClientResponse created = clientService.createClient(createRequest, therapistPrincipal, "127.0.0.1");
        assertThat(created.getStreetAddress1()).isEqualTo("123 Main Street");
        assertThat(created.getCity()).isEqualTo("Toronto");

        UpdateClientRequest patchRequest = new UpdateClientRequest();
        patchRequest.setStreetAddress1("House no 631 street 39, E11-3deedede");
        patchRequest.markFieldPresent("streetAddress1");

        ClientResponse patched = clientService.updateClient(
                created.getId(), patchRequest, therapistPrincipal, "127.0.0.1");

        assertThat(patched.getStreetAddress1()).isEqualTo("House no 631 street 39, E11-3deedede");
        assertThat(patched.getStreetAddress2()).isEqualTo("Suite 100");
        assertThat(patched.getCity()).isEqualTo("Toronto");
        assertThat(patched.getProvince()).isEqualTo("ON");
        assertThat(patched.getPostalCode()).isEqualTo("M5H 2N2");
        assertThat(patched.getCountry()).isEqualTo("Canada");

        ClientResponse reloaded = clientService.getClient(created.getId(), therapistPrincipal);
        assertThat(reloaded.getFullName()).isEqualTo("Address Patch Client");
        assertThat(reloaded.getEmail()).isEqualTo("address-patch@example.com");
        assertThat(reloaded.getStreetAddress1()).isEqualTo(patched.getStreetAddress1());
        assertThat(reloaded.getCity()).isEqualTo("Toronto");
        assertThat(reloaded.getInsuranceProvider()).isNull();
        assertThat(reloaded.getReferrerName()).isNull();
        assertThat(reloaded.getEmploymentStatus()).isNull();

        assertThat(clientAddressService.getPrimaryAddress(created.getId()))
                .isPresent()
                .get()
                .satisfies(address -> assertThat(address.getIsPrimary()).isTrue());
    }

    @Test
    @DisplayName("Should delete client successfully with real database")
    void shouldDeleteClientSuccessfullyWithRealDatabase() {
        // Arrange
        CreateClientRequest request = new CreateClientRequest();
        request.setStatus("active");
        request.setFullName("To Be Deleted");
        request.setEmail("delete@example.com");
        ClientResponse created = clientService.createClient(request, therapistPrincipal, "127.0.0.1");

        // Act
        clientService.deleteClient(created.getId(), therapistPrincipal, "127.0.0.1");

        // Assert
        assertThat(clientRepository.findByIdIncludingDeleted(created.getId()).orElseThrow().getDeletedAt()).isNotNull();
    }
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void shouldPersistRenamesWithMoreAndFewerNameTokens() {
        CreateClientRequest request = new CreateClientRequest();
        request.setStatus("active");
        request.setFullName("Ada Lovelace");
        request.setEmail(uniqueEmail("rename"));
        Long id = clientService.createClient(request, therapistPrincipal, "127.0.0.1").getId();
        Long firstTokenId = jdbc.queryForObject(
                "SELECT id FROM tenant_api_test.client_name_blind_indexes WHERE client_id = ? AND token_ord = 0",
                Long.class, id);
        UpdateClientRequest rename = new UpdateClientRequest();
        rename.markFieldPresent("fullName");
        rename.setFullName("Grace Brewster Hopper");
        clientService.updateClient(id, rename, therapistPrincipal, "127.0.0.1");
        // Tokens are whole words plus every prefix of two or more characters, so
        // "Grace Brewster Hopper" grows the collection from 9 rows to 16:
        // 3 words + grace(3) + brewster(6) + hopper(4) prefixes.
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM tenant_api_test.client_name_blind_indexes WHERE client_id = ?", Integer.class, id))
                .isEqualTo(16);
        rename.setFullName("Grace");
        clientService.updateClient(id, rename, therapistPrincipal, "127.0.0.1");
        // Shrinking to "Grace" leaves grace, gr, gra, grac. Rows are reused by ordinal
        // rather than deleted and reinserted, so ordinal 0 keeps its original id.
        assertThat(jdbc.queryForList(
                "SELECT id FROM tenant_api_test.client_name_blind_indexes WHERE client_id = ? ORDER BY token_ord",
                Long.class, id))
                .hasSize(4)
                .first().isEqualTo(firstTokenId);
        assertThat(clientRepository.findById(id).orElseThrow().getFullName()).isEqualTo("Grace");
    }

    @Autowired
    private com.smart.therapy.flow.client.service.ClientMrnService mrnService;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    @Test
    void shouldNotReuseMrnReservedByRolledBackClientTransaction() {
        var transaction = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        String reserved = transaction.execute(status -> {
            String mrn = mrnService.allocateNextMrn();
            status.setRollbackOnly();
            return mrn;
        });
        String next = mrnService.allocateNextMrn();
        assertThat(reserved).isNotBlank();
        assertThat(next).isNotEqualTo(reserved);
        int reservedNumber = Integer.parseInt(reserved.substring(reserved.lastIndexOf('-') + 1));
        int nextNumber = Integer.parseInt(next.substring(next.lastIndexOf('-') + 1));
        assertThat(nextNumber).isEqualTo(reservedNumber + 1);
    }

    @Test
    void shouldFindEncryptedMrnWhenBlindIndexIsMissingOrStale() {
        var client = persistClient(therapist);
        String mrn = mrnService.displayMrn(client);
        jdbc.update("UPDATE tenant_api_test.clients SET client_id_blind_idx = NULL WHERE id = ?", client.getId());
        assertThat(mrnService.existsMrn(mrn, false)).isTrue();
        assertThat(mrnService.findActiveByMrn(mrn)).get().extracting("id").isEqualTo(client.getId());
        jdbc.update("UPDATE tenant_api_test.clients SET client_id_blind_idx = ? WHERE id = ?",
                new byte[]{1, 2, 3}, client.getId());
        assertThat(mrnService.existsMrn(mrn, false)).isTrue();
        assertThat(mrnService.findActiveByMrn(mrn)).get().extracting("id").isEqualTo(client.getId());
        assertThat(mrnService.existsMrn("CL-1900-9999", false)).isFalse();
    }

}

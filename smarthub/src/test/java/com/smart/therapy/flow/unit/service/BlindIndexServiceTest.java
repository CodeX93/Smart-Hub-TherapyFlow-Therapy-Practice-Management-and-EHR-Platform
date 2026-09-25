package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientContact;
import com.smart.therapy.flow.client.enums.ContactType;
import com.smart.therapy.flow.common.service.BlindIndexService;
import com.smart.therapy.flow.common.service.ConfigKeyProvider;
import com.smart.therapy.flow.common.service.KeyProvider;
import com.smart.therapy.flow.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BlindIndexServiceTest {

    private static final String MASTER_KEY =
            "test-master-key-with-at-least-thirty-two-characters";

    private KeyProvider keyProvider;

    @BeforeEach
    void setUp() {
        keyProvider = new ConfigKeyProvider(new MockEnvironment(), MASTER_KEY, "primary", "", null);
        TenantContext.setOrganisationId(42L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void sameInputSameDigestDifferentTenantDifferentDigest() {
        BlindIndexService service = new BlindIndexService(keyProvider, "dual", "pepper");

        byte[] first = service.compute(BlindIndexService.Kind.FULL_NAME, service.normalizeFullName("Jane Doe"));
        byte[] second = service.compute(BlindIndexService.Kind.FULL_NAME, service.normalizeFullName("Jane Doe"));
        assertThat(first).isEqualTo(second);

        TenantContext.setOrganisationId(99L);
        byte[] otherTenant = service.compute(BlindIndexService.Kind.FULL_NAME, service.normalizeFullName("Jane Doe"));
        assertThat(otherTenant).isNotEqualTo(first);
    }

    @Test
    void legacyModeDoesNotWriteBlindIndexes() {
        BlindIndexService service = new BlindIndexService(keyProvider, "legacy", "pepper");
        Client client = Client.builder().clientId("CL-2026-0001").fullName("Jane Doe").build();
        ClientContact contact = ClientContact.builder()
                .contactType(ContactType.EMAIL)
                .contactValue("jane@example.com")
                .build();

        service.updateBlindIndexes(client, List.of(contact));

        assertThat(client.getClientIdBlindIdx()).isNull();
        assertThat(client.getFullNameBlindIdx()).isNull();
        assertThat(contact.getContactBlindIdx()).isNull();
    }

    @Test
    void dualModeWritesBlindIndexes() {
        BlindIndexService service = new BlindIndexService(keyProvider, "dual", "pepper");
        Client client = Client.builder().clientId("CL-2026-0001").fullName("Jane Doe").build();
        ClientContact contact = ClientContact.builder()
                .contactType(ContactType.EMAIL)
                .contactValue("jane@example.com")
                .build();

        service.updateBlindIndexes(client, List.of(contact));

        assertThat(client.getClientIdBlindIdx()).hasSize(32);
        assertThat(client.getFullNameBlindIdx()).hasSize(32);
        assertThat(contact.getContactBlindIdx()).hasSize(32);
        assertThat(client.getNameBlindIndexes()).hasSize(5);
        assertThat(client.getNameBlindIndexes().get(0).getTokenOrd()).isZero();
        assertThat(client.getNameBlindIndexes().get(0).getTokenBlindIdx()).isEqualTo(
                service.compute(BlindIndexService.Kind.NAME_TOKEN, "jane"));
        assertThat(client.getNameBlindIndexes().get(1).getTokenBlindIdx()).isEqualTo(
                service.compute(BlindIndexService.Kind.NAME_TOKEN, "doe"));
    }

    @Test
    void nameTokenDigestsDifferFromFullNameDigest() {
        BlindIndexService service = new BlindIndexService(keyProvider, "blind_only", "pepper");
        byte[] full = service.compute(BlindIndexService.Kind.FULL_NAME, "jane");
        byte[] token = service.compute(BlindIndexService.Kind.NAME_TOKEN, "jane");
        assertThat(token).isNotEqualTo(full);
    }

    @Test
    void syncNameTokenBlindIndexesReplacesTokensOnRename() {
        BlindIndexService service = new BlindIndexService(keyProvider, "blind_only", "pepper");
        Client client = Client.builder().clientId("CL-2026-0001").fullName("Jane Doe").build();
        service.updateBlindIndexes(client, null);
        assertThat(client.getNameBlindIndexes()).hasSize(5);

        client.setFullName("Ada Lovelace");
        service.updateBlindIndexes(client, null);

        assertThat(client.getNameBlindIndexes()).hasSize(9);
        assertThat(client.getNameBlindIndexes().get(0).getTokenBlindIdx()).isEqualTo(
                service.compute(BlindIndexService.Kind.NAME_TOKEN, "ada"));
        assertThat(client.getNameBlindIndexes().get(1).getTokenBlindIdx()).isEqualTo(
                service.compute(BlindIndexService.Kind.NAME_TOKEN, "lovelace"));
    }

    @Test
    void nameTokenRefreshPreservesRowsWhenGrowingAndShrinking() {
        var service = new BlindIndexService(keyProvider, "blind_only", "pepper");
        Client client = Client.builder().fullName("Ada Lovelace").build();
        service.syncNameTokenBlindIndexes(client);
        var first = client.getNameBlindIndexes().get(0);
        var second = client.getNameBlindIndexes().get(1);
        first.setId(10L);
        second.setId(11L);
        // ORM collection order is not guaranteed; reuse by ordinal, not list position.
        java.util.Collections.reverse(client.getNameBlindIndexes());
        client.setFullName("Grace Brewster Hopper");
        service.syncNameTokenBlindIndexes(client);
        assertThat(client.getNameBlindIndexes()).hasSize(16).contains(first, second);
        assertThat(first.getId()).isEqualTo(10L);
        assertThat(first.getTokenBlindIdx()).isEqualTo(service.compute(BlindIndexService.Kind.NAME_TOKEN, "grace"));
        client.setFullName("Grace");
        service.syncNameTokenBlindIndexes(client);
        assertThat(client.getNameBlindIndexes()).hasSize(4).contains(first);
        client.setFullName(" ");
        service.syncNameTokenBlindIndexes(client);
        assertThat(client.getNameBlindIndexes()).isEmpty();
    }

    @Test
    void contactOnlyRefreshDoesNotRebuildPersistedNameTokens() {
        BlindIndexService service = new BlindIndexService(keyProvider, "dual", "pepper");
        Client client = Client.builder().clientId("CL-2026-0001").fullName("Jane Doe").build();
        service.updateBlindIndexes(client, null);
        var originalTokens = new java.util.ArrayList<>(client.getNameBlindIndexes());
        ClientContact contact = ClientContact.builder()
                .client(client)
                .contactType(ContactType.EMAIL)
                .contactValue("jane@example.com")
                .build();

        service.updateContactBlindIndexes(List.of(contact));

        assertThat(client.getNameBlindIndexes()).containsExactlyElementsOf(originalTokens);
        assertThat(contact.getContactBlindIdx()).isNotNull();
    }

    @Test
    void scalarClientRefreshDoesNotCreateNameTokens() {
        BlindIndexService service = new BlindIndexService(keyProvider, "dual", "pepper");
        Client client = Client.builder().clientId("CL-2026-0001").fullName("Jane Doe").build();

        service.updateClientScalarBlindIndexes(client);

        assertThat(client.getClientIdBlindIdx()).isNotNull();
        assertThat(client.getFullNameBlindIdx()).isNotNull();
        assertThat(client.getNameBlindIndexes()).isEmpty();
    }
}

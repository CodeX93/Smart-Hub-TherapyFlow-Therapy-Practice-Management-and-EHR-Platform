package com.smart.therapy.flow.unit.client;

import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.service.ClientSearchHelper;
import com.smart.therapy.flow.common.service.BlindIndexService;
import com.smart.therapy.flow.common.service.ConfigKeyProvider;
import com.smart.therapy.flow.common.service.EncryptionService;
import com.smart.therapy.flow.common.service.KeyProvider;
import com.smart.therapy.flow.common.tenant.TenantContext;
import org.jasypt.encryption.StringEncryptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ClientSearchHelperTest {

    private static final String MASTER_KEY =
            "test-master-key-with-at-least-thirty-two-characters";

    private ClientSearchHelper helper;
    private ClientRepository clientRepository;
    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        KeyProvider keyProvider = new ConfigKeyProvider(new MockEnvironment(), MASTER_KEY, "primary", "", null);
        BlindIndexService blindIndexService = new BlindIndexService(keyProvider, "legacy", "pepper");
        StringEncryptor noop = new StringEncryptor() {
            @Override
            public String encrypt(String message) {
                return message;
            }

            @Override
            public String decrypt(String encryptedMessage) {
                return encryptedMessage;
            }
        };
        encryptionService = new EncryptionService(noop, keyProvider);
        clientRepository = mock(ClientRepository.class);
        helper = new ClientSearchHelper(encryptionService, blindIndexService, clientRepository);
        TenantContext.setOrganisationId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void classifiesMrnEmailPhoneAndName() {
        assertThat(helper.classify("CL-2026-0001")).isEqualTo(ClientSearchHelper.SearchKind.MRN);
        assertThat(helper.classify("cl-2024-42")).isEqualTo(ClientSearchHelper.SearchKind.MRN);
        assertThat(helper.classify("2026-0001")).isEqualTo(ClientSearchHelper.SearchKind.MRN);
        assertThat(helper.classify("CL 2026 0001")).isEqualTo(ClientSearchHelper.SearchKind.MRN);
        assertThat(helper.classify("CL")).isEqualTo(ClientSearchHelper.SearchKind.MRN);
        assertThat(helper.classify("cl-2026")).isEqualTo(ClientSearchHelper.SearchKind.MRN);
        assertThat(helper.classify("Clark")).isEqualTo(ClientSearchHelper.SearchKind.FULL_NAME);
        assertThat(helper.classify("jane.doe@example.com")).isEqualTo(ClientSearchHelper.SearchKind.EMAIL);
        assertThat(helper.classify("+1 (555) 010-1234")).isEqualTo(ClientSearchHelper.SearchKind.PHONE);
        assertThat(helper.classify("5550101234")).isEqualTo(ClientSearchHelper.SearchKind.PHONE);
        assertThat(helper.classify("Jane Doe")).isEqualTo(ClientSearchHelper.SearchKind.FULL_NAME);
    }

    @Test
    void normalizeMrnCanonicalizesPrefixAndPadding() {
        assertThat(helper.normalizeMrn("cl-2025-581")).isEqualTo("CL-2025-0581");
        assertThat(helper.normalizeMrn("2025-0581")).isEqualTo("CL-2025-0581");
        assertThat(helper.normalizeMrn(" CL-2025-0581 ")).isEqualTo("CL-2025-0581");
    }

    @Test
    void mrnSearchCandidatesIncludePaddedAndUnpaddedForms() {
        assertThat(helper.mrnSearchCandidates("CL-2025-581"))
                .contains("CL-2025-581", "CL-2025-0581");
        assertThat(helper.mrnSearchCandidates("2025-0581"))
                .contains("CL-2025-0581");
    }

    @Test
    void normalizeFullNameCollapsesWhitespace() {
        assertThat(helper.normalizeFullName("  Jane   Doe  ")).isEqualTo("jane doe");
        assertThat(helper.normalizeFullName("Jane\t\tDoe")).isEqualTo("jane doe");
    }

    @Test
    void classifiesSingleTokenAsFullNameKindForTokenSearch() {
        // Client Page typically searches a first or last name; classification stays FULL_NAME
        // so ClientSearchHelper can match name-token digests (not LIKE on ciphertext).
        assertThat(helper.classify("Jane")).isEqualTo(ClientSearchHelper.SearchKind.FULL_NAME);
        assertThat(helper.classify("doe")).isEqualTo(ClientSearchHelper.SearchKind.FULL_NAME);
    }

    @Test
    void findClientIdsMatchingMrnMatchesExactAndPrefixCaseInsensitive() {
        when(clientRepository.findActiveClientNumberCandidates()).thenReturn(List.of(
                new Object[]{10L, "CL-2026-0001", null}, new Object[]{11L, "CL-2025-0042", null},
                new Object[]{12L, "CL-2024-0099", null}));

        assertThat(helper.findClientIdsMatchingMrn("CL-2026-0001")).containsExactly(10L);
        assertThat(helper.findClientIdsMatchingMrn("cl-2026-0001")).containsExactly(10L);
        assertThat(helper.findClientIdsMatchingMrn("CL")).containsExactlyInAnyOrder(10L, 11L, 12L);
        assertThat(helper.findClientIdsMatchingMrn("CL-2025")).containsExactly(11L);
        assertThat(helper.findClientIdsMatchingMrn("cl-2026")).containsExactly(10L);
    }
    @Test
    void recognizesMrnFragmentsWithoutChangingPhoneOrNameSearch() {
        assertThat(helper.classify("0395")).isEqualTo(ClientSearchHelper.SearchKind.MRN);
        assertThat(helper.classify("2026")).isEqualTo(ClientSearchHelper.SearchKind.MRN);
        assertThat(helper.classify("2026-03")).isEqualTo(ClientSearchHelper.SearchKind.MRN);
        assertThat(helper.classify("5550101234")).isEqualTo(ClientSearchHelper.SearchKind.PHONE);
        assertThat(helper.classify("Clark")).isEqualTo(ClientSearchHelper.SearchKind.FULL_NAME);
    }

    @Test
    void partialMrnFindsSequenceAndYearFragments() {
        when(clientRepository.findActiveClientNumberCandidates()).thenReturn(java.util.Collections.singletonList(
                new Object[]{20L, "CL-2026-0395", null}));
        assertThat(helper.findClientIdsMatchingMrn("0395")).containsExactly(20L);
        assertThat(helper.findClientIdsMatchingMrn("2026-03")).containsExactly(20L);
    }

    @Test
    void encryptedAndReferralNumberSearchDoesNotHydrateClientGraphs() {
        when(clientRepository.findActiveClientNumberCandidates()).thenReturn(List.of(
                new Object[]{30L, encryptionService.encrypt("CL-2026-0395"), null},
                new Object[]{31L, "CL-2025-0100", encryptionService.encrypt("CL-2026-0395")},
                new Object[]{32L, null, null}));
        assertThat(helper.findClientIdsMatchingMrn("0395")).containsExactlyInAnyOrder(30L, 31L);
        assertThat(helper.findClientIdsMatchingMrn("CL-2025-100")).containsExactly(31L);
        assertThat(helper.findClientIdsMatchingMrn("9999")).isEmpty();
        verify(clientRepository, never()).findByOrganisationId(any());
    }

}

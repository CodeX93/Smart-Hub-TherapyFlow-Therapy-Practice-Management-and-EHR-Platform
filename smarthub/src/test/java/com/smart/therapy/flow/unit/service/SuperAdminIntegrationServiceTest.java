package com.smart.therapy.flow.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.common.service.EncryptionService;
import com.smart.therapy.flow.superadmin.entity.PlatformIntegrationConfig;
import com.smart.therapy.flow.superadmin.repository.PlatformApiKeyRepository;
import com.smart.therapy.flow.superadmin.repository.PlatformApiKeyScopeRepository;
import com.smart.therapy.flow.superadmin.repository.PlatformIntegrationConfigRepository;
import com.smart.therapy.flow.superadmin.service.SuperAdminIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SuperAdminIntegrationService Stripe config tests")
class SuperAdminIntegrationServiceTest {

    @Mock
    private PlatformIntegrationConfigRepository integrationRepository;
    @Mock
    private PlatformApiKeyRepository apiKeyRepository;
    @Mock
    private PlatformApiKeyScopeRepository apiKeyScopeRepository;
    @Mock
    private EncryptionService encryptionService;

    private SuperAdminIntegrationService integrationService;

    @BeforeEach
    void setUp() {
        integrationService = new SuperAdminIntegrationService(
                integrationRepository,
                apiKeyRepository,
                apiKeyScopeRepository,
                new ObjectMapper(),
                encryptionService
        );
        when(encryptionService.encrypt(anyString())).thenAnswer(inv -> "enc:" + inv.getArgument(0));
        when(encryptionService.decrypt(anyString())).thenAnswer(inv -> {
            String value = inv.getArgument(0);
            return value.startsWith("enc:") ? value.substring(4) : value;
        });
    }

    @Test
    @DisplayName("Persists platform webhook secret separately from connect webhook secret")
    void shouldPersistPlatformWebhookSecret() throws Exception {
        when(integrationRepository.findByIntegrationKey("stripe"))
                .thenReturn(Optional.of(PlatformIntegrationConfig.builder()
                        .integrationKey("stripe")
                        .createdAt(Instant.now())
                        .build()));
        when(integrationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        integrationService.upsertIntegration(
                "stripe",
                true,
                null,
                "pk_test_abc",
                "sk_test_abc",
                null,
                "whsec_connect_secret",
                "whsec_platform_secret",
                null,
                1L
        );

        ArgumentCaptor<PlatformIntegrationConfig> captor = ArgumentCaptor.forClass(PlatformIntegrationConfig.class);
        verify(integrationRepository).save(captor.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> config = new ObjectMapper().readValue(
                captor.getValue().getConfigJson(),
                Map.class
        );
        assertThat(config.get("connectWebhookSecretEncrypted")).isEqualTo("enc:whsec_connect_secret");
        assertThat(config.get("platformWebhookSecretEncrypted")).isEqualTo("enc:whsec_platform_secret");
        assertThat(config.get("webhookSecretEncrypted")).isEqualTo("enc:whsec_platform_secret");
    }
}

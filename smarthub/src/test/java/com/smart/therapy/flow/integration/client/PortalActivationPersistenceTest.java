package com.smart.therapy.flow.integration.client;

import com.smart.therapy.flow.client.service.ClientPortalSettingsService;
import com.smart.therapy.flow.common.BaseTenantApiTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class PortalActivationPersistenceTest extends BaseTenantApiTest {
    @Autowired private ClientPortalSettingsService settings;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private EntityManager entityManager;

    @Test
    void activationSurvivesPersistenceContextClearBeforeCommit() {
        var therapist = persistStaff(uniqueEmail("activation-therapist"), "password123", "THERAPIST");
        var client = persistClient(therapist);
        settings.setHasPortalAccess(client.getId(), true);
        Instant activatedAt = Instant.parse("2026-01-01T12:00:00Z");
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            settings.setActivated(client.getId(), true, activatedAt);
            // The activation response switches tenant read contexts and clears the session.
            entityManager.clear();
        });
        assertThat(settings.isActivated(client.getId())).isTrue();
        assertThat(settings.getByClientId(client.getId()).getActivatedAt()).isEqualTo(activatedAt);
    }

    @Autowired private com.smart.therapy.flow.auth.service.AuthIdentityService identities;

    @Test
    void activationThenFreshLoginSucceedsWithTheChosenPassword() throws Exception {
        var therapist = persistStaff(uniqueEmail("activation-login-therapist"), "password123", "THERAPIST");
        var client = persistClient(therapist);
        portalToken(client);
        enterFixtureTenant();
        var identity = client.getAuthIdentity();
        String email = identity.getLoginIdentifier();
        settings.setActivated(client.getId(), false, null);
        String token = java.util.UUID.randomUUID().toString();
        identities.setEmailVerificationToken(identity.getId(), token, Instant.now().plusSeconds(3600));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/portal/activate").headers(createHeaders())
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "token", token, "password", "chosen-password-123"))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        enterFixtureTenant();
        assertThat(settings.isActivated(client.getId())).isTrue();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/portal/login").headers(createHeaders())
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", email, "password", "chosen-password-123", "orgSlug", TENANT_SLUG))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    @org.springframework.boot.test.mock.mockito.SpyBean private com.smart.therapy.flow.auth.service.AuthKnownDeviceService devices;
    @Autowired private com.smart.therapy.flow.auth.service.TotpService totp;
    @Autowired private com.smart.therapy.flow.common.service.EncryptionService encryption;
    @Autowired private com.smart.therapy.flow.auth.repository.AuthMfaCredentialRepository mfaCredentials;

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void publicMfaLoginThenPortalProfileSucceeds(boolean trusted) throws Exception {
        var therapist = persistStaff(uniqueEmail("public-login-therapist"), "password123", "THERAPIST");
        var client = persistClient(therapist);
        portalToken(client);
        enterFixtureTenant();
        var identity = client.getAuthIdentity();
        String secret = totp.generateSecret();
        mfaCredentials.save(com.smart.therapy.flow.auth.entity.AuthMfaCredential.builder()
                .authIdentity(identity).enabled(true).confirmedAt(Instant.now())
                .totpEnabled(true).encryptedSecret(encryption.encrypt(secret)).build());
        String trustToken = devices.trustCurrentDevice(identity.getId(), "127.0.0.1", "public-login-test");
        org.mockito.Mockito.doAnswer(invocation -> {
            // The fixture has schema-qualified mappings, so explicitly verify the
            // transaction's actual schema too; otherwise public routing can be masked.
            assertThat(entityManager.createNativeQuery("select current_schema()").getSingleResult())
                    .isEqualTo(TENANT_SCHEMA);
            return invocation.callRealMethod();
        }).when(devices).recordSuccessfulLogin(identity.getId(), "127.0.0.1", "public-login-test");
        com.smart.therapy.flow.common.tenant.TenantContext.clear();
        // No tenant header: this is the production app's shared client login page.
        String body = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/portal/login").contentType("application/json")
                        .header("User-Agent", "public-login-test")
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", identity.getLoginIdentifier(), "password", "password123",
                                "orgId", tenantOrganisation.getId().toString(), "deviceTrustToken", trusted ? trustToken : ""))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString();
        var response = objectMapper.readTree(body);
        if (!trusted) {
            assertThat(response.path("mfaRequired").asBoolean()).isTrue();
            String challenge = response.path("mfaChallengeToken").asText();
            String code = totp.generateCode(secret, Instant.now().getEpochSecond() / 30, 6);
            String verification = objectMapper.writeValueAsString(java.util.Map.of(
                    "mfaChallengeToken", challenge, "code", code, "method", "TOTP"));
            body = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .post("/api/v1/portal/mfa/verify-login").contentType("application/json")
                            .header("User-Agent", "public-login-test").content(verification))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andReturn().getResponse().getContentAsString();
            response = objectMapper.readTree(body);
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .post("/api/v1/portal/mfa/verify-login").contentType("application/json")
                            .content(verification))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
        } else {
            assertThat(response.path("mfaSkippedTrustedDevice").asBoolean()).isTrue();
        }
        String accessToken = response.path("accessToken").asText();
        assertThat(accessToken).isNotBlank();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/portal/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }
}

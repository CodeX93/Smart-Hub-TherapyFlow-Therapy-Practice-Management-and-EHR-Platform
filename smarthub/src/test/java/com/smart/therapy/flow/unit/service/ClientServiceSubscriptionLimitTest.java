package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.client.dto.CreateClientRequest;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.service.ClientContactService;
import com.smart.therapy.flow.client.service.ClientMrnService;
import com.smart.therapy.flow.client.service.ClientService;
import com.smart.therapy.flow.client.validation.ClientRequestValidator;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientService subscription limit tests")
class ClientServiceSubscriptionLimitTest {

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private ClientRequestValidator requestValidator;
    @Mock
    private SubscriptionFeatureService subscriptionFeatureService;
    @Mock
    private PermissionChecker permissionChecker;
    @Mock
    private ClientContactService contactService;

    @Mock
    private ClientMrnService clientMrnService;

    @InjectMocks
    private ClientService clientService;

    private AuthPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        TenantContext.setOrganisationId(1L);
        TenantContext.setSchemaName("tenant_1");

        adminPrincipal = TestDataFactory.createAuthPrincipal(TestDataFactory.createTestAdmin());
        when(subscriptionFeatureService.getEffectiveLimit(1L, SubscriptionFeatureService.FEATURE_CLIENT_LIMIT, null))
                .thenReturn(1);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Blocks client creation when active client limit is reached")
    void shouldBlockClientCreationAtLimit() {
        CreateClientRequest request = new CreateClientRequest();
        request.setFullName("Client One");
        request.setStatus("active");

        when(clientRepository.countNonDeleted()).thenReturn(1L);

        assertThatThrownBy(() -> clientService.createClient(request, adminPrincipal, "127.0.0.1"))
                .isInstanceOf(StoryApiException.class)
                .satisfies(ex -> {
                    StoryApiException error = (StoryApiException) ex;
                    assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(error.getCode()).isEqualTo("LIMIT_EXCEEDED");
                });
    }

    @Test
    @DisplayName("Blocks restoring a deleted client when active client limit is reached")
    void shouldBlockClientRestoreAtLimit() {
        Client deletedClient = Client.builder()
                .clientId("CL-1")
                .fullName("Deleted Client")
                .build();
        deletedClient.setId(5L);
        deletedClient.setIsDeleted(true);

        when(permissionChecker.hasPermission(adminPrincipal, "CLIENT_DELETE")).thenReturn(true);
        when(clientRepository.findByIdIncludingDeleted(5L)).thenReturn(Optional.of(deletedClient));
        when(contactService.getPrimaryEmail(5L)).thenReturn(Optional.empty());
        when(clientRepository.countNonDeleted()).thenReturn(1L);

        assertThatThrownBy(() -> clientService.restoreClient(5L, adminPrincipal, "127.0.0.1"))
                .isInstanceOf(StoryApiException.class)
                .satisfies(ex -> {
                    StoryApiException error = (StoryApiException) ex;
                    assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(error.getCode()).isEqualTo("LIMIT_EXCEEDED");
                });
    }
}

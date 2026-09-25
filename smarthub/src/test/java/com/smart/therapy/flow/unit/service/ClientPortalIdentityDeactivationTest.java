package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientPortalSettings;
import com.smart.therapy.flow.client.repository.ClientPortalSettingsRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.service.ClientPortalSettingsService;
import com.smart.therapy.flow.client.service.ClientService;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.session.repository.SessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Portal login is deactivated when a client loses portal access")
class ClientPortalIdentityDeactivationTest {

    private static AuthIdentity activeClientIdentity() {
        AuthIdentity identity = new AuthIdentity();
        identity.setId(7001L);
        identity.setIdentityType(IdentityType.CLIENT);
        identity.setIsActive(true);
        return identity;
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("Client soft delete")
    class OnClientDelete {

        @Mock
        private ClientRepository clientRepository;
        @Mock
        private SessionRepository sessionRepository;
        @Mock
        private CurrentUserService currentUserService;
        @Mock
        private PermissionChecker permissionChecker;
        @Mock
        private AuditLogService auditLogService;
        @Mock
        private ApplicationEventPublisher eventPublisher;
        @Mock
        private AuthIdentityRepository authIdentityRepository;

        @InjectMocks
        private ClientService clientService;

        @Test
        @DisplayName("Deactivates the portal identity so it stops counting as an end user")
        void deactivatesPortalIdentityOnDelete() {
            User admin = TestDataFactory.createTestAdmin();
            admin.setId(2L);
            AuthPrincipal principal = TestDataFactory.createAuthPrincipal(admin);
            when(permissionChecker.hasPermission(principal, "CLIENT_DELETE")).thenReturn(true);
            when(currentUserService.requireCurrentUser(principal)).thenReturn(admin);

            Long clientId = 4242L;
            Client client = TestDataFactory.createTestClientWithId(clientId);
            client.setIsDeleted(false);
            client.setHistory(List.of());
            AuthIdentity identity = activeClientIdentity();
            client.setAuthIdentity(identity);

            when(clientRepository.findByIdIncludingDeleted(clientId)).thenReturn(Optional.of(client));
            when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));
            when(sessionRepository.findByClientId(clientId)).thenReturn(List.of());

            clientService.deleteClient(clientId, principal, "127.0.0.1");

            assertThat(identity.getIsActive()).isFalse();
            verify(authIdentityRepository).save(identity);
        }

        @Test
        @DisplayName("Leaves an already inactive identity untouched")
        void skipsInactiveIdentity() {
            User admin = TestDataFactory.createTestAdmin();
            admin.setId(2L);
            AuthPrincipal principal = TestDataFactory.createAuthPrincipal(admin);
            when(permissionChecker.hasPermission(principal, "CLIENT_DELETE")).thenReturn(true);
            when(currentUserService.requireCurrentUser(principal)).thenReturn(admin);

            Long clientId = 4243L;
            Client client = TestDataFactory.createTestClientWithId(clientId);
            client.setIsDeleted(false);
            client.setHistory(List.of());
            AuthIdentity identity = activeClientIdentity();
            identity.setIsActive(false);
            client.setAuthIdentity(identity);

            when(clientRepository.findByIdIncludingDeleted(clientId)).thenReturn(Optional.of(client));
            when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));
            when(sessionRepository.findByClientId(clientId)).thenReturn(List.of());

            clientService.deleteClient(clientId, principal, "127.0.0.1");

            verify(authIdentityRepository, never()).save(any(AuthIdentity.class));
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("Portal access revoke")
    class OnPortalDisable {

        @Mock
        private ClientPortalSettingsRepository portalSettingsRepository;
        @Mock
        private ClientRepository clientRepository;
        @Mock
        private AuthIdentityRepository authIdentityRepository;

        @InjectMocks
        private ClientPortalSettingsService portalSettingsService;

        @Test
        @DisplayName("Deactivates the portal identity when access is revoked")
        void deactivatesIdentityOnDisable() {
            Long clientId = 5150L;
            Client client = TestDataFactory.createTestClientWithId(clientId);
            AuthIdentity identity = activeClientIdentity();
            client.setAuthIdentity(identity);

            ClientPortalSettings settings = ClientPortalSettings.builder()
                    .client(client)
                    .hasPortalAccess(true)
                    .isActivated(true)
                    .build();
            when(portalSettingsRepository.findByClientId(clientId)).thenReturn(Optional.of(settings));
            when(portalSettingsRepository.save(any(ClientPortalSettings.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(clientRepository.findByIdIncludingDeleted(clientId)).thenReturn(Optional.of(client));

            portalSettingsService.disablePortalAccess(clientId);

            assertThat(settings.getHasPortalAccess()).isFalse();
            assertThat(settings.getIsActivated()).isFalse();
            assertThat(identity.getIsActive()).isFalse();
            verify(authIdentityRepository).save(identity);
        }

        @Test
        @DisplayName("Is a no-op for a client that never had a portal login")
        void skipsClientWithoutIdentity() {
            Long clientId = 5151L;
            Client client = TestDataFactory.createTestClientWithId(clientId);
            client.setAuthIdentity(null);

            ClientPortalSettings settings = ClientPortalSettings.builder()
                    .client(client)
                    .hasPortalAccess(true)
                    .isActivated(false)
                    .build();
            when(portalSettingsRepository.findByClientId(clientId)).thenReturn(Optional.of(settings));
            when(portalSettingsRepository.save(any(ClientPortalSettings.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(clientRepository.findByIdIncludingDeleted(clientId)).thenReturn(Optional.of(client));

            portalSettingsService.disablePortalAccess(clientId);

            verify(authIdentityRepository, never()).save(any(AuthIdentity.class));
        }
    }
}

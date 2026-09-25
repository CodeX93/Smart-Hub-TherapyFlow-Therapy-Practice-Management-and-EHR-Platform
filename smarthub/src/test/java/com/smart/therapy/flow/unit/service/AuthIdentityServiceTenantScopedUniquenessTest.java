package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.service.AuthIdentityService;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthIdentityService tenant-scoped uniqueness")
class AuthIdentityServiceTenantScopedUniquenessTest {

    @Mock
    private AuthIdentityRepository authIdentityRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private OrganisationRepository organisationRepository;

    @InjectMocks
    private AuthIdentityService service;

    private Organisation orgA;
    private Organisation orgB;

    @BeforeEach
    void setUp() {
        orgA = Organisation.builder().id(1L).slug("acme").schemaName("tenant_1").build();
        orgB = Organisation.builder().id(2L).slug("beta").schemaName("tenant_2").build();
        lenient().when(passwordEncoder.encode(any())).thenReturn("hashed");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Nested
    @DisplayName("createStaffIdentity")
    class CreateStaff {

        @Test
        @DisplayName("Creates staff with email + username scoped to current organisation")
        void createsStaffWithEmailAndUsername() {
            TenantContext.setOrganisationId(1L);
            when(organisationRepository.findById(1L)).thenReturn(Optional.of(orgA));
            when(authIdentityRepository.existsByOrganisationIdAndNormalisedUsername(1L, "jdoe", null))
                    .thenReturn(false);
            when(authIdentityRepository.existsByOrganisationIdAndNormalisedEmail(1L, "jdoe@acme.com", null))
                    .thenReturn(false);
            when(authIdentityRepository.save(any(AuthIdentity.class))).thenAnswer(inv -> {
                AuthIdentity saved = inv.getArgument(0);
                saved.setId(100L);
                return saved;
            });

            AuthIdentity created = service.createStaffIdentity("jdoe", "jdoe@acme.com", "Secret1!", 9L);

            ArgumentCaptor<AuthIdentity> captor = ArgumentCaptor.forClass(AuthIdentity.class);
            verify(authIdentityRepository).save(captor.capture());
            AuthIdentity persisted = captor.getValue();
            assertThat(persisted.getUsername()).isEqualTo("jdoe");
            assertThat(persisted.getNormalisedUsername()).isEqualTo("jdoe");
            assertThat(persisted.getEmail()).isEqualTo("jdoe@acme.com");
            assertThat(persisted.getNormalisedEmail()).isEqualTo("jdoe@acme.com");
            assertThat(persisted.getLoginIdentifier()).isEqualTo("jdoe");
            assertThat(persisted.getIdentityType()).isEqualTo(IdentityType.STAFF);
            assertThat(persisted.getOrganisation()).isEqualTo(orgA);
            assertThat(created.getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Rejects username already used in the same organisation")
        void rejectsDuplicateUsernameInSameOrg() {
            TenantContext.setOrganisationId(1L);
            when(organisationRepository.findById(1L)).thenReturn(Optional.of(orgA));
            when(authIdentityRepository.existsByOrganisationIdAndNormalisedUsername(1L, "jdoe", null))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.createStaffIdentity("jdoe", "other@acme.com", "Secret1!", 9L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Username already in use");

            verify(authIdentityRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rejects email already used by staff or client in the same organisation")
        void rejectsDuplicateEmailInSameOrg() {
            TenantContext.setOrganisationId(1L);
            when(organisationRepository.findById(1L)).thenReturn(Optional.of(orgA));
            when(authIdentityRepository.existsByOrganisationIdAndNormalisedUsername(1L, "jdoe", null))
                    .thenReturn(false);
            when(authIdentityRepository.existsByOrganisationIdAndNormalisedEmail(1L, "shared@acme.com", null))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.createStaffIdentity("jdoe", "shared@acme.com", "Secret1!", 9L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Email already in use");
        }

        @Test
        @DisplayName("Allows same username in a different organisation (tenant-scoped uniqueness)")
        void allowsSameUsernameInOtherOrg() {
            TenantContext.setOrganisationId(2L);
            when(organisationRepository.findById(2L)).thenReturn(Optional.of(orgB));
            // Org B does not have jdoe — uniqueness check is org-scoped only
            when(authIdentityRepository.existsByOrganisationIdAndNormalisedUsername(2L, "jdoe", null))
                    .thenReturn(false);
            when(authIdentityRepository.existsByOrganisationIdAndNormalisedEmail(2L, "jdoe@beta.com", null))
                    .thenReturn(false);
            when(authIdentityRepository.save(any(AuthIdentity.class))).thenAnswer(inv -> inv.getArgument(0));

            AuthIdentity created = service.createStaffIdentity("jdoe", "jdoe@beta.com", "Secret1!", 9L);

            assertThat(created.getOrganisation()).isEqualTo(orgB);
            assertThat(created.getNormalisedUsername()).isEqualTo("jdoe");
            // Never consulted global uniqueness APIs
            verify(authIdentityRepository, never()).existsByNormalisedLoginIdentifier(any());
        }
    }

    @Nested
    @DisplayName("createClientIdentity")
    class CreateClient {

        @Test
        @DisplayName("Creates CLIENT identity with email only (username null)")
        void createsClientWithEmailOnly() {
            TenantContext.setOrganisationId(1L);
            when(organisationRepository.findById(1L)).thenReturn(Optional.of(orgA));
            when(authIdentityRepository.existsByOrganisationIdAndNormalisedEmail(1L, "client@acme.com", null))
                    .thenReturn(false);
            when(authIdentityRepository.save(any(AuthIdentity.class))).thenAnswer(inv -> inv.getArgument(0));

            AuthIdentity created = service.createClientIdentity("client@acme.com", "TempPass1!", 9L);

            assertThat(created.getIdentityType()).isEqualTo(IdentityType.CLIENT);
            assertThat(created.getEmail()).isEqualTo("client@acme.com");
            assertThat(created.getNormalisedEmail()).isEqualTo("client@acme.com");
            assertThat(created.getUsername()).isNull();
            assertThat(created.getNormalisedUsername()).isNull();
            assertThat(created.getLoginIdentifier()).isEqualTo("client@acme.com");
        }

        @Test
        @DisplayName("Rejects portal email that collides with staff email in same org")
        void rejectsPortalEmailTakenByStaff() {
            TenantContext.setOrganisationId(1L);
            when(organisationRepository.findById(1L)).thenReturn(Optional.of(orgA));
            when(authIdentityRepository.existsByOrganisationIdAndNormalisedEmail(1L, "staff@acme.com", null))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.createClientIdentity("staff@acme.com", "TempPass1!", 9L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Email already in use");
        }

        @Test
        @DisplayName("Requires organisation context for client identities")
        void requiresOrgContext() {
            TenantContext.clear();

            assertThatThrownBy(() -> service.createClientIdentity("client@acme.com", "TempPass1!", 9L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Organisation context");
        }
    }

    @Nested
    @DisplayName("updateUsername / updateEmail")
    class Updates {

        @Test
        @DisplayName("updateUsername rejects CLIENT identities")
        void clientCannotHaveUsername() {
            AuthIdentity client = AuthIdentity.builder()
                    .id(50L)
                    .identityType(IdentityType.CLIENT)
                    .organisation(orgA)
                    .email("c@acme.com")
                    .normalisedEmail("c@acme.com")
                    .build();

            assertThatThrownBy(() -> service.updateUsername(client, "anyuser"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Clients cannot have a username");
        }

        @Test
        @DisplayName("updateUsername checks uniqueness within the identity organisation")
        void updateUsernameScopedToOrg() {
            AuthIdentity staff = AuthIdentity.builder()
                    .id(50L)
                    .identityType(IdentityType.STAFF)
                    .organisation(orgA)
                    .username("old")
                    .normalisedUsername("old")
                    .email("s@acme.com")
                    .normalisedEmail("s@acme.com")
                    .loginIdentifier("old")
                    .normalisedLoginIdentifier("old")
                    .build();
            when(authIdentityRepository.existsByOrganisationIdAndNormalisedUsername(1L, "newname", 50L))
                    .thenReturn(false);
            when(authIdentityRepository.save(any(AuthIdentity.class))).thenAnswer(inv -> inv.getArgument(0));

            AuthIdentity updated = service.updateUsername(staff, "newname");

            assertThat(updated.getUsername()).isEqualTo("newname");
            assertThat(updated.getNormalisedUsername()).isEqualTo("newname");
            assertThat(updated.getLoginIdentifier()).isEqualTo("newname");
            verify(authIdentityRepository).existsByOrganisationIdAndNormalisedUsername(eq(1L), eq("newname"), eq(50L));
        }

        @Test
        @DisplayName("updateEmail checks uniqueness within the identity organisation")
        void updateEmailScopedToOrg() {
            AuthIdentity staff = AuthIdentity.builder()
                    .id(50L)
                    .identityType(IdentityType.STAFF)
                    .organisation(orgA)
                    .username("jdoe")
                    .normalisedUsername("jdoe")
                    .email("old@acme.com")
                    .normalisedEmail("old@acme.com")
                    .build();
            when(authIdentityRepository.existsByOrganisationIdAndNormalisedEmail(1L, "new@acme.com", 50L))
                    .thenReturn(false);
            when(authIdentityRepository.save(any(AuthIdentity.class))).thenAnswer(inv -> inv.getArgument(0));

            AuthIdentity updated = service.updateEmail(staff, "new@acme.com");

            assertThat(updated.getEmail()).isEqualTo("new@acme.com");
            assertThat(updated.getNormalisedEmail()).isEqualTo("new@acme.com");
            verify(authIdentityRepository).existsByOrganisationIdAndNormalisedEmail(eq(1L), eq("new@acme.com"), eq(50L));
        }
    }

    @Nested
    @DisplayName("platform uniqueness")
    class Platform {

        @Test
        @DisplayName("Platform identities use org-null uniqueness checks")
        void platformUsesNullOrgChecks() {
            TenantContext.clear();
            when(authIdentityRepository.existsPlatformByNormalisedUsername("superadmin", null)).thenReturn(false);
            when(authIdentityRepository.existsPlatformByNormalisedEmail("super@therapyflow.com", null))
                    .thenReturn(false);
            when(authIdentityRepository.save(any(AuthIdentity.class))).thenAnswer(inv -> inv.getArgument(0));

            AuthIdentity created = service.createStaffIdentity("superadmin", "super@therapyflow.com", "Secret1!", 1L);

            assertThat(created.getOrganisation()).isNull();
            verify(authIdentityRepository).existsPlatformByNormalisedUsername("superadmin", null);
            verify(authIdentityRepository).existsPlatformByNormalisedEmail("super@therapyflow.com", null);
            verify(authIdentityRepository, never())
                    .existsByOrganisationIdAndNormalisedEmail(any(), any(), isNull());
        }
    }
}

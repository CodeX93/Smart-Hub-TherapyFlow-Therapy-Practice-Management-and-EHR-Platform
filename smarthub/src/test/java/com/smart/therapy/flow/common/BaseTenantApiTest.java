package com.smart.therapy.flow.common;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import com.smart.therapy.flow.auth.dto.JwtAuthenticationResponse;
import com.smart.therapy.flow.auth.dto.LoginRequest;
import com.smart.therapy.flow.auth.entity.*;
import com.smart.therapy.flow.auth.repository.AuthIdentityRoleRepository;
import com.smart.therapy.flow.auth.repository.PermissionRepository;
import com.smart.therapy.flow.auth.repository.RolePermissionRepository;
import com.smart.therapy.flow.auth.repository.RoleRepository;
import com.smart.therapy.flow.billing.entity.Service;
import com.smart.therapy.flow.billing.repository.ServiceRepository;
import com.smart.therapy.flow.client.dto.CreateClientRequest;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientPortalSettings;
import com.smart.therapy.flow.client.repository.ClientPortalSettingsRepository;
import com.smart.therapy.flow.client.service.ClientService;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.service.EmailService;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.entity.UserOrganisation;
import com.smart.therapy.flow.organisation.repository.UserOrganisationRepository;
import com.smart.therapy.flow.organisation.service.TenantDirectoryService;
import com.smart.therapy.flow.organisation.service.TenantMigrationScheduler;
import com.smart.therapy.flow.organisation.service.TenantSystemOptionSeedService;
import com.smart.therapy.flow.session.entity.Room;
import com.smart.therapy.flow.session.repository.RoomRepository;
import com.smart.therapy.flow.subscription.entity.OrgFeatureOverride;
import com.smart.therapy.flow.subscription.repository.OrgFeatureOverrideRepository;
import com.smart.therapy.flow.superadmin.service.PlatformControlPlaneDefaultsEnsurer;
import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.entity.UserProfileWorkingHours;
import com.smart.therapy.flow.user.repository.UserProfileRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Committed synthetic fixtures allow real HTTP auth transactions to see identities and memberships. */
@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.default_schema=tenant_api_test",
        "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
        "app.phi-encryption.search.mode=blind_only",
        "app.encryption.provider=config",
        "app.encryption.master-key=repository-fixture-key-not-for-production",
        "spring.datasource.hikari.maximum-pool-size=201",
        "spring.datasource.hikari.minimum-idle=2",
        "logging.level.com.smart.therapy.flow.common.filter.HttpLoggingFilter=WARN",
        "logging.level.com.smart.therapy.flow.common.logging.MethodLoggingAspect=WARN"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public abstract class BaseTenantApiTest extends BaseIntegrationTest {
    @DynamicPropertySource
    static void isolateFixtureDatabase(DynamicPropertyRegistry registry) {
        TenantTestDatabase.register(registry, "api");
    }

    protected static final String TENANT_SCHEMA = "tenant_api_test";
    protected static final String TENANT_SLUG = "api-fixture";
    protected Organisation tenantOrganisation;

    protected Service fixtureService;
    protected Room fixtureRoom;
    @Autowired private RoomRepository fixtureRooms;
    @Autowired private PermissionRepository fixturePermissions;
    @Autowired private RolePermissionRepository fixtureRolePermissions;
    @Autowired private TenantSystemOptionSeedService optionSeeds;
    @Autowired private OrgFeatureOverrideRepository featureOverrides;
    @Autowired private ServiceRepository fixtureServices;
    @Autowired private ClientService fixtureClients;
    @Autowired private JdbcTemplate fixtureJdbc;
    @Autowired private ClientPortalSettingsRepository fixturePortalSettings;
    @Autowired private UserProfileRepository fixtureProfiles;
    @Autowired private RoleRepository fixtureRoles;
    @Autowired private AuthIdentityRoleRepository fixtureIdentityRoles;
    @Autowired private UserOrganisationRepository fixtureMemberships;
    @Autowired private TenantDirectoryService fixtureDirectory;

    // Match the isolated QA harness: authentication stays real, delivery/maintenance do not run.
    @MockBean private JavaMailSender mailSender;
    @MockBean private EmailService emailService;
    @MockBean private TenantMigrationScheduler migrationScheduler;
    @MockBean private PlatformControlPlaneDefaultsEnsurer defaultsEnsurer;
    @MockBean(name = "org.springframework.context.annotation.internalScheduledAnnotationProcessor")
    private ScheduledAnnotationBeanPostProcessor scheduledProcessor;

    @BeforeEach
    void initialiseTenantApiFixture() {
        TenantContext.clear();
        tenantOrganisation = organisationRepository.findAll().stream()
                .filter(org -> TENANT_SLUG.equals(org.getSlug())).findFirst()
                .orElseGet(() -> organisationRepository.save(Organisation.builder()
                        .name("Synthetic API fixture").slug(TENANT_SLUG).subdomain(TENANT_SLUG)
                        .schemaName(TENANT_SCHEMA).status("ACTIVE").timezone("UTC").build()));
        fixtureDirectory.runDirectoryRefreshNow();
        enterFixtureTenant();
        // Billing list ordering reads V91's non-entity legacy ID mapping table.
        if (fixtureJdbc.queryForObject("SELECT to_regclass('public.clienthub_legacy_id_mappings')::text",
                String.class) == null) {
            new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
                    new org.springframework.core.io.ClassPathResource(
                            "db/migration/V91__clienthub_migration_control.sql"))
                    .execute(java.util.Objects.requireNonNull(fixtureJdbc.getDataSource()));
        }
        // V57 owns this non-entity table, so Hibernate test DDL cannot create it.
        fixtureJdbc.execute("CREATE TABLE IF NOT EXISTS tenant_api_test.client_mrn_counters (year integer PRIMARY KEY, next_value bigint NOT NULL DEFAULT 0)");
        // Match the V54 encryption column widths used by migrated tenants.
        fixtureJdbc.execute("ALTER TABLE tenant_api_test.client_addresses ALTER COLUMN city TYPE TEXT, ALTER COLUMN state_province TYPE TEXT, ALTER COLUMN postal_code TYPE TEXT, ALTER COLUMN country TYPE TEXT, ALTER COLUMN state_legacy TYPE TEXT, ALTER COLUMN zip_code_legacy TYPE TEXT");
        fixtureRoom = fixtureRooms.save(Room.builder()
                .roomNumber("QA-" + UUID.randomUUID()).roomName("Synthetic room").build());
        optionSeeds.seedDefaults(tenantOrganisation.getId(), TENANT_SCHEMA);
        for (String feature : List.of("BILLING_MODULE", "CLIENT_PORTAL")) {
            if (featureOverrides.findByOrganisationIdAndFeatureKey(tenantOrganisation.getId(), feature).isEmpty()) {
                featureOverrides.save(OrgFeatureOverride.builder()
                        .organisation(tenantOrganisation).featureKey(feature).enabled(true).build());
            }
        }
        fixtureService = fixtureServices.save(Service.builder()
                .serviceCode("QA-" + UUID.randomUUID().toString().substring(0, 16)).serviceName("Synthetic therapy")
                .baseRate(new BigDecimal("100.00")).duration(60).isActive(true).build());
    }

    protected void enterFixtureTenant() {
        TenantContext.setSchemaName(TENANT_SCHEMA);
        TenantContext.setOrganisationId(tenantOrganisation.getId());
    }

    @AfterEach
    void clearTenantApiFixtureContext() {
        TenantContext.clear();
    }

    protected User persistStaff(String email, String password, String roleName) {
        AuthIdentity identity = persistIdentity(email, password, roleName, IdentityType.STAFF);
        User user = userRepository.save(User.builder().email(identity.getEmail()).fullName("Synthetic API user")
                .authIdentity(identity).isActive(true).build());
        var profile = UserProfile.builder().user(user).timezone("UTC").build();
        for (var day : DayOfWeek.values()) {
            profile.getWorkingHours().add(UserProfileWorkingHours.builder()
                    .userProfile(profile).day(day.name()).startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(17, 0)).build());
        }
        fixtureProfiles.save(profile);
        return user;
    }

    private AuthIdentity persistIdentity(String email, String password, String roleName, IdentityType type) {
        enterFixtureTenant();
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        AuthIdentity identity = authIdentityRepository.save(AuthIdentity.builder()
                .organisation(tenantOrganisation).loginIdentifier(normalized).normalisedLoginIdentifier(normalized)
                .email(normalized).normalisedEmail(normalized).username(normalized).normalisedUsername(normalized)
                .passwordHash(passwordEncoder.encode(password)).identityType(type).isActive(true).build());
        fixtureMemberships.save(UserOrganisation.builder().auth(identity).organisation(tenantOrganisation)
                .createdAt(Instant.now()).build());
        Role role = ensureFixtureRole(roleName);
        fixtureIdentityRoles.save(AuthIdentityRole.builder().authIdentity(identity)
                .organisation(tenantOrganisation).role(role).build());
        return identity;
    }

    protected String portalToken(Client client) throws Exception {
        String response = portalLogin(client).getResponse().getContentAsString();
        String token = objectMapper.readTree(response).path("accessToken").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    /** Gives the client portal access and signs in, returning the whole login response. */
    protected MvcResult portalLogin(Client client) throws Exception {
        String email = uniqueEmail("portal");
        var identity = persistIdentity(email, "password123", "CLIENT", IdentityType.CLIENT);
        client.setAuthIdentity(identity);
        clientRepository.save(client);
        var settings = fixturePortalSettings.findByClientId(client.getId())
                .orElseGet(ClientPortalSettings::new);
        settings.setClient(client);
        settings.setHasPortalAccess(true);
        settings.setIsActivated(true);
        settings.setActivatedAt(Instant.now());
        fixturePortalSettings.save(settings);
        return mockMvc.perform(post("/api/v1/portal/login").headers(createHeaders())
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "password123", "orgSlug", TENANT_SLUG))))
                .andExpect(status().isOk()).andReturn();
    }

    protected void registerOtherTenant() {
        fixtureJdbc.execute("CREATE SCHEMA IF NOT EXISTS tenant_api_other");
        if (organisationRepository.findAll().stream().noneMatch(org -> "api-other".equals(org.getSlug()))) {
            organisationRepository.save(Organisation.builder().name("Other synthetic tenant").slug("api-other")
                    .subdomain("api-other").schemaName("tenant_api_other").status("ACTIVE").timezone("UTC").build());
        }
        fixtureDirectory.runDirectoryRefreshNow();
    }

    protected static Instant fixtureSessionDate(int days) {
        return LocalDate.now(ZoneOffset.UTC).plusDays(days)
                .atTime(12, 0).toInstant(ZoneOffset.UTC);
    }

    protected static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.test";
    }

    protected static Set<String> grants(String role) {
        if ("CLIENT".equals(role)) return Set.of("CLIENT_PORTAL_ACCESS");
        var permissions = new HashSet<>(Set.of("CLIENT_VIEW", "CLIENT_VIEW_OWN", "CLIENT_CREATE",
                "CLIENT_EDIT", "CLIENT_DELETE", "SESSION_VIEW", "SESSION_CREATE", "SESSION_EDIT",
                "CONSENT_ADMIN_VIEW", "BILLING_VIEW", "BILLING_CREATE"));
        if ("ADMIN".equals(role)) permissions.addAll(Set.of("CLIENT_VIEW_ALL", "USER_VIEW", "USER_CREATE", "USER_MANAGE"));
        if ("STAFF".equals(role)) return Set.of("CLIENT_VIEW_OWN");
        return permissions;
    }

    /**
     * Roles live in one shared schema for the whole Spring context, so whichever suite asks
     * for a role first defines it. Find-or-create here and top up any grant that a role
     * built elsewhere is missing, so a suite running second never inherits a
     * permission-less role and starts answering 403.
     */
    protected Role ensureFixtureRole(String name) {
        Role role = fixtureRoles.findByNameAndOrganisationIsNullIgnoreCase(name)
                .orElseGet(() -> createFixtureRole(name));
        // Read the join table rather than role.getRolePermissions(): that collection is
        // lazy and callers are not guaranteed an open session.
        Set<Long> granted = new HashSet<>();
        for (RolePermission link : fixtureRolePermissions.findAll()) {
            if (link.getRole() != null && role.getId().equals(link.getRole().getId())
                    && link.getPermission() != null) {
                granted.add(link.getPermission().getId());
            }
        }
        for (String grant : grants(name)) {
            Permission permission = fixturePermissions.findByName(grant).orElseGet(() -> fixturePermissions.save(
                    Permission.builder().name(grant).displayName(grant).category("qa").isActive(true).build()));
            if (!granted.contains(permission.getId())) {
                fixtureRolePermissions.save(RolePermission.builder().role(role).permission(permission).build());
            }
        }
        return role;
    }

    private Role createFixtureRole(String name) {
        Role role = fixtureRoles.save(Role.builder().name(name).displayName(name).isActive(true).isSystem(true).build());
        var links = new HashSet<RolePermission>();
        for (String grant : grants(name)) {
            Permission permission = fixturePermissions.findByName(grant).orElseGet(() -> fixturePermissions.save(
                    Permission.builder().name(grant).displayName(grant).category("qa").isActive(true).build()));
            links.add(RolePermission.builder().role(role).permission(permission).build());
        }
        role.setRolePermissions(links);
        return fixtureRoles.save(role);
    }

    protected AuthPrincipal fixturePrincipal(User user, String role) {
        var authorities = new HashSet<>(grants(role));
        authorities.add("ROLE_" + role);
        return TestDataFactory.createAuthPrincipal(user, authorities.toArray(String[]::new));
    }

    protected Client persistClient(User therapist) {
        return persistClient(therapist, uniqueEmail("client"));
    }

    protected Client persistClient(User therapist, String email) {
        enterFixtureTenant();
        var request = new CreateClientRequest();
        request.setStatus("active");
        request.setFullName("Synthetic client");
        request.setEmail(email);
        var response = fixtureClients.createClient(request, fixturePrincipal(therapist, "THERAPIST"), "127.0.0.1");
        return clientRepository.findById(response.getId()).orElseThrow();
    }

    @Override
    protected String getAuthToken(String email, String password) {
        enterFixtureTenant();
        if (userRepository.findByEmail(email).isEmpty()) {
            persistStaff(email, password, "THERAPIST");
        }
        var request = new LoginRequest();
        request.setUsername(email);
        request.setPassword(password);
        request.setOrgSlug(TENANT_SLUG);
        try {
            String json = mockMvc.perform(post("/api/v1/auth/login").headers(createHeaders())
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            var response = objectMapper.readValue(json, JwtAuthenticationResponse.class);
            assertThat(response.getTenantSchema()).isEqualTo(TENANT_SCHEMA);
            assertThat(response.getOrganisationId()).isEqualTo(tenantOrganisation.getId());
            assertThat(response.getAccessToken()).isNotBlank();
            return response.getAccessToken();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to authenticate synthetic tenant fixture", ex);
        } finally {
            enterFixtureTenant();
        }
    }

    @Override
    protected HttpHeaders createHeaders() {
        HttpHeaders headers = super.createHeaders();
        headers.set("X-Tenant-Subdomain", TENANT_SLUG);
        return headers;
    }

    @Override
    protected HttpHeaders createHeaders(String token) {
        HttpHeaders headers = createHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}

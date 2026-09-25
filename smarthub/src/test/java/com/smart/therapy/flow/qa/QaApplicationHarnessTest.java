package com.smart.therapy.flow.qa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.auth.entity.*;
import com.smart.therapy.flow.auth.repository.*;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientPortalSettings;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.security.PermissionConstants;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.entity.UserOrganisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.repository.UserOrganisationRepository;
import com.smart.therapy.flow.organisation.service.TenantDirectoryService;
import com.smart.therapy.flow.organisation.service.TenantSystemOptionSeedService;
import com.smart.therapy.flow.subscription.entity.OrgFeatureOverride;
import com.smart.therapy.flow.subscription.repository.OrgFeatureOverrideRepository;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test-classpath-only real HTTP application for the external Playwright runner.
 * It exposes no fixture/reset/auth-bypass endpoint and cannot be packaged in the production jar.
 * Hibernate fixture schema is not a replacement for Flyway migration tests.
 */
@EnabledIfSystemProperty(named = "qa.browser.enabled", matches = "true")
@ActiveProfiles("test")
// The launcher owns this long-lived server. Close its cached Spring context
// after the stop signal, before Surefire waits for the forked JVM to exit.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "server.address=127.0.0.1", "spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true",
        "app.auth.mfa.enforcement=OPTIONAL", "spring.jpa.show-sql=false",
        "app.phi-encryption.search.mode=blind_only",
        "logging.level.com.smart.therapy.flow=ERROR", "logging.level.org.hibernate.SQL=OFF",
        "app.notifications.email-enabled=false", "tenant.jobs.migration.enabled=false",
        "tenant.migration.run-on-startup=false", "spring.docker.compose.enabled=false"
})
class QaApplicationHarnessTest {
    static {
        // This check runs before Spring can initialize Hibernate's create-drop datasource.
        if (Boolean.getBoolean("qa.browser.enabled")) {
            String url = System.getenv("DB_URL");
            String runId = System.getenv("QA_MANAGED_DATABASE");
            if (url == null || !url.matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/therapyflow_test") || runId == null)
                throw new IllegalStateException("Use scripts/test-app.sh with its owned disposable database");
            try (var connection = java.sql.DriverManager.getConnection(url, System.getenv("DB_USERNAME"), System.getenv("DB_PASSWORD"));
                 var statement = connection.prepareStatement("select run_id from public.qa_run_guard")) {
                try (var rows = statement.executeQuery()) {
                    if (!rows.next() || !runId.equals(rows.getString(1))) throw new IllegalStateException("QA database ownership marker mismatch");
                }
            } catch (java.sql.SQLException e) { throw new IllegalStateException("QA database ownership marker missing", e); }
        }
    }
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManagerFactory emf;
    @Autowired OrganisationRepository organisations;
    @Autowired AuthIdentityRepository identities;
    @Autowired AuthIdentityRoleRepository identityRoles;
    @Autowired RoleRepository roles;
    @Autowired PermissionRepository permissions;
    @Autowired UserRepository users;
    @Autowired UserOrganisationRepository memberships;
    @Autowired ClientRepository clients;
    @Autowired com.smart.therapy.flow.common.service.BlindIndexService blindIndexes;
    @Autowired OrgFeatureOverrideRepository features;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper mapper;
    @Autowired TenantSystemOptionSeedService optionSeeds;
    @Autowired TenantDirectoryService directory;
    @Autowired com.smart.therapy.flow.session.repository.SessionRepository sessions;
    @Autowired com.smart.therapy.flow.billing.repository.ServiceRepository services;

    // Disable outbound delivery and automatic maintenance; business APIs/auth remain real.
    @MockBean org.springframework.mail.javamail.JavaMailSender mailSender;
    @MockBean com.smart.therapy.flow.common.service.EmailService emailService;
    @MockBean com.smart.therapy.flow.organisation.service.TenantMigrationScheduler migrationScheduler;
    @MockBean com.smart.therapy.flow.superadmin.service.PlatformControlPlaneDefaultsEnsurer defaultsEnsurer;
    @MockBean(name = "org.springframework.context.annotation.internalScheduledAnnotationProcessor")
    ScheduledAnnotationBeanPostProcessor scheduledProcessor;

    @Test
    void serveIsolatedApplicationUntilRunnerFinishes() throws Exception {
        String url = System.getenv("DB_URL");
        assertThat(url).matches("jdbc:postgresql://127\\.0\\.0\\.1:[0-9]+/therapyflow_test");
        Path runtime = Path.of(Objects.requireNonNull(System.getenv("QA_RUNTIME_DIR"))).toRealPath();
        assertThat(runtime.getFileName().toString()).isEqualTo(".runtime");
        Map<String, Object> fixtures = new LinkedHashMap<>();
        TenantContext.clear();
        createRole("ADMIN", allPermissions());
        createRole("THERAPIST", Set.of("CLIENT_VIEW_OWN", "CLIENT_VIEW", "SESSION_VIEW", "SESSION_CREATE", "SESSION_EDIT", "CONSENT_ADMIN_VIEW", "ASSESSMENT_VIEW", "BILLING_VIEW", "NOTE_VIEW", "NOTE_CREATE", "NOTE_EDIT"));
        createRole("STAFF", Set.of("CLIENT_VIEW_OWN"));
        createRole("CLIENT", Set.of("CLIENT_PORTAL_ACCESS"));
        fixtures.put("alpha", seedTenant("tenant_qa_alpha", "qa-alpha", "Alpha"));
        fixtures.put("beta", seedTenant("tenant_qa_beta", "qa-beta", "Beta"));
        TenantContext.clear();
        directory.refresh();
        fixtures.put("baseUrl", "http://127.0.0.1:" + port);
        fixtures.put("searchMode", blindIndexes.getSearchMode().name());
        fixtures.put("schemaMode", "Hibernate generated, tenant tables cloned; Flyway not verified");
        Path ready = runtime.resolve("ready.json");
        Path temp = runtime.resolve("ready.tmp");
        Files.writeString(temp, mapper.writeValueAsString(fixtures));
        Files.setPosixFilePermissions(temp, PosixFilePermissions.fromString("rw-------"));
        Files.move(temp, ready);
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.MINUTES.toNanos(20);
        while (!Files.exists(runtime.resolve("stop")) && System.nanoTime() < deadline) Thread.sleep(250);
        assertThat(Files.exists(runtime.resolve("stop"))).as("QA runner completed before server deadline").isTrue();
        TenantContext.clear();
    }

    private Set<String> allPermissions() throws IllegalAccessException {
        Set<String> names = new TreeSet<>();
        for (var field : PermissionConstants.class.getFields()) {
            if (field.getType() != String.class) continue;
            var matcher = Pattern.compile("'([A-Z][A-Z_]+)'").matcher((String) field.get(null));
            while (matcher.find()) names.add(matcher.group(1));
        }
        names.addAll(Set.of("CONSENT_ADMIN_VIEW", "USER_MANAGE", "BILLING_MANAGE", "CLIENT_VIEW_ALL"));
        return names;
    }

    private void createRole(String name, Set<String> grants) {
        Role role = roles.save(Role.builder().name(name).displayName(name).isActive(true).isSystem(true).build());
        Set<RolePermission> links = new HashSet<>();
        for (String grant : grants) {
            Permission permission = permissions.findByName(grant).orElseGet(() -> permissions.save(
                    Permission.builder().name(grant).displayName(grant).category("qa").isActive(true).build()));
            links.add(RolePermission.builder().role(role).permission(permission).build());
        }
        role.setRolePermissions(links);
        roles.save(role);
    }

    private Map<String, Object> seedTenant(String schema, String slug, String label) throws Exception {
        TenantContext.clear();
        cloneTenantTables(schema);
        Organisation org = organisations.save(Organisation.builder().name("QA " + label).slug(slug)
                .subdomain(slug).schemaName(schema).status("ACTIVE").timezone("America/Toronto").build());
        for (String feature : List.of("BILLING_MODULE", "ADVANCED_BILLING", "TASK_MANAGEMENT", "CLIENT_PORTAL", "ASSESSMENT_MODULE", "DOCUMENT_MANAGEMENT", "AI_ASSISTANT", "AI_TRANSCRIPTION")) {
            features.save(OrgFeatureOverride.builder().organisation(org).featureKey(feature).enabled(true).build());
        }
        TenantContext.setOrganisationId(org.getId());TenantContext.setSchemaName(schema);
        optionSeeds.seedDefaults(org.getId(), schema);
        String password = "Qa!" + UUID.randomUUID();
        User therapist = staff(org, "THERAPIST", "therapist-" + slug + "@example.test", "QA " + label + " Therapist", password);
        User admin = staff(org, "ADMIN", "admin-" + slug + "@example.test", "QA " + label + " Admin", password);
        User restricted = staff(org, "STAFF", "staff-" + slug + "@example.test", "QA " + label + " Restricted", password);
        AuthIdentity clientIdentity = identity(org, "CLIENT", "client-" + slug + "@example.test", password);
        Client client = saveClient(Client.builder().clientId("QA-" + label.toUpperCase()).fullName("QA " + label + " Client")
                .status("active").assignedTherapist(therapist).authIdentity(clientIdentity).build());
        client.setPortalSettings(ClientPortalSettings.builder().client(client).hasPortalAccess(true).isActivated(true)
                .activatedAt(Instant.now()).emailNotifications(false).build());
        clients.save(client);
        Client unassigned = saveClient(Client.builder().clientId("QA-" + label.toUpperCase() + "-OTHER")
                .fullName("QA " + label + " Unassigned").status("active").build());
        unassigned.setPortalSettings(ClientPortalSettings.builder().client(unassigned).hasPortalAccess(false).isActivated(false).build());
        clients.save(unassigned);
        var service = services.save(com.smart.therapy.flow.billing.entity.Service.builder().serviceCode("QA-THERAPY")
                .serviceName("QA Therapy").baseRate(new java.math.BigDecimal("100.00")).duration(60).isActive(true).build());
        var session = sessions.save(com.smart.therapy.flow.session.entity.Session.builder().client(client).therapist(therapist)
                .service(service).sessionDate(Instant.now().minusSeconds(86400)).duration(60).status("completed").sessionType("in_person").build());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orgId",org.getId());result.put("schema",schema);result.put("slug",slug);
        result.put("admin",Map.of("email",admin.getEmail(),"password",password,"id",admin.getId()));
        result.put("therapist",Map.of("email",therapist.getEmail(),"password",password,"id",therapist.getId()));
        result.put("restricted",Map.of("email",restricted.getEmail(),"password",password,"id",restricted.getId()));
        result.put("client",Map.of("email",clientIdentity.getLoginIdentifier(),"password",password,"id",client.getId(),"name",client.getFullName()));
        result.put("unassignedClientId",unassigned.getId());
        result.put("completedSessionId",session.getId());
        result.put("sessionDate",session.getSessionDate().toString());
        return result;
    }

    private Client saveClient(Client client) {
        // Search uses encrypted-name digests; repository-only fixtures must seed those too.
        blindIndexes.updateClientScalarBlindIndexes(client);
        blindIndexes.syncNameTokenBlindIndexes(client);
        return clients.save(client);
    }

    private User staff(Organisation org, String role, String email, String name, String password) {
        AuthIdentity auth = identity(org, role, email, password);
        return users.save(User.builder().email(email).fullName(name).authIdentity(auth).isActive(true).build());
    }

    private AuthIdentity identity(Organisation org, String role, String email, String password) {
        AuthIdentity auth = identities.save(AuthIdentity.builder().organisation(org).loginIdentifier(email).normalisedLoginIdentifier(email)
                .email(email).normalisedEmail(email).username(email).normalisedUsername(email)
                .passwordHash(encoder.encode(password)).identityType("CLIENT".equals(role) ? IdentityType.CLIENT : IdentityType.STAFF)
                .isActive(true).build());
        memberships.save(UserOrganisation.builder().auth(auth).organisation(org).createdAt(Instant.now()).build());
        identityRoles.save(AuthIdentityRole.builder().authIdentity(auth).organisation(org).role(roles.findByName(role).orElseThrow()).build());
        return auth;
    }

    private void cloneTenantTables(String schema) {
        // Names are fixed test constants, never HTTP/user-provided identifiers.
        assertThat(schema).matches("tenant_qa_(alpha|beta)");
        Set<String> publicEntities = new HashSet<>();
        emf.getMetamodel().getEntities().forEach(entity -> {
            Table table = entity.getJavaType().getAnnotation(Table.class);
            if (table != null && "public".equals(table.schema())) publicEntities.add(table.name());
        });
        jdbc.execute("CREATE SCHEMA " + schema);
        List<String> tables = jdbc.queryForList("SELECT tablename FROM pg_tables WHERE schemaname='public'", String.class);
        for (String table : tables) {
            if (!publicEntities.contains(table)) {
                assertThat(table).matches("[a-zA-Z0-9_]+");
                jdbc.execute("CREATE TABLE " + schema + ".\"" + table + "\" (LIKE public.\"" + table + "\" INCLUDING ALL)");
            }
        }
    }
}

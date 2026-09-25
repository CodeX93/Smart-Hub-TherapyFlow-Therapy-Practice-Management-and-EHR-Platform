package com.smart.therapy.flow.integration.repository;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientContact;
import com.smart.therapy.flow.client.enums.ContactType;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.service.ClientSearchHelper;
import com.smart.therapy.flow.common.BaseTenantRepositoryTest;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.service.BlindIndexService;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

/** Characterizes supported inputs and explicit gaps; a passing gap test does NOT mean support. */
@Import(ClientSearchHelper.class)
@TestPropertySource(properties = "app.phi-encryption.search.mode=blind_only")
class ClientSearchFieldIntegrationTest extends BaseTenantRepositoryTest {
    @Autowired private TestEntityManager em;
    @Autowired private ClientRepository clients;
    @Autowired private ClientSearchHelper search;
    @Autowired private BlindIndexService indexes;
    private Long targetId;

    @BeforeEach
    void fixtures() {
        var therapist = persistTherapist();
        Client target = TestDataFactory.createTestClient(therapist);
        target.setFullName("Jane Doe");
        target.setClientId("CL-2026-0395");
        target.setDateOfBirth(LocalDate.of(1988, 4, 19));
        indexes.updateBlindIndexes(target, List.of());
        em.persistAndFlush(target);
        targetId = target.getId();
        contact(target, ContactType.EMAIL, "jane.doe@example.com", false);
        contact(target, ContactType.PHONE, "+15195550123", false);
        contact(target, ContactType.WORK_PHONE, "+15195550124", false);
        contact(target, ContactType.EMAIL, "removed@example.com", true);
        Client other = TestDataFactory.createTestClient(therapist);
        other.setFullName("Joan Roe");
        other.setClientId("CL-2025-0800");
        indexes.updateBlindIndexes(other, List.of());
        em.persistAndFlush(other);
        em.clear();
    }

    private void contact(Client client, ContactType type, String value, boolean deleted) {
        var contact = ClientContact.builder().client(client).contactType(type).contactValue(value).build();
        contact.setIsDeleted(deleted);
        indexes.applyContactBlindIndex(contact);
        em.persistAndFlush(contact);
    }

    @org.junit.jupiter.api.Test
    void renamingRemovesOldPrefixesAndIndexesNewOnes() {
        Client client = clients.findById(targetId).orElseThrow();
        client.setFullName("Alice Brown");
        indexes.updateBlindIndexes(client, List.of());
        em.flush();
        em.clear();
        assertThat(clients.findAll(search.clientSearchSpecification("Jan"))).isEmpty();
        assertThat(clients.findAll(search.clientSearchSpecification("Al Br")))
                .extracting(Client::getId).containsExactly(targetId);
    }

    @org.junit.jupiter.api.Test
    void slashDatesAreMonthFirstAndLeapDatesAreStrict() {
        Client client = clients.findById(targetId).orElseThrow();
        client.setDateOfBirth(LocalDate.of(1988, 4, 5));
        indexes.updateBlindIndexes(client, List.of());
        em.flush();
        assertThat(clients.findAll(search.clientSearchSpecification("04/05/1988")))
                .extracting(Client::getId).containsExactly(targetId);
        assertThat(clients.findAll(search.clientSearchSpecification("05/04/1988"))).isEmpty();
        client.setDateOfBirth(LocalDate.of(1988, 2, 29));
        indexes.updateBlindIndexes(client, List.of());
        em.flush();
        assertThat(clients.findAll(search.clientSearchSpecification("02/29/1988")))
                .extracting(Client::getId).containsExactly(targetId);
        assertThat(clients.findAll(search.clientSearchSpecification("02/29/1989"))).isEmpty();
    }

    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired private com.smart.therapy.flow.common.service.EncryptionService encryption;

    @org.junit.jupiter.api.Test
    void newBackfillJobRebuildsExistingClientsAndCanBeRepeated() {
        var jobs = org.mockito.Mockito.mock(com.smart.therapy.flow.common.repository.PlatformPhiBlindIndexBackfillJobRepository.class);
        var organisations = org.mockito.Mockito.mock(com.smart.therapy.flow.organisation.repository.OrganisationRepository.class);
        var health = org.mockito.Mockito.mock(com.smart.therapy.flow.organisation.service.TenantSchemaHealthService.class);
        var scheduler = new com.smart.therapy.flow.common.service.PhiBlindIndexBackfillScheduler(
                jobs, organisations, health, encryption, indexes, jdbc, null);
        var organisation = new com.smart.therapy.flow.organisation.entity.Organisation();
        organisation.setId(com.smart.therapy.flow.common.tenant.TenantContext.getOrganisationId());
        organisation.setSchemaName(SCHEMA);
        org.mockito.Mockito.when(organisations.findAll()).thenReturn(List.of(organisation));
        org.mockito.Mockito.when(health.schemaExists(SCHEMA)).thenReturn(true);
        String version = org.springframework.test.util.ReflectionTestUtils.invokeMethod(scheduler, "computeHmacKeyVersion");
        org.mockito.Mockito.when(jobs.findBySchemaNameAndJobKind(org.mockito.ArgumentMatchers.eq(SCHEMA), org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> {
                    String kind = invocation.getArgument(1);
                    if (kind.equals("NAME_PREFIX_DIGESTS")) return java.util.Optional.empty();
                    return java.util.Optional.of(com.smart.therapy.flow.common.entity.PlatformPhiBlindIndexBackfillJob.builder()
                            .jobKind(kind).status("COMPLETED").hmacKeyVersion(version).build());
                });
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(scheduler, "seedMissingJobs");
        var captured = org.mockito.ArgumentCaptor.forClass(com.smart.therapy.flow.common.entity.PlatformPhiBlindIndexBackfillJob.class);
        org.mockito.Mockito.verify(jobs).save(captured.capture());
        var job = captured.getValue();
        assertThat(job.getJobKind()).isEqualTo("NAME_PREFIX_DIGESTS");
        assertThat(job.getLastProcessedId()).isZero();
        jdbc.update("DELETE FROM " + SCHEMA + ".client_name_blind_indexes WHERE token_ord >= 2");
        assertThat(clients.findAll(search.clientSearchSpecification("Jan"))).isEmpty();
        org.mockito.Mockito.when(jobs.findRunnableJobs()).thenReturn(List.of(job));
        for (int run = 0; run < 2; run++) {
            job.setLastProcessedId(0L);
            org.springframework.test.util.ReflectionTestUtils.invokeMethod(scheduler, "processOneJob");
            assertThat(job.getStatus()).isEqualTo("PENDING");
            assertThat(job.getLastError()).isNull();
            assertThat(clients.findAll(search.clientSearchSpecification("Ja Do")))
                    .extracting(Client::getId).containsExactly(targetId);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + SCHEMA
                    + ".client_name_blind_indexes WHERE client_id = ?", Long.class, targetId)).isEqualTo(5);
        }
    }

    @org.junit.jupiter.api.Test
    void completedNamePrefixJobReopensWhenClientsMissingTokens() {
        var jobs = org.mockito.Mockito.mock(com.smart.therapy.flow.common.repository.PlatformPhiBlindIndexBackfillJobRepository.class);
        var organisations = org.mockito.Mockito.mock(com.smart.therapy.flow.organisation.repository.OrganisationRepository.class);
        var health = org.mockito.Mockito.mock(com.smart.therapy.flow.organisation.service.TenantSchemaHealthService.class);
        var scheduler = new com.smart.therapy.flow.common.service.PhiBlindIndexBackfillScheduler(
                jobs, organisations, health, encryption, indexes, jdbc, null);
        var organisation = new com.smart.therapy.flow.organisation.entity.Organisation();
        organisation.setId(com.smart.therapy.flow.common.tenant.TenantContext.getOrganisationId());
        organisation.setSchemaName(SCHEMA);
        org.mockito.Mockito.when(organisations.findAll()).thenReturn(List.of(organisation));
        org.mockito.Mockito.when(health.schemaExists(SCHEMA)).thenReturn(true);
        String version = org.springframework.test.util.ReflectionTestUtils.invokeMethod(scheduler, "computeHmacKeyVersion");
        var completedPrefix = com.smart.therapy.flow.common.entity.PlatformPhiBlindIndexBackfillJob.builder()
                .jobKind("NAME_PREFIX_DIGESTS").status("COMPLETED").hmacKeyVersion(version)
                .schemaName(SCHEMA).lastProcessedId(99L).processedCount(10L).build();
        org.mockito.Mockito.when(jobs.findBySchemaNameAndJobKind(org.mockito.ArgumentMatchers.eq(SCHEMA), org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> {
                    String kind = invocation.getArgument(1);
                    if ("NAME_PREFIX_DIGESTS".equals(kind)) {
                        return java.util.Optional.of(completedPrefix);
                    }
                    return java.util.Optional.of(com.smart.therapy.flow.common.entity.PlatformPhiBlindIndexBackfillJob.builder()
                            .jobKind(kind).status("COMPLETED").hmacKeyVersion(version).build());
                });
        jdbc.update("DELETE FROM " + SCHEMA + ".client_name_blind_indexes WHERE client_id = ?", targetId);
        assertThat(clients.findAll(search.clientSearchSpecification("Jane"))).isEmpty();

        org.springframework.test.util.ReflectionTestUtils.invokeMethod(scheduler, "seedMissingJobs");

        assertThat(completedPrefix.getStatus()).isEqualTo("PENDING");
        assertThat(completedPrefix.getLastProcessedId()).isZero();
        org.mockito.Mockito.verify(jobs).save(completedPrefix);
    }

    @org.junit.jupiter.api.Test
    void completedNamePrefixJobReopensWhenNewerClientIdsExist() {
        var jobs = org.mockito.Mockito.mock(com.smart.therapy.flow.common.repository.PlatformPhiBlindIndexBackfillJobRepository.class);
        var organisations = org.mockito.Mockito.mock(com.smart.therapy.flow.organisation.repository.OrganisationRepository.class);
        var health = org.mockito.Mockito.mock(com.smart.therapy.flow.organisation.service.TenantSchemaHealthService.class);
        var scheduler = new com.smart.therapy.flow.common.service.PhiBlindIndexBackfillScheduler(
                jobs, organisations, health, encryption, indexes, jdbc, null);
        var organisation = new com.smart.therapy.flow.organisation.entity.Organisation();
        organisation.setId(com.smart.therapy.flow.common.tenant.TenantContext.getOrganisationId());
        organisation.setSchemaName(SCHEMA);
        org.mockito.Mockito.when(organisations.findAll()).thenReturn(List.of(organisation));
        org.mockito.Mockito.when(health.schemaExists(SCHEMA)).thenReturn(true);
        String version = org.springframework.test.util.ReflectionTestUtils.invokeMethod(scheduler, "computeHmacKeyVersion");
        long maxId = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM " + SCHEMA + ".clients", Long.class);
        var completedPrefix = com.smart.therapy.flow.common.entity.PlatformPhiBlindIndexBackfillJob.builder()
                .jobKind("NAME_PREFIX_DIGESTS").status("COMPLETED").hmacKeyVersion(version)
                .schemaName(SCHEMA).lastProcessedId(Math.max(0L, maxId - 1)).processedCount(10L).build();
        org.mockito.Mockito.when(jobs.findBySchemaNameAndJobKind(org.mockito.ArgumentMatchers.eq(SCHEMA), org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> {
                    String kind = invocation.getArgument(1);
                    if ("NAME_PREFIX_DIGESTS".equals(kind)) {
                        return java.util.Optional.of(completedPrefix);
                    }
                    return java.util.Optional.of(com.smart.therapy.flow.common.entity.PlatformPhiBlindIndexBackfillJob.builder()
                            .jobKind(kind).status("COMPLETED").hmacKeyVersion(version).build());
                });

        org.springframework.test.util.ReflectionTestUtils.invokeMethod(scheduler, "seedMissingJobs");

        assertThat(completedPrefix.getStatus()).isEqualTo("PENDING");
        assertThat(completedPrefix.getLastProcessedId()).isZero();
        org.mockito.Mockito.verify(jobs).save(completedPrefix);
    }

    @org.junit.jupiter.api.Test
    void clientDigestBackfillSurvivesDuplicateMrnWithoutAbortingBatch() {
        Client seeded = clients.findById(targetId).orElseThrow();
        var therapist = seeded.getAssignedTherapist();
        Client first = TestDataFactory.createTestClient(therapist);
        first.setFullName("Amin One");
        first.setClientId("CL-2026-0400");
        indexes.updateBlindIndexes(first, List.of());
        em.persistAndFlush(first);
        Client duplicate = TestDataFactory.createTestClient(therapist);
        duplicate.setFullName("Amin Hammoud");
        duplicate.setClientId("CL-2026-0400"); // same MRN plaintext → same blind digest
        em.persistAndFlush(duplicate);
        // Clear digests so CLIENT_DIGESTS must write them (MRN write will collide).
        jdbc.update("UPDATE " + SCHEMA + ".clients SET client_id_blind_idx = NULL, full_name_blind_idx = NULL "
                + "WHERE id IN (?, ?)", first.getId(), duplicate.getId());
        em.clear();

        var scheduler = new com.smart.therapy.flow.common.service.PhiBlindIndexBackfillScheduler(
                org.mockito.Mockito.mock(com.smart.therapy.flow.common.repository.PlatformPhiBlindIndexBackfillJobRepository.class),
                org.mockito.Mockito.mock(com.smart.therapy.flow.organisation.repository.OrganisationRepository.class),
                org.mockito.Mockito.mock(com.smart.therapy.flow.organisation.service.TenantSchemaHealthService.class),
                encryption, indexes, jdbc, null);
        var job = com.smart.therapy.flow.common.entity.PlatformPhiBlindIndexBackfillJob.builder()
                .schemaName(SCHEMA)
                .organisationId(com.smart.therapy.flow.common.tenant.TenantContext.getOrganisationId())
                .jobKind("CLIENT_DIGESTS")
                .status("PENDING")
                .lastProcessedId(Math.min(first.getId(), duplicate.getId()) - 1)
                .processedCount(0L)
                .build();

        int processed = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                scheduler, "backfillClientDigests", job, 50);

        assertThat(processed).isGreaterThanOrEqualTo(2);
        assertThat(job.getLastProcessedId()).isGreaterThanOrEqualTo(Math.max(first.getId(), duplicate.getId()));
        // Both rows remain writable after the duplicate; at least one keeps the MRN digest,
        // and the colliding row still receives a full-name digest via savepoint retry.
        Integer withName = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + SCHEMA + ".clients WHERE id IN (?, ?) AND full_name_blind_idx IS NOT NULL",
                Integer.class, first.getId(), duplicate.getId());
        assertThat(withName).isEqualTo(2);
        // Transaction must still be usable after the duplicate collision.
        assertThat(jdbc.queryForObject("SELECT 1", Integer.class)).isEqualTo(1);
    }

    static Stream<Arguments> supported() {
        return Stream.of(
            Arguments.of("DOB ISO", "1988-04-19"),
            Arguments.of("DOB month/day/year", "04/19/1988"),
            Arguments.of("partial first name", "Jan"),
            Arguments.of("partial last name", "Do"),
            Arguments.of("multiple name prefixes", "Ja Do"),
            Arguments.of("full name", "Jane Doe"),
            Arguments.of("first name", "Jane"),
            Arguments.of("last name", "Doe"),
            Arguments.of("case and whitespace", "  JANE   DOE  "),
            Arguments.of("reordered name tokens", "Doe Jane"),
            Arguments.of("full email", "jane.doe@example.com"),
            Arguments.of("email case and whitespace", " JANE.DOE@EXAMPLE.COM "),
            Arguments.of("international phone", "+15195550123"),
            Arguments.of("national phone", "5195550123"),
            Arguments.of("formatted phone", "(519) 555-0123"),
            Arguments.of("formatted international phone", "+1 (519) 555-0123"),
            Arguments.of("work phone", "5195550124"),
            Arguments.of("full MRN", "CL-2026-0395"),
            Arguments.of("MRN case", "cl-2026-0395"),
            Arguments.of("MRN whitespace", "CL 2026 0395"),
            Arguments.of("MRN without prefix", "2026-0395"),
            Arguments.of("MRN prefix", "CL-2026"),
            Arguments.of("MRN middle fragment", "2026-03"),
            Arguments.of("MRN sequence fragment", "0395")
        );
    }

    @ParameterizedTest(name = "Supported: {0}")
    @MethodSource("supported")
    void supportedInputFindsOnlyMatchingClient(String kind, String input) {
        assertThat(clients.findAll(search.clientSearchSpecification(input)))
                .as(kind).extracting(Client::getId).containsExactly(targetId);
    }

    static Stream<Arguments> unsupported() {
        return Stream.of(
            Arguments.of("DOB day/month/year is not supported", "19/04/1988"),
            Arguments.of("email local part is not supported", "jane.doe"),
            Arguments.of("email prefix is not supported", "jane.doe@"),
            Arguments.of("email domain is not supported", "example.com"),
            Arguments.of("phone last four digits searches MRNs, not phones", "0123"),
            Arguments.of("combined name and email is not supported", "Jane jane.doe@example.com")
        );
    }

    @ParameterizedTest(name = "Known gap: {0}")
    @MethodSource("unsupported")
    void recordsCurrentUnsupportedInputs(String gap, String input) {
        assertThat(clients.findAll(search.clientSearchSpecification(input))).as(gap).isEmpty();
    }

    static Stream<String> absent() {
        return Stream.of("1988-02-30", "04/05/1988", "J", "Jane Ro", "Nobody Here", "nobody@example.com", "5195550999", "CL-2099-9999", "removed@example.com");
    }

    @ParameterizedTest(name = "No match: {0}")
    @MethodSource("absent")
    void unmatchedOrDeletedContactDoesNotReturnClients(String input) {
        assertThat(clients.findAll(search.clientSearchSpecification(input))).isEmpty();
    }
}

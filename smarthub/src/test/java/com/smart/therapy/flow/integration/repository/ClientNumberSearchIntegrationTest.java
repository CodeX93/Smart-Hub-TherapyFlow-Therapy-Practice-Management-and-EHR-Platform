package com.smart.therapy.flow.integration.repository;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientReferral;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.service.ClientSearchHelper;
import com.smart.therapy.flow.common.BaseTenantRepositoryTest;
import com.smart.therapy.flow.common.TestDataFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@Import(ClientSearchHelper.class)
class ClientNumberSearchIntegrationTest extends BaseTenantRepositoryTest {
    @Autowired private TestEntityManager entityManager;
    @Autowired private ClientRepository clients;
    @Autowired private ClientSearchHelper search;
    private Long matchingId;
    private Long referralId;

    @BeforeEach
    void fixtures() {
        var therapist = persistTherapist();
        Client matching = TestDataFactory.createTestClient(therapist);
        matching.setClientId("CL-2026-0395");
        entityManager.persistAndFlush(matching);
        matchingId = matching.getId();
        Client referral = TestDataFactory.createTestClient(therapist);
        referral.setClientId("CL-2025-0100");
        entityManager.persistAndFlush(referral);
        referralId = referral.getId();
        entityManager.persistAndFlush(ClientReferral.builder().client(referral)
                .referenceNumber("CL-2026-0395").build());
        Client deleted = TestDataFactory.createTestClient(therapist);
        deleted.setClientId("CL-2026-03950");
        deleted.setIsDeleted(true);
        entityManager.persistAndFlush(deleted);
        entityManager.clear();
    }

    @Test
    void partialEncryptedNumbersMatchWithoutHydratingProfiles() {
        var stats = entityManager.getEntityManager().getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
        boolean enabled = stats.isStatisticsEnabled();
        stats.setStatisticsEnabled(true);
        stats.clear();
        try {
            assertThat(search.findClientIdsMatchingMrn("0395"))
                    .containsExactlyInAnyOrder(matchingId, referralId);
            assertThat(stats.getQueryExecutionCount()).isEqualTo(1);
            assertThat(stats.getEntityLoadCount()).isZero();
            assertThat(stats.getCollectionLoadCount()).isZero();
        } finally {
            stats.setStatisticsEnabled(enabled);
        }
    }

    @Test
    void contentAndCountShareScanAndPreserveAdditionalClientScope() {
        var stats = entityManager.getEntityManager().getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
        boolean enabled = stats.isStatisticsEnabled();
        stats.setStatisticsEnabled(true);
        stats.clear();
        try {
            var specification = search.clientSearchSpecification("2026-03");
            assertThat(stats.getQueryExecutionCount()).isEqualTo(1);
            assertThat(clients.count(specification)).isEqualTo(2);
            assertThat(clients.count(specification.and((root, query, cb) ->
                    cb.equal(root.get("id"), matchingId)))).isEqualTo(1);
            // One narrow candidate scan, then two counts; no repeat scan per predicate.
            assertThat(stats.getQueryExecutionCount()).isEqualTo(3);
            assertThat(stats.getEntityLoadCount()).isZero();
        } finally {
            stats.setStatisticsEnabled(enabled);
        }
    }
}

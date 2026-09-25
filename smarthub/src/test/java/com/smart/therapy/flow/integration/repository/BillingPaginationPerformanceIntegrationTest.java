package com.smart.therapy.flow.integration.repository;

import com.smart.therapy.flow.billing.entity.SessionBilling;
import com.smart.therapy.flow.billing.repository.SessionBillingRepository;
import com.smart.therapy.flow.billing.service.BillingService;
import com.smart.therapy.flow.common.BaseTenantRepositoryTest;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.tenant.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real PostgreSQL workload: missing lookup predicates must not turn paging into repeated full scans. */
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS)
class BillingPaginationPerformanceIntegrationTest extends BaseTenantRepositoryTest {
    @Autowired TestEntityManager em;
    @Autowired SessionBillingRepository bills;
    private com.smart.therapy.flow.auth.entity.User therapist;

    @BeforeEach void identity() { therapist = persistTherapist(); }

    @Test
    void unfilteredPageRemainsBoundedWithLargeMigrationHistory() {
        var client = em.persistAndFlush(TestDataFactory.createTestClient(therapist));
        var catalog = em.persistAndFlush(com.smart.therapy.flow.billing.entity.Service.builder()
                .serviceCode("paging").serviceName("Paging fixture").duration(60).baseRate(BigDecimal.TEN).build());
        var entityManager = em.getEntityManager();
        entityManager.unwrap(org.hibernate.Session.class).doWork(connection ->
                new ResourceDatabasePopulator(new ClassPathResource("db/migration/V91__clienthub_migration_control.sql")).populate(connection));
        entityManager.createNativeQuery("""
                INSERT INTO sessions (session_date,session_mode,session_type,status,client_id,therapist_id,service_id,
                  insurance_applicable,createdat,updatedat,created_by,updated_by,is_deleted,version)
                SELECT now(),'online','General','completed',:client,:therapist,:service,false,now(),now(),0,0,false,0
                FROM generate_series(1,4000)
                """).setParameter("client", client.getId()).setParameter("therapist", therapist.getId())
                .setParameter("service", catalog.getId()).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT INTO session_billing (session_id,service_code,rate_per_unit,units,subtotal_amount,total_amount,
                  paid_amount,client_paid_amount,insurance_paid_amount,outstanding_amount,insurance_covered,billing_status,
                  billing_date,createdat,updatedat,created_by,updated_by,is_deleted,version)
                SELECT id,'paging',10,1,10,10,0,0,0,10,false,'PENDING',DATE '2026-09-01',now(),now(),0,0,false,0
                FROM sessions WHERE client_id=:client
                """).setParameter("client", client.getId()).executeUpdate();
        Long firstId = ((Number) entityManager.createNativeQuery("SELECT min(id) FROM session_billing").getSingleResult()).longValue();
        entityManager.createNativeQuery("""
                INSERT INTO public.clienthub_legacy_id_mappings (organisation_id,entity_name,source_id,target_schema,target_table,target_id)
                SELECT :org,'session_billing',CAST(1000000-id AS text),:schema,'session_billing',id FROM session_billing
                UNION ALL
                SELECT :org,'session_billing',CAST(2000000+g AS text),:schema,'session_billing',2000000+g FROM generate_series(1,100000) g
                """).setParameter("org", TenantContext.getOrganisationId()).setParameter("schema", SCHEMA).executeUpdate();
        // The same numeric target id in another schema must not affect this tenant's order.
        entityManager.createNativeQuery("""
                INSERT INTO public.clienthub_legacy_id_mappings (organisation_id,entity_name,source_id,target_schema,target_table,target_id)
                VALUES (:org,'session_billing','9000000','different_schema','session_billing',:target)
                """).setParameter("org", TenantContext.getOrganisationId()).setParameter("target", firstId + 3999).executeUpdate();
        entityManager.createNativeQuery("ANALYZE public.clienthub_legacy_id_mappings").executeUpdate();
        entityManager.createNativeQuery("ANALYZE session_billing").executeUpdate();
        entityManager.createNativeQuery("SET LOCAL statement_timeout = '5s'").executeUpdate();
        var service = mock(BillingService.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        ReflectionTestUtils.setField(service, "sessionBillingRepository", bills);
        Specification<SessionBilling> all = (root, query, cb) -> cb.conjunction();
        var result = new AtomicReference<Page<SessionBilling>>();
        long started = System.nanoTime();
        assertThatCode(() -> result.set(ReflectionTestUtils.invokeMethod(service, "findBillingRecordsClientHubOrder", all,
                PageRequest.of(0,10,Sort.by("billingDate").descending()))))
                .as("A ten-row page must not scan the complete migration history for every bill")
                .doesNotThrowAnyException();
        System.out.println("BILLING_PAGING_MS=" + (System.nanoTime()-started)/1_000_000);
        assertThat(result.get().getTotalElements()).isEqualTo(4000);
        assertThat(result.get().getContent()).hasSize(10);
        assertThat(result.get().getContent().get(0).getId()).isEqualTo(firstId);
        assertThat(result.get().getContent().get(9).getId()).isEqualTo(firstId+9);
    }
}

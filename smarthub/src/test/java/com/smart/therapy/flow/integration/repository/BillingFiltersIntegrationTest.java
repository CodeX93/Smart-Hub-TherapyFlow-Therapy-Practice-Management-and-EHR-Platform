package com.smart.therapy.flow.integration.repository;

import com.smart.therapy.flow.common.BaseTenantRepositoryTest;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.billing.entity.SessionBilling;
import com.smart.therapy.flow.billing.entity.Payment;
import com.smart.therapy.flow.billing.enums.*;
import com.smart.therapy.flow.billing.repository.SessionBillingRepository;
import com.smart.therapy.flow.billing.service.BillingService;
import com.smart.therapy.flow.client.enums.ClientType;
import com.smart.therapy.flow.client.service.ClientSearchHelper;
import com.smart.therapy.flow.common.service.BlindIndexService;
import com.smart.therapy.flow.system.service.PracticeConfigurationService;
import com.smart.therapy.flow.system.dto.PracticeConfigurationResponse;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Import(ClientSearchHelper.class)
@org.springframework.test.context.TestPropertySource(properties = "app.phi-encryption.search.mode=blind_only")
class BillingFiltersIntegrationTest extends BaseTenantRepositoryTest {
    @Autowired TestEntityManager em;
    @Autowired SessionBillingRepository bills;
    @Autowired ClientSearchHelper search;
    @Autowired BlindIndexService indexes;

    private com.smart.therapy.flow.auth.entity.User therapist;
    @org.junit.jupiter.api.BeforeEach
    void identity() { therapist = persistTherapist(); }

    static Stream<Arguments> cases() {
        var cases = new ArrayList<Arguments>();
        for (String s : List.of("pending","partial","paid","denied","refunded")) cases.add(Arguments.of("status",s));
        for (String s : List.of("pending","partial","paid","failed","refunded")) cases.add(Arguments.of("paymentStatus",s));
        for (String s : List.of("cash","check","credit_card","debit_card","insurance","bank_transfer","online_payment","credit_balance")) cases.add(Arguments.of("paymentMethod",s));
        for (String s : List.of("individual","couple","family","group","refugee","mva")) cases.add(Arguments.of("clientType",s));
        for (String s : List.of("in_person","virtual","telehealth","online")) cases.add(Arguments.of("sessionType",s));
        for (String s : List.of("clientId","therapistId","serviceCode","startDate","endDate","minAmount","maxAmount","combined","reset")) cases.add(Arguments.of(s,""));
        for (String s : List.of("Jane","Ja Do","1988-04-19","CL-2026-0395","jane@example.com","+15195550123")) cases.add(Arguments.of("clientSearch",s));
        return cases.stream();
    }

    @ParameterizedTest(name="billing filter {0}={1}")
    @MethodSource("cases")
    void filterSelectsExpectedRows(String field, String value) {
        var catalogService = em.persistAndFlush(com.smart.therapy.flow.billing.entity.Service.builder()
                .serviceCode("fixture").serviceName("Fixture").duration(60).baseRate(BigDecimal.valueOf(100)).build());
        var client = TestDataFactory.createTestClient(therapist);
        client.setFullName("Jane Doe"); client.setClientId("CL-2026-0395");
        client.setDateOfBirth(LocalDate.of(1988,4,19));
        client.setClientType(field.equals("clientType") ? value : "individual");
        indexes.updateBlindIndexes(client,List.of()); em.persistAndFlush(client);
        for (var contact : List.of(
                com.smart.therapy.flow.client.entity.ClientContact.builder().client(client).contactType(com.smart.therapy.flow.client.enums.ContactType.EMAIL).contactValue("jane@example.com").build(),
                com.smart.therapy.flow.client.entity.ClientContact.builder().client(client).contactType(com.smart.therapy.flow.client.enums.ContactType.PHONE).contactValue("+15195550123").build())) {
            indexes.applyContactBlindIndex(contact); em.persistAndFlush(contact);
        }
        var session = TestDataFactory.createTestSession(client,therapist);
        session.setService(catalogService);
        session.setSessionDate(Instant.parse("2026-09-12T04:00:00Z"));
        session.setSessionType(field.equals("sessionType") && !value.equals("in_person") ? "online" : "in-person"); em.persistAndFlush(session);
        var bill = SessionBilling.builder().ratePerUnit(new BigDecimal("100")).subtotalAmount(new BigDecimal("100")).outstandingAmount(new BigDecimal("100")).session(session).serviceCode("TARGET").totalAmount(new BigDecimal("100.00")).billingDate(LocalDate.of(2026,9,20)).billingStatus(field.equals("status") && !List.of("refunded","failed").contains(value) ? BillingStatus.valueOf(value.toUpperCase()) : BillingStatus.PENDING).build();
        if (field.equals("status") && value.equals("partial")) {
            bill.setBillingStatus(BillingStatus.BILLED);
            bill.setPaidAmount(new BigDecimal("20"));
        }
        if (field.equals("paymentStatus")) {
            bill.setPaidAmount(value.equals("paid") ? new BigDecimal("100") : value.equals("partial") ? new BigDecimal("20") : BigDecimal.ZERO);
        }
        em.persistAndFlush(bill);
        var otherClient = TestDataFactory.createTestClient(therapist); otherClient.setFullName("Other Client"); otherClient.setClientType(value.equals("group") ? "individual" : "group"); em.persistAndFlush(otherClient);
        var otherSession = TestDataFactory.createTestSession(otherClient,therapist);
        otherSession.setService(catalogService);
        otherSession.setSessionDate(field.equals("endDate") ? Instant.parse("2026-09-13T04:00:00Z") : Instant.parse("2026-09-12T03:59:59Z"));
        otherSession.setSessionType(session.getSessionType().equals("online") ? "in-person" : "online"); em.persistAndFlush(otherSession);
        var other = SessionBilling.builder().ratePerUnit(new BigDecimal("100")).subtotalAmount(new BigDecimal("100")).outstandingAmount(new BigDecimal("100")).session(otherSession).serviceCode("OTHER").totalAmount(new BigDecimal(field.equals("maxAmount") ? "200" : "50")).billingStatus(field.equals("paymentStatus") ? BillingStatus.DENIED : bill.getBillingStatus()==BillingStatus.PAID ? BillingStatus.PENDING : BillingStatus.PAID).build(); em.persistAndFlush(other);
        var method = field.equals("paymentMethod") ? PaymentMethod.fromValue(value) : PaymentMethod.CASH;
        em.persistAndFlush(Payment.builder().sessionBilling(bill).amount(BigDecimal.TEN).paymentMethod(method).status(value.equals("refunded") ? PaymentStatus.REFUNDED : value.equals("failed") ? PaymentStatus.FAILED : PaymentStatus.PAID).build());
        var service = mock(BillingService.class, CALLS_REAL_METHODS);
        var practice = mock(PracticeConfigurationService.class);
        when(practice.getPracticeConfiguration()).thenReturn(PracticeConfigurationResponse.builder().timezone("America/Toronto").build());
        ReflectionTestUtils.setField(service,"practiceConfigurationService",practice);
        ReflectionTestUtils.setField(service,"clientSearchHelper",search);
        boolean combined = field.equals("combined");
        Object[] args = { field.equals("clientId")||combined ? client.getId():null, field.equals("clientSearch")?value:null, field.equals("therapistId")?therapist.getId():null,
            field.equals("status")?BillingRecordStatusFilter.fromValue(value):null, field.equals("serviceCode")||combined?"TARGET":null,
            field.equals("clientType")?value:null, field.equals("sessionType")?value:null, field.equals("paymentMethod")?value:null,
            field.equals("startDate")||combined?LocalDate.of(2026,9,12):null, field.equals("endDate")||combined?LocalDate.of(2026,9,12):null,
            field.equals("minAmount")||combined?new BigDecimal("100"):null, field.equals("maxAmount")||combined?new BigDecimal("100"):null };
        if (field.equals("paymentStatus")) {
            args = Arrays.copyOf(args, 13); args[12] = value;
        }
        Specification<SessionBilling> spec = ReflectionTestUtils.invokeMethod(service,"buildBillingSpecification",args);
        var ids = bills.findAll(spec).stream().map(SessionBilling::getId).toList();
        if (field.equals("reset") || field.equals("therapistId")) assertThat(ids).containsExactlyInAnyOrder(bill.getId(),other.getId());
        else assertThat(ids).containsExactly(bill.getId());
        // Statistics must apply the identical predicate, including encrypted search and combinations.
        ReflectionTestUtils.setField(service, "sessionBillingRepository", bills);
        var scopes = mock(com.smart.therapy.flow.common.security.CaseloadScopeService.class);
        when(scopes.resolve(any())).thenReturn(new com.smart.therapy.flow.common.security.CaseloadScopeService.ResolvedCaseloadScope(
                com.smart.therapy.flow.common.security.CaseloadScope.ALL, List.of(), therapist.getId()));
        ReflectionTestUtils.setField(service, "caseloadScopeService", scopes);
        var principal = TestDataFactory.createAuthPrincipal(therapist);
        if (!field.equals("reset")) {
            var stats = service.getBillingStatistics(principal, (Long) args[0], (String) args[1], (Long) args[2],
                    field.equals("status") ? value : null, field.equals("paymentStatus") ? value : null,
                    (String) args[4], (String) args[5], (String) args[6], (String) args[7],
                    (LocalDate) args[8], (LocalDate) args[9], (BigDecimal) args[10], (BigDecimal) args[11]);
            assertThat(stats.getTotalBillingRecords()).isEqualTo(ids.size());
            assertThat(stats.getTotalCollected()).isEqualByComparingTo(
                    bills.findAll(spec).stream().map(b -> b.getPaidAmount() == null ? BigDecimal.ZERO : b.getPaidAmount())
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        if (field.equals("reset")) {
            other.setBillingDate(bill.getBillingDate()); em.persistAndFlush(other);
            em.getEntityManager().createNativeQuery("CREATE TABLE IF NOT EXISTS public.clienthub_legacy_id_mappings (id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, organisation_id bigint, entity_name varchar(120), source_id varchar(120), target_schema varchar(63), target_table varchar(120), target_id bigint)").executeUpdate();
            em.getEntityManager().createNativeQuery("INSERT INTO public.clienthub_legacy_id_mappings (organisation_id,entity_name,source_id,target_schema,target_table,target_id) VALUES (:org,'session_billing','9',:schema,'session_billing',:first),(:org,'session_billing','2',:schema,'session_billing',:second)")
                    .setParameter("org", com.smart.therapy.flow.common.tenant.TenantContext.getOrganisationId())
                    .setParameter("schema", SCHEMA).setParameter("first", bill.getId()).setParameter("second", other.getId()).executeUpdate();
            var bounded = mock(SessionBillingRepository.class, org.mockito.AdditionalAnswers.delegatesTo(bills));
            ReflectionTestUtils.setField(service, "sessionBillingRepository", bounded);
            ReflectionTestUtils.setField(service, "entityManager", em.getEntityManager());
            org.springframework.data.domain.Page<SessionBilling> page = ReflectionTestUtils.invokeMethod(service,
                    "findBillingRecordsClientHubOrder", spec,
                    org.springframework.data.domain.PageRequest.of(0, 1, org.springframework.data.domain.Sort.by("billingDate").descending()));
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent()).extracting(SessionBilling::getId).containsExactly(bill.getId());
            for (var direction : org.springframework.data.domain.Sort.Direction.values()) {
                for (int index = 0; index < 3; index++) {
                    org.springframework.data.domain.Page<SessionBilling> ordered = ReflectionTestUtils.invokeMethod(service,
                            "findBillingRecordsClientHubOrder", spec,
                            org.springframework.data.domain.PageRequest.of(index, 1, org.springframework.data.domain.Sort.by(direction, "billingDate")));
                    assertThat(ordered.getTotalElements()).isEqualTo(2);
                    if (index == 2) assertThat(ordered.getContent()).isEmpty();
                    else assertThat(ordered.getContent()).extracting(SessionBilling::getId).containsExactly(
                            (direction.isAscending() == (index == 0)) ? other.getId() : bill.getId());
                }
            }
            // Invalid and overflowing source ids must fall back to target ids, without cast errors.
            for (String invalid : java.util.List.of("not-a-number", "9223372036854775808", "9999999999999999999999999999999")) {
                em.getEntityManager().createNativeQuery("UPDATE public.clienthub_legacy_id_mappings SET source_id=:source WHERE organisation_id=:org")
                        .setParameter("source", invalid).setParameter("org", com.smart.therapy.flow.common.tenant.TenantContext.getOrganisationId()).executeUpdate();
                org.springframework.data.domain.Page<SessionBilling> fallback = ReflectionTestUtils.invokeMethod(service,
                        "findBillingRecordsClientHubOrder", spec,
                        org.springframework.data.domain.PageRequest.of(0, 2, org.springframework.data.domain.Sort.by("billingDate").descending()));
                assertThat(fallback.getContent()).extracting(SessionBilling::getId).containsExactly(other.getId(), bill.getId());
            }
            verify(bounded, never()).findAll(any(Specification.class));
        }
        if (field.equals("therapistId")) {
            args[2] = Long.MAX_VALUE;
            Specification<SessionBilling> absent = ReflectionTestUtils.invokeMethod(service,"buildBillingSpecification",args);
            assertThat(bills.findAll(absent)).isEmpty();
        }
    }
}

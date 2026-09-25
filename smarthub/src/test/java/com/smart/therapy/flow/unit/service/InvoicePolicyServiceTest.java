package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.billing.dto.InvoicePolicyRateResult;
import com.smart.therapy.flow.billing.dto.InvoicePolicyRequest;
import com.smart.therapy.flow.billing.dto.InvoicePolicyResponse;
import com.smart.therapy.flow.billing.entity.InvoicePolicy;
import com.smart.therapy.flow.billing.enums.InvoicePolicyPriceType;
import com.smart.therapy.flow.billing.repository.InvoicePolicyRepository;
import com.smart.therapy.flow.billing.repository.ServiceRepository;
import com.smart.therapy.flow.billing.service.InvoicePolicyService;
import com.smart.therapy.flow.billing.service.SessionAppointmentStatusMapper;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.service.TimezoneService;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.enums.SessionStatus;
import com.smart.therapy.flow.system.dto.OptionCategoryResponse;
import com.smart.therapy.flow.system.dto.SystemOptionResponse;
import com.smart.therapy.flow.system.service.SystemOptionResolverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("InvoicePolicyService Unit Tests")
class InvoicePolicyServiceTest {

    @Mock
    private InvoicePolicyRepository invoicePolicyRepository;
    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private SystemOptionResolverService systemOptionResolverService;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private TimezoneService timezoneService;

    @InjectMocks
    private InvoicePolicyService invoicePolicyService;

    private AuthPrincipal adminPrincipal;
    private User admin;

    @BeforeEach
    void setUp() {
        admin = TestDataFactory.createTestAdmin();
        admin.setId(10L);
        adminPrincipal = TestDataFactory.createAuthPrincipal(admin);
        when(currentUserService.requireCurrentUser(adminPrincipal)).thenReturn(admin);
        when(userRepository.findById(10L)).thenReturn(Optional.of(admin));
        when(auditLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(timezoneService.getPracticeTimezone()).thenReturn(java.time.ZoneOffset.UTC);
    }

    @Test
    @DisplayName("Should create invoice policy with extended fields")
    void shouldCreateInvoicePolicy() {
        InvoicePolicyRequest request = buildRequest();
        stubOptionValidation();

        when(invoicePolicyRepository.findByScope("refugee", "show_up", 5L)).thenReturn(Optional.empty());
        when(invoicePolicyRepository.save(any(InvoicePolicy.class))).thenAnswer(invocation -> {
            InvoicePolicy saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(serviceRepository.findById(5L)).thenReturn(Optional.of(new com.smart.therapy.flow.billing.entity.Service()));

        InvoicePolicyResponse response = invoicePolicyService.createPolicy(request, adminPrincipal, "127.0.0.1");

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getServiceId()).isEqualTo(5L);
        assertThat(response.getPriority()).isEqualTo(20);
        verify(invoicePolicyRepository).save(any(InvoicePolicy.class));
    }

    @Test
    @DisplayName("Should reject duplicate policy scope")
    void shouldRejectDuplicatePolicyScope() {
        InvoicePolicyRequest request = buildRequest();
        stubOptionValidation();
        InvoicePolicy existing = InvoicePolicy.builder().build();
        existing.setId(99L);
        when(invoicePolicyRepository.findByScope("refugee", "show_up", 5L))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> invoicePolicyService.createPolicy(request, adminPrincipal, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should resolve fixed policy rate for completed session")
    void shouldResolveFixedPolicyRate() {
        Client client = TestDataFactory.createTestClientWithId(1L);
        client.setClientType("refugee");
        Session session = TestDataFactory.createTestSession(client, admin);
        session.setStatus("completed");
        com.smart.therapy.flow.billing.entity.Service serviceEntity = com.smart.therapy.flow.billing.entity.Service.builder()
                .baseRate(new BigDecimal("150.00"))
                .build();
        serviceEntity.setId(5L);
        session.setService(serviceEntity);

        InvoicePolicy policy = InvoicePolicy.builder()
                .enabled(true)
                .priceType(InvoicePolicyPriceType.FIXED)
                .invoicePrice(new BigDecimal("50.00"))
                .build();
        policy.setId(7L);

        when(invoicePolicyRepository.findEnabledMatches(any(), any(), eq(5L), any()))
                .thenReturn(List.of(policy));

        InvoicePolicyRateResult result = invoicePolicyService.resolveBillingRate(session, new BigDecimal("150.00"));

        assertThat(result.isPolicyApplied()).isTrue();
        assertThat(result.getRatePerUnit()).isEqualByComparingTo("50.00");
        assertThat(result.getInvoicePolicyId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("Should map completed session to show-up appointment status keys")
    void shouldMapCompletedSessionToShowUpKeys() {
        Set<String> keys = SessionAppointmentStatusMapper.toPolicyAppointmentStatusKeys(SessionStatus.COMPLETED);
        assertThat(keys).contains("completed", "show-up");

        Set<String> stringKeys = SessionAppointmentStatusMapper.toPolicyAppointmentStatusKeys("completed");
        assertThat(stringKeys).contains("completed", "show-up");
    }

    @Test
    @DisplayName("Should fallback to base rate when no policy matches")
    void shouldFallbackToBaseRateWhenNoPolicy() {
        Session session = TestDataFactory.createTestSession();
        session.setStatus("no-show");

        when(invoicePolicyRepository.findEnabledMatches(any(), any(), any(), any())).thenReturn(List.of());

        InvoicePolicyRateResult result = invoicePolicyService.resolveBillingRate(session, new BigDecimal("120.00"));

        assertThat(result.isPolicyApplied()).isFalse();
        assertThat(result.getRatePerUnit()).isEqualByComparingTo("120.00");
    }

    @Test
    @DisplayName("Should resolve invoice policy date in the Administration timezone")
    void shouldResolvePolicyDateInPracticeTimezone() {
        Client client = TestDataFactory.createTestClientWithId(1L);
        client.setClientType("Refugee");
        Session session = TestDataFactory.createTestSession(client, admin);
        session.setStatus("completed");
        session.setSessionDate(java.time.Instant.parse("2026-09-11T02:30:00Z"));
        when(timezoneService.getPracticeTimezone()).thenReturn(java.time.ZoneId.of("America/Toronto"));
        when(invoicePolicyRepository.findEnabledMatches(any(), any(), any(), any())).thenReturn(List.of());

        invoicePolicyService.resolveBillingRate(session, new BigDecimal("120.00"));

        verify(invoicePolicyRepository).findEnabledMatches(
                any(), any(), any(), eq(java.time.LocalDate.of(2026, 9, 10)));
    }

    @Test
    @DisplayName("Should match policy when appointment status uses underscore catalog key")
    void shouldMatchPolicyForUnderscoreAppointmentStatusKey() {
        Client client = TestDataFactory.createTestClientWithId(1L);
        client.setClientType("Refugee");
        Session session = TestDataFactory.createTestSession(client, admin);
        session.setStatus("no-show");
        com.smart.therapy.flow.billing.entity.Service serviceEntity = com.smart.therapy.flow.billing.entity.Service.builder()
                .baseRate(new BigDecimal("150.00"))
                .build();
        serviceEntity.setId(5L);
        session.setService(serviceEntity);

        InvoicePolicy policy = InvoicePolicy.builder()
                .enabled(true)
                .priceType(InvoicePolicyPriceType.FIXED)
                .invoicePrice(new BigDecimal("75.00"))
                .build();
        policy.setId(8L);

        when(invoicePolicyRepository.findEnabledMatches(any(), any(), eq(5L), any()))
                .thenReturn(List.of(policy));

        InvoicePolicyRateResult result = invoicePolicyService.resolveBillingRate(
                session, new BigDecimal("150.00"), 5L, java.time.LocalDate.now());

        assertThat(result.isPolicyApplied()).isTrue();
        assertThat(result.getRatePerUnit()).isEqualByComparingTo("75.00");
    }

    @Test
    @DisplayName("Should expand client type keys for policy lookup")
    void shouldExpandClientTypeKeysForPolicyLookup() {
        Client client = TestDataFactory.createTestClientWithId(1L);
        client.setClientType("Refugee");
        Session session = TestDataFactory.createTestSession(client, admin);
        session.setStatus("completed");

        when(systemOptionResolverService.resolveOptionKey("client_type", "Refugee")).thenReturn("Refugee");
        when(invoicePolicyRepository.findEnabledMatches(any(), any(), any(), any())).thenReturn(List.of());

        invoicePolicyService.resolveBillingRate(session, new BigDecimal("100.00"), null, java.time.LocalDate.now());

        verify(invoicePolicyRepository).findEnabledMatches(
                org.mockito.ArgumentMatchers.argThat(keys -> keys.contains("refugee") && keys.contains("all")),
                org.mockito.ArgumentMatchers.argThat(keys -> keys.contains("all")),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("Should create wildcard policy for all client types, statuses, and services")
    void shouldCreateAllScopePolicy() {
        InvoicePolicyRequest request = new InvoicePolicyRequest();
        request.setClientTypeKey("all");
        request.setClientTypeLabel("All client types");
        request.setAppointmentStatusKey("all");
        request.setAppointmentStatusLabel("All session statuses");
        request.setEnabled(true);
        request.setPriceType(InvoicePolicyPriceType.PERCENTAGE);
        request.setInvoicePrice(new BigDecimal("90.00"));
        request.setServiceId(null);
        request.setServiceScopeKey("all");
        request.setPriority(1);
        request.setPolicyName("Global 10% off");

        when(invoicePolicyRepository.findByScope("all", "all", null)).thenReturn(Optional.empty());
        when(invoicePolicyRepository.save(any(InvoicePolicy.class))).thenAnswer(invocation -> {
            InvoicePolicy saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        InvoicePolicyResponse response = invoicePolicyService.createPolicy(request, adminPrincipal, "127.0.0.1");

        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getClientTypeKey()).isEqualTo("all");
        assertThat(response.getAppointmentStatusKey()).isEqualTo("all");
        assertThat(response.getServiceId()).isNull();
        assertThat(response.getServiceScopeKey()).isEqualTo("all");
    }

    @Test
    @DisplayName("Service options include All services first")
    void serviceOptionsIncludeAll() {
        com.smart.therapy.flow.billing.entity.Service service = com.smart.therapy.flow.billing.entity.Service.builder()
                .serviceCode("PSY-60")
                .serviceName("Psychotherapy 60")
                .baseRate(new BigDecimal("150.00"))
                .isActive(true)
                .build();
        service.setId(5L);
        when(serviceRepository.findByIsActive(true)).thenReturn(List.of(service));

        List<com.smart.therapy.flow.billing.dto.InvoicePolicyServiceOptionResponse> options =
                invoicePolicyService.getServiceOptions();

        assertThat(options.get(0).getOptionKey()).isEqualTo("all");
        assertThat(options.get(0).getServiceId()).isNull();
        assertThat(options.get(0).getAllServices()).isTrue();
        assertThat(options.get(1).getServiceId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Options lists include All wildcard first")
    void optionsIncludeAllWildcard() {
        when(systemOptionResolverService.resolveCategoryWithOptions("client_type"))
                .thenReturn(OptionCategoryResponse.builder()
                        .options(List.of(SystemOptionResponse.builder().optionKey("refugee").optionLabel("Refugee").build()))
                        .build());
        when(systemOptionResolverService.resolveCategoryWithOptions("session_status"))
                .thenReturn(OptionCategoryResponse.builder()
                        .options(List.of(SystemOptionResponse.builder().optionKey("completed").optionLabel("Completed").build()))
                        .build());

        List<SystemOptionResponse> clientTypes = invoicePolicyService.getClientTypeOptions();
        List<SystemOptionResponse> statuses = invoicePolicyService.getAppointmentStatusOptions();

        assertThat(clientTypes.get(0).getOptionKey()).isEqualTo("all");
        assertThat(statuses.get(0).getOptionKey()).isEqualTo("all");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"negative", "percentage", "dates"})
    void invalidPolicyBoundariesCannotBeSaved(String invalid) {
        var request = buildRequest();
        switch (invalid) {
            case "negative" -> request.setInvoicePrice(new BigDecimal("-0.01"));
            case "percentage" -> { request.setPriceType(InvoicePolicyPriceType.PERCENTAGE); request.setInvoicePrice(new BigDecimal("100.01")); }
            default -> { request.setEffectiveFrom(java.time.LocalDate.of(2026, 9, 2)); request.setEffectiveTo(java.time.LocalDate.of(2026, 9, 1)); }
        }
        assertThatThrownBy(() -> invoicePolicyService.createPolicy(request, adminPrincipal, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class);
        verify(invoicePolicyRepository, org.mockito.Mockito.never()).save(any());
    }

    private InvoicePolicyRequest buildRequest() {
        InvoicePolicyRequest request = new InvoicePolicyRequest();
        request.setClientTypeKey("refugee");
        request.setClientTypeLabel("Refugee");
        request.setAppointmentStatusKey("show-up");
        request.setAppointmentStatusLabel("Show-up");
        request.setEnabled(true);
        request.setPriceType(InvoicePolicyPriceType.FIXED);
        request.setInvoicePrice(new BigDecimal("50.00"));
        request.setServiceId(5L);
        request.setPriority(20);
        request.setPolicyName("Refugee show-up fixed");
        return request;
    }

    private void stubOptionValidation() {
        OptionCategoryResponse clientTypes = OptionCategoryResponse.builder()
                .options(List.of(SystemOptionResponse.builder().optionKey("refugee").optionLabel("Refugee").build()))
                .build();
        OptionCategoryResponse appointmentStatuses = OptionCategoryResponse.builder()
                .options(List.of(SystemOptionResponse.builder().optionKey("show-up").optionLabel("Show-up").build()))
                .build();
        when(systemOptionResolverService.resolveCategoryWithOptions("client_type")).thenReturn(clientTypes);
        when(systemOptionResolverService.resolveCategoryWithOptions("session_status")).thenReturn(appointmentStatuses);
    }
}

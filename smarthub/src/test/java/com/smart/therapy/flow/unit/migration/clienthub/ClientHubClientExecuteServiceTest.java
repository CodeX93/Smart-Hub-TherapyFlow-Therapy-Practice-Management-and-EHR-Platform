package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientAddressRepository;
import com.smart.therapy.flow.client.repository.ClientContactRepository;
import com.smart.therapy.flow.client.repository.ClientEmploymentRepository;
import com.smart.therapy.flow.client.repository.ClientInsuranceRepository;
import com.smart.therapy.flow.client.repository.ClientReferralRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.service.BlindIndexService;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceClientRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientHubClientExecuteServiceTest {

    private TenantTransactionExecutor tenantTx;
    private JdbcTemplate jdbc;
    private ClientRepository clients;
    private BlindIndexService blindIndexes;
    private ClientHubClientExecuteService service;

    @BeforeEach
    void setUp() {
        tenantTx = mock(TenantTransactionExecutor.class);
        jdbc = mock(JdbcTemplate.class);
        clients = mock(ClientRepository.class);
        blindIndexes = mock(BlindIndexService.class);
        when(tenantTx.executeWrite(anyLong(), anyString(), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Supplier<Object> work = invocation.getArgument(2);
            return work.get();
        });
        service = new ClientHubClientExecuteService(
                tenantTx,
                jdbc,
                clients,
                mock(UserRepository.class),
                mock(ClientContactRepository.class),
                mock(ClientAddressRepository.class),
                mock(ClientInsuranceRepository.class),
                mock(ClientReferralRepository.class),
                mock(ClientEmploymentRepository.class),
                blindIndexes);
    }

    @Test
    void refusesToExecuteWithoutResolvedTargetOrganisation() {
        assertThatThrownBy(() -> service.execute(List.of(), new TargetInventory(0, 0, null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Exactly one target organisation");
    }

    @Test
    void keepsPastDateOfBirth() {
        assertThat(service.sanitizeDateOfBirth(clientWithDob(LocalDate.of(1990, 5, 1))))
                .isEqualTo(LocalDate.of(1990, 5, 1));
    }

    @Test
    void nullsTodayOrFutureDateOfBirth() {
        assertThat(service.sanitizeDateOfBirth(clientWithDob(LocalDate.now()))).isNull();
        assertThat(service.sanitizeDateOfBirth(clientWithDob(LocalDate.now().plusDays(1)))).isNull();
    }

    @Test
    void nullsFutureReferralDatesOnly() {
        assertThat(service.sanitizePastOrPresentDate(LocalDate.now(), "referral_date", "10"))
                .isEqualTo(LocalDate.now());
        assertThat(service.sanitizePastOrPresentDate(LocalDate.now().plusDays(1), "referral_date", "10"))
                .isNull();
    }

    @Test
    void alreadyMappedClientStillRefreshesNameTokenBlindIndexes() {
        TargetInventory target = new TargetInventory(1, 0, 7L, "tenant_real");
        SourceClientRecord source = clientRecord("10", "CL-2026-0398", "Amin Hammoud");
        Client mapped = Client.builder()
                .clientId("CL-2026-0398")
                .fullName("Amin Hammoud")
                .build();
        mapped.setId(55L);

        when(jdbc.query(anyString(), any(RowMapper.class), eq(7L), eq("clients"), eq("10")))
                .thenReturn(List.of(55L));
        when(clients.findById(55L)).thenReturn(Optional.of(mapped));
        when(clients.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jdbc.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);

        var result = service.execute(List.of(source), target);

        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.created()).isZero();
        verify(blindIndexes).updateBlindIndexes(mapped, null);
        verify(clients).save(mapped);
        verify(clients, never()).findByClientIdBlindIdxAndIsDeletedFalse(any());
    }

    private SourceClientRecord clientWithDob(LocalDate dob) {
        return clientRecord("10", "CL-2026-0001", "Test Client", dob);
    }

    private SourceClientRecord clientRecord(String legacyPk, String mrn, String fullName) {
        return clientRecord(legacyPk, mrn, fullName, LocalDate.of(1990, 5, 1));
    }

    private SourceClientRecord clientRecord(String legacyPk, String mrn, String fullName, LocalDate dob) {
        return new SourceClientRecord(
                legacyPk,
                mrn,
                fullName,
                dob,
                null, null, null, null, null, null, null, null, null, null, null,
                Instant.parse("2026-01-01T00:00:00Z"),
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null);
    }
}

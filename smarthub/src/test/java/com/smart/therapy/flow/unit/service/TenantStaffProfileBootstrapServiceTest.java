package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.organisation.repository.UserOrganisationRepository;
import com.smart.therapy.flow.organisation.service.TenantStaffProfileBootstrapService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantStaffProfileBootstrapServiceTest {

    @Mock private UserOrganisationRepository userOrganisationRepository;
    @Mock private AuthIdentityRepository authIdentityRepository;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private TenantTransactionExecutor tenantTransactionExecutor;
    @InjectMocks private TenantStaffProfileBootstrapService service;

    @SuppressWarnings("unchecked")
    private void runExecuteWriteInline() {
        when(tenantTransactionExecutor.executeWrite(anyLong(), anyString(), any(Supplier.class)))
                .thenAnswer(inv -> ((Supplier<Integer>) inv.getArgument(2)).get());
    }

    private void stubTenantSchema(Long authId) {
        when(jdbcTemplate.queryForObject(contains("information_schema.tables"), eq(Integer.class), eq("tenant_7")))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("auth_id = ?"), eq(Integer.class), eq(authId)))
                .thenReturn(0);
    }

    @Test
    void seedsAdminProfileWithOnboardingFullNameNotEmailLocalPart() {
        AuthIdentity identity = AuthIdentity.builder()
                .id(11L)
                .loginIdentifier("qa+e2emuemcptm@smarthub.qa.local")
                .fullName("Quinn Auditor")
                .identityType(IdentityType.STAFF)
                .isActive(true)
                .build();
        when(userOrganisationRepository.findDistinctAuthIdsByOrganisationId(7L)).thenReturn(List.of(11L));
        when(authIdentityRepository.findAllById(List.of(11L))).thenReturn(List.of(identity));
        runExecuteWriteInline();
        stubTenantSchema(11L);

        int created = service.ensureStaffProfiles(7L, "tenant_7", "UTC");

        assertThat(created).isEqualTo(1);
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(contains("insert into tenant_7.users"), args.capture());
        Object[] values = args.getValue();
        // Insert column order: createdat, created_by, is_deleted(inline), updatedat, updated_by,
        // version(inline), email, full_name, is_active, status, auth_id
        assertThat(values[4]).isEqualTo("qa+e2emuemcptm@smarthub.qa.local");
        assertThat(values[5]).isEqualTo("Quinn Auditor");
    }

    @Test
    void fallsBackToEmailDerivedNameWhenIdentityHasNoFullName() {
        AuthIdentity identity = AuthIdentity.builder()
                .id(12L)
                .loginIdentifier("jane.doe@smarthub.qa.local")
                .fullName("   ")
                .identityType(IdentityType.STAFF)
                .isActive(true)
                .build();
        when(userOrganisationRepository.findDistinctAuthIdsByOrganisationId(7L)).thenReturn(List.of(12L));
        when(authIdentityRepository.findAllById(List.of(12L))).thenReturn(List.of(identity));
        runExecuteWriteInline();
        stubTenantSchema(12L);

        int created = service.ensureStaffProfiles(7L, "tenant_7", "UTC");

        assertThat(created).isEqualTo(1);
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(contains("insert into tenant_7.users"), args.capture());
        assertThat(args.getValue()[5]).isEqualTo("Jane Doe");
    }
}

package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.repository.AuthIdentityRoleRepository;
import com.smart.therapy.flow.superadmin.service.SuperAdminOrganisationQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuperAdminOrganisationPrimaryAdminTest {
    @Mock AuthIdentityRoleRepository authIdentityRoles;
    @InjectMocks SuperAdminOrganisationQueryService service;

    @Test
    void returnsTheFirstAdminTheQueryOrdered() {
        when(authIdentityRoles.findAdminEmailsByOrganisationId(7L))
                .thenReturn(List.of(" owner@clinic.test ", "second.admin@clinic.test"));
        assertThat(service.findPrimaryAdminEmail(7L)).contains("owner@clinic.test");
    }

    @Test
    void skipsIdentitiesWithoutAnEmail() {
        when(authIdentityRoles.findAdminEmailsByOrganisationId(7L))
                .thenReturn(java.util.Arrays.asList(null, "   ", "owner@clinic.test"));
        assertThat(service.findPrimaryAdminEmail(7L)).contains("owner@clinic.test");
    }

    @Test
    void isEmptyWhenTheOrganisationHasNoAdmin() {
        when(authIdentityRoles.findAdminEmailsByOrganisationId(7L)).thenReturn(List.of());
        assertThat(service.findPrimaryAdminEmail(7L)).isEmpty();
    }

    @Test
    void doesNotQueryWithoutAnOrganisationId() {
        assertThat(service.findPrimaryAdminEmail(null)).isEmpty();
        verifyNoInteractions(authIdentityRoles);
    }
}

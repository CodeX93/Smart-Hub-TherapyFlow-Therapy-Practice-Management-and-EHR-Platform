package com.smart.therapy.flow.unit.client;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.util.ClientServiceEligibilityMessages;
import com.smart.therapy.flow.system.service.SystemOptionCategories;
import com.smart.therapy.flow.system.service.SystemOptionResolverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceEligibilityMessagesTest {

    @Mock
    private SystemOptionResolverService systemOptionResolverService;

    private ClientServiceEligibilityMessages messages;

    @BeforeEach
    void setUp() {
        messages = new ClientServiceEligibilityMessages(systemOptionResolverService);
    }

    @Test
    void pendingStatusExplainsClinicalTabFix() {
        Client client = Client.builder().status("pending").build();
        when(systemOptionResolverService.resolveOptionKey(
                eq(SystemOptionCategories.CLIENT_STATUS), eq("pending")))
                .thenReturn("pending");
        when(systemOptionResolverService.resolveOptionLabel(
                eq(SystemOptionCategories.CLIENT_STATUS), eq("pending")))
                .thenReturn("Pending");

        assertThat(messages.schedulingBlocked(client))
                .contains("status is Pending")
                .contains("Edit → Clinical tab")
                .contains("Status to Active")
                .doesNotContain("inactive client file");
    }

    @Test
    void inactiveStatusMentionsClosedFile() {
        Client client = Client.builder().status("inactive").build();
        when(systemOptionResolverService.resolveOptionKey(
                eq(SystemOptionCategories.CLIENT_STATUS), eq("inactive")))
                .thenReturn("inactive");
        when(systemOptionResolverService.resolveOptionLabel(
                eq(SystemOptionCategories.CLIENT_STATUS), eq("inactive")))
                .thenReturn("Inactive");

        assertThat(messages.schedulingBlocked(client))
                .contains("closed (Inactive)")
                .contains("Open File")
                .contains("Edit → Clinical tab");
    }
}

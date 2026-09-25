package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientContact;
import com.smart.therapy.flow.client.enums.ContactType;
import com.smart.therapy.flow.client.repository.ClientContactRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.service.ClientContactService;
import com.smart.therapy.flow.common.service.BlindIndexService;
import com.smart.therapy.flow.common.service.EncryptionService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientContactService Unit Tests")
class ClientContactServiceTest {

    @Mock
    private ClientContactRepository contactRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private BlindIndexService blindIndexService;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private ClientContactService contactService;

    @Test
    @DisplayName("Should update non-primary phone and promote it to primary")
    void shouldUpdateNonPrimaryPhoneAndPromoteItToPrimary() {
        Client client = Client.builder().fullName("Jane Doe").build();
        client.setId(10L);
        ClientContact existingPhone = ClientContact.builder()
                .client(client)
                .contactType(ContactType.PHONE)
                .contactValue("03001234567")
                .isPrimary(false)
                .build();
        existingPhone.setId(5L);

        when(contactRepository.findPrimaryPhoneByClientId(eq(10L), any())).thenReturn(Optional.empty());
        when(contactRepository.findByClientIdAndContactType(10L, ContactType.PHONE)).thenReturn(List.of(existingPhone));
        when(contactRepository.findById(5L)).thenReturn(Optional.of(existingPhone));
        when(contactRepository.findByClientIdAndContactType(10L, ContactType.PHONE)).thenReturn(List.of(existingPhone));
        when(contactRepository.save(any(ClientContact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientContact saved = contactService.upsertPrimaryPhone(10L, "03427090837");

        assertThat(saved.getContactValue()).isEqualTo("03427090837");
        assertThat(saved.getIsPrimary()).isTrue();
        verify(contactRepository, never()).existsByClientIdAndContactTypeAndContactValue(10L, ContactType.PHONE, "03427090837");
    }

    @Test
    @DisplayName("Should create primary phone when client has no phone contacts")
    void shouldCreatePrimaryPhoneWhenClientHasNoPhoneContacts() {
        when(blindIndexService.getSearchMode()).thenReturn(BlindIndexService.SearchMode.LEGACY);
        Client client = Client.builder().fullName("Jane Doe").build();
        client.setId(10L);

        when(contactRepository.findPrimaryPhoneByClientId(eq(10L), any())).thenReturn(Optional.empty());
        when(contactRepository.findByClientIdAndContactType(10L, ContactType.PHONE)).thenReturn(List.of());
        when(contactRepository.findByClientIdAndContactType(10L, ContactType.WORK_PHONE)).thenReturn(List.of());
        when(contactRepository.findByClientId(10L)).thenReturn(List.of());
        when(clientRepository.findById(10L)).thenReturn(Optional.of(client));
        when(contactRepository.existsByClientIdAndContactTypeAndContactValue(
                10L, ContactType.PHONE, "03427090837")).thenReturn(false);
        when(contactRepository.save(any(ClientContact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        contactService.upsertPrimaryPhone(10L, "03427090837");

        ArgumentCaptor<ClientContact> captor = ArgumentCaptor.forClass(ClientContact.class);
        verify(contactRepository).save(captor.capture());
        assertThat(captor.getValue().getContactValue()).isEqualTo("03427090837");
        assertThat(captor.getValue().getIsPrimary()).isTrue();
        assertThat(captor.getValue().getContactType()).isEqualTo(ContactType.PHONE);
    }
}

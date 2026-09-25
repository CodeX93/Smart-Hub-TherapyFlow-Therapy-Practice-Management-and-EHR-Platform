package com.smart.therapy.flow.unit.consultation;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientContact;
import com.smart.therapy.flow.client.enums.ContactType;
import com.smart.therapy.flow.client.repository.ClientContactRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.common.service.BlindIndexService;
import com.smart.therapy.flow.consultation.service.PublicConsultationExistingClientGuard;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicConsultationExistingClientGuardTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientContactRepository contactRepository;

    @Mock
    private BlindIndexService blindIndexService;

    @Test
    void rejectsAnExistingActiveClientEmailWithAStableConflictCode() {
        byte[] digest = new byte[] { 1, 2, 3 };
        Client client = Client.builder().build();
        client.setIsDeleted(false);
        ClientContact contact = ClientContact.builder()
                .client(client)
                .contactType(ContactType.EMAIL)
                .contactValue("person@example.com")
                .build();

        when(blindIndexService.getSearchMode()).thenReturn(BlindIndexService.SearchMode.BLIND_ONLY);
        when(blindIndexService.compute(BlindIndexService.Kind.CONTACT, "person@example.com"))
                .thenReturn(digest);
        when(contactRepository.findByContactBlindIdx(digest)).thenReturn(List.of(contact));

        PublicConsultationExistingClientGuard guard = new PublicConsultationExistingClientGuard(
                clientRepository, contactRepository, blindIndexService);

        assertThatThrownBy(() -> guard.requireNewClientEmail(" Person@Example.com "))
                .isInstanceOf(StoryApiException.class)
                .satisfies(error -> {
                    StoryApiException apiError = (StoryApiException) error;
                    org.assertj.core.api.Assertions.assertThat(apiError.getStatus().value()).isEqualTo(409);
                    org.assertj.core.api.Assertions.assertThat(apiError.getCode()).isEqualTo("PUBLIC_CLIENT_EXISTS");
                });
    }
}

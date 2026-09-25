package com.smart.therapy.flow.consultation.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.service.ClientService;
import com.smart.therapy.flow.common.service.BlindIndexService;
import com.smart.therapy.flow.consultation.dto.PublicBookConsultationRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicConsultationServicePortalTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private BlindIndexService blindIndexService;

    @Mock
    private ClientService clientService;

    @InjectMocks
    private PublicConsultationService publicConsultationService;

    @Test
    void creatingAConsultationClientAutomaticallyProvisionsPortalAccess() {
        PublicBookConsultationRequest request = new PublicBookConsultationRequest();
        request.setClientFullName("Public Client");
        request.setClientEmail("client@example.com");
        request.setClientTimezone("America/Toronto");

        User therapist = User.builder().fullName("Assigned Therapist").build();
        therapist.setId(3L);
        when(clientRepository.saveAndFlush(any(Client.class))).thenAnswer(invocation -> {
            Client saved = invocation.getArgument(0);
            saved.setId(7L);
            return saved;
        });

        Client saved = publicConsultationService.createConsultationClient(request, therapist);

        verify(clientService).enablePortalForPublicBooking(
                saved, "client@example.com", 3L, "Assigned Therapist");
    }
}

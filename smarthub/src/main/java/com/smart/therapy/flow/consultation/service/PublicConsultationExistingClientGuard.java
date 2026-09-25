package com.smart.therapy.flow.consultation.service;

import com.smart.therapy.flow.client.enums.ContactType;
import com.smart.therapy.flow.client.repository.ClientContactRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.common.service.BlindIndexService;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PublicConsultationExistingClientGuard {
    private final ClientRepository clientRepository;
    private final ClientContactRepository contactRepository;
    private final BlindIndexService blindIndexService;

    public void requireNewClientEmail(String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        boolean existingPortalClient = clientRepository.existsByPortalEmail(normalized);
        boolean existingContact = false;

        if (blindIndexService.getSearchMode().writesBlindIndexes()) {
            byte[] digest = blindIndexService.compute(BlindIndexService.Kind.CONTACT, normalized);
            existingContact = contactRepository.findByContactBlindIdx(digest).stream()
                    .anyMatch(contact -> contact.getContactType() == ContactType.EMAIL
                            && contact.getClient() != null
                            && !Boolean.TRUE.equals(contact.getClient().getIsDeleted()));
        } else {
            existingContact = !contactRepository.findActiveClientsByEmail(normalized).isEmpty();
        }

        if (existingPortalClient || existingContact) {
            throw new StoryApiException(
                    HttpStatus.CONFLICT,
                    "PUBLIC_CLIENT_EXISTS",
                    "An account with this email already exists. Sign in to the client portal to book an appointment.");
        }
    }
}

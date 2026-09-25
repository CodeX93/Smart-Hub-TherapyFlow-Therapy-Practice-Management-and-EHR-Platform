package com.smart.therapy.flow.client.service;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientContact;
import com.smart.therapy.flow.client.enums.ContactType;
import com.smart.therapy.flow.client.repository.ClientContactRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smart.therapy.flow.common.util.PhoneNormalizationUtil;
import com.smart.therapy.flow.common.converter.EncryptedSearchableStringConverter;
import com.smart.therapy.flow.common.service.BlindIndexService;
import com.smart.therapy.flow.common.service.EncryptionService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientContactService {

    private final ClientContactRepository contactRepository;
    private final ClientRepository clientRepository;
    private final EncryptionService encryptionService;
    private final BlindIndexService blindIndexService;

    @Transactional(readOnly = true)
    public List<ClientContact> getContacts(Long clientId) {
        return contactRepository.findByClientId(clientId);
    }

    @Transactional(readOnly = true)
    public List<ClientContact> getContactsByType(Long clientId, ContactType contactType) {
        return contactRepository.findByClientIdAndContactType(clientId, contactType);
    }

    @Transactional(readOnly = true)
    public Optional<ClientContact> getPrimaryContact(Long clientId, ContactType contactType) {
        return contactRepository.findPrimaryByClientIdAndContactType(clientId, contactType);
    }

    @Transactional(readOnly = true)
    public Optional<ClientContact> getPrimaryEmail(Long clientId) {
        return contactRepository.findPrimaryEmailByClientId(clientId, ContactType.EMAIL);
    }

    @Transactional(readOnly = true)
    public Optional<ClientContact> getPrimaryPhone(Long clientId) {
        return contactRepository.findPrimaryPhoneByClientId(clientId, List.of(ContactType.PHONE, ContactType.WORK_PHONE));
    }

    @Transactional(readOnly = true)
    public List<Client> findClientsByNormalizedPhone(String phoneE164) {
        if (phoneE164 == null || phoneE164.isBlank()) {
            return List.of();
        }

        Set<Client> matches = new LinkedHashSet<>();
        if (blindIndexService.getSearchMode().writesBlindIndexes()) {
            String normalized = blindIndexService.normalizePhone(phoneE164);
            byte[] digest = blindIndexService.compute(BlindIndexService.Kind.CONTACT, normalized);
            contactRepository.findByContactBlindIdx(digest).stream()
                    .map(ClientContact::getClient)
                    .filter(Objects::nonNull)
                    .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                    .forEach(matches::add);
            if (!matches.isEmpty()
                    || blindIndexService.getSearchMode() == BlindIndexService.SearchMode.BLIND_ONLY) {
                return new ArrayList<>(matches);
            }
        }

        // Legacy / dual fallback for unreencrypted TFENC:v2d rows
        String encryptedPhone = encryptionService.encryptDeterministic(
                phoneE164, EncryptedSearchableStringConverter.PURPOSE_CONTACT_VALUE);
        contactRepository.findByContactValue(encryptedPhone).stream()
                .map(ClientContact::getClient)
                .filter(Objects::nonNull)
                .forEach(matches::add);
        if (matches.isEmpty()) {
            contactRepository.findByContactValue(phoneE164).stream()
                    .map(ClientContact::getClient)
                    .filter(Objects::nonNull)
                    .forEach(matches::add);
        }

        if (matches.isEmpty()
                && blindIndexService.getSearchMode() != BlindIndexService.SearchMode.BLIND_ONLY) {
            List<ContactType> phoneTypes = List.of(ContactType.PHONE, ContactType.WORK_PHONE);
            contactRepository.findActivePhoneContacts(phoneTypes).stream()
                    .filter(contact -> phoneE164.equals(
                            PhoneNormalizationUtil.normalizePhoneE164(contact.getContactValue())))
                    .map(ClientContact::getClient)
                    .filter(Objects::nonNull)
                    .forEach(matches::add);
        }

        return new ArrayList<>(matches);
    }

    @Transactional(readOnly = true)
    public List<ClientContact> getEmergencyContacts(Long clientId) {
        return contactRepository.findEmergencyContactsByClientId(clientId, ContactType.EMERGENCY_CONTACT);
    }

    @Transactional
    public ClientContact createContact(Long clientId, ClientContact contact) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(contact, "Contact is required");
        
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        // Validate contact type
        if (contact.getContactType() == null) {
            throw new BadRequestException("Contact type is required");
        }

        // Validate contact value
        if (contact.getContactValue() == null || contact.getContactValue().trim().isEmpty()) {
            throw new BadRequestException("Contact value is required");
        }

        // Validate uniqueness
        if (contactExists(clientId, contact.getContactType(), contact.getContactValue())) {
            throw new BadRequestException("Contact already exists for this client");
        }

        // Validate contact value format
        if (!contact.isValidForType()) {
            throw new BadRequestException("Invalid contact value for type: " + contact.getContactType());
        }

        contact.setClient(client);

        // If this is marked as primary, unset other primary contacts of same type
        if (Boolean.TRUE.equals(contact.getIsPrimary())) {
            unsetPrimaryContacts(clientId, contact.getContactType(), contact.getId());
        }

        blindIndexService.applyContactBlindIndex(contact);
        return contactRepository.save(contact);
    }

    @Transactional
    public ClientContact upsertPrimaryEmail(Long clientId, String email) {
        Objects.requireNonNull(clientId, "Client ID is required");
        if (email == null || email.trim().isEmpty()) {
            throw new BadRequestException("Email is required");
        }
        String emailValue = email.trim();

        Optional<ClientContact> existingByValue = findContactByValue(clientId, ContactType.EMAIL, emailValue);
        if (existingByValue.isPresent()) {
            return promoteToPrimary(existingByValue.get());
        }

        Optional<ClientContact> existingPrimary = getPrimaryEmail(clientId);
        if (existingPrimary.isPresent()) {
            ClientContact contact = existingPrimary.get();
            contact.setContactValue(emailValue);
            contact.setIsPrimary(true);
            return updateContact(contact.getId(), contact);
        }

        Optional<ClientContact> existingEmail = findAnyEmailContact(clientId);
        if (existingEmail.isPresent()) {
            ClientContact contact = existingEmail.get();
            contact.setContactValue(emailValue);
            contact.setIsPrimary(true);
            return updateContact(contact.getId(), contact);
        }

        return createContact(clientId, ClientContact.builder()
                .contactType(ContactType.EMAIL)
                .contactValue(emailValue)
                .isPrimary(true)
                .isVerified(false)
                .displayOrder(1)
                .build());
    }

    @Transactional
    public ClientContact upsertPrimaryPhone(Long clientId, String phone) {
        Objects.requireNonNull(clientId, "Client ID is required");
        if (phone == null || phone.trim().isEmpty()) {
            throw new BadRequestException("Phone number is required");
        }
        String phoneValue = phone.trim();

        Optional<ClientContact> existingByValue = findMatchingPhoneContact(clientId, phoneValue);
        if (existingByValue.isPresent()) {
            return promoteToPrimary(existingByValue.get());
        }

        Optional<ClientContact> existingPrimary = getPrimaryPhone(clientId);
        if (existingPrimary.isPresent()) {
            ClientContact contact = existingPrimary.get();
            contact.setContactValue(phoneValue);
            contact.setIsPrimary(true);
            return updateContact(contact.getId(), contact);
        }

        Optional<ClientContact> existingPhone = findAnyPhoneContact(clientId);
        if (existingPhone.isPresent()) {
            ClientContact contact = existingPhone.get();
            contact.setContactValue(phoneValue);
            contact.setIsPrimary(true);
            return updateContact(contact.getId(), contact);
        }

        return createContact(clientId, ClientContact.builder()
                .contactType(ContactType.PHONE)
                .contactValue(phoneValue)
                .isPrimary(true)
                .isVerified(false)
                .displayOrder(2)
                .build());
    }

    @Transactional
    public ClientContact updateContact(Long contactId, ClientContact updatedContact) {
        ClientContact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        // Validate contact value format
        if (!updatedContact.isValidForType()) {
            throw new BadRequestException("Invalid contact value for type: " + updatedContact.getContactType());
        }

        String nextValue = updatedContact.getContactValue() != null
                ? updatedContact.getContactValue().trim()
                : null;
        if (nextValue == null || nextValue.isEmpty()) {
            throw new BadRequestException("Contact value is required");
        }

        if (!nextValue.equals(contact.getContactValue())
                && contactExists(contact.getClient().getId(), updatedContact.getContactType(), nextValue)) {
            throw new BadRequestException("Contact already exists for this client");
        }

        // Update fields
        contact.setContactType(updatedContact.getContactType());
        contact.setContactValue(nextValue);
        contact.setIsPrimary(updatedContact.getIsPrimary());
        contact.setIsVerified(updatedContact.getIsVerified());
        contact.setLabel(updatedContact.getLabel());
        contact.setContactPersonName(updatedContact.getContactPersonName());
        contact.setRelationship(updatedContact.getRelationship());
        contact.setCanMakeMedicalDecisions(updatedContact.getCanMakeMedicalDecisions());
        contact.setCanBeNotified(updatedContact.getCanBeNotified());
        contact.setNotes(updatedContact.getNotes());
        contact.setDisplayOrder(updatedContact.getDisplayOrder());

        // If this is marked as primary, unset other primary contacts of same type
        if (Boolean.TRUE.equals(contact.getIsPrimary())) {
            unsetPrimaryContacts(contact.getClient().getId(), contact.getContactType(), contactId);
        }

        blindIndexService.applyContactBlindIndex(contact);
        return contactRepository.save(contact);
    }

    @Transactional
    public void deleteContact(Long contactId) {
        ClientContact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
        contactRepository.delete(contact);
    }

    @Transactional
    public ClientContact setAsPrimary(Long contactId) {
        ClientContact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));

        unsetPrimaryContacts(contact.getClient().getId(), contact.getContactType(), contactId);
        contact.setIsPrimary(true);
        return contactRepository.save(contact);
    }

    private boolean contactExists(Long clientId, ContactType contactType, String contactValue) {
        if (blindIndexService.getSearchMode().writesBlindIndexes()) {
            String normalized = blindIndexService.normalizeContactValue(contactType, contactValue);
            byte[] digest = blindIndexService.compute(BlindIndexService.Kind.CONTACT, normalized);
            return contactRepository.existsByClientIdAndContactTypeAndContactBlindIdx(
                    clientId, contactType, digest);
        }
        return contactRepository.existsByClientIdAndContactTypeAndContactValue(
                clientId, contactType, contactValue);
    }

    private Optional<ClientContact> findAnyPhoneContact(Long clientId) {
        List<ClientContact> phones = contactRepository.findByClientIdAndContactType(clientId, ContactType.PHONE);
        if (!phones.isEmpty()) {
            return Optional.of(phones.get(0));
        }
        phones = contactRepository.findByClientIdAndContactType(clientId, ContactType.WORK_PHONE);
        if (!phones.isEmpty()) {
            return Optional.of(phones.get(0));
        }
        return contactRepository.findByClientId(clientId).stream()
                .filter(ClientContact::isPhone)
                .findFirst();
    }

    private Optional<ClientContact> findAnyEmailContact(Long clientId) {
        List<ClientContact> emails = contactRepository.findByClientIdAndContactType(clientId, ContactType.EMAIL);
        if (!emails.isEmpty()) {
            return Optional.of(emails.get(0));
        }
        return contactRepository.findByClientId(clientId).stream()
                .filter(ClientContact::isEmail)
                .findFirst();
    }

    private Optional<ClientContact> findContactByValue(Long clientId, ContactType contactType, String value) {
        return contactRepository.findByClientIdAndContactType(clientId, contactType).stream()
                .filter(contact -> value.equals(contact.getContactValue()))
                .findFirst();
    }

    private Optional<ClientContact> findMatchingPhoneContact(Long clientId, String phoneValue) {
        return contactRepository.findByClientId(clientId).stream()
                .filter(ClientContact::isPhone)
                .filter(contact -> phoneValue.equals(contact.getContactValue()))
                .findFirst();
    }

    private ClientContact promoteToPrimary(ClientContact contact) {
        contact.setIsPrimary(true);
        return updateContact(contact.getId(), contact);
    }

    private void unsetPrimaryContacts(Long clientId, ContactType contactType, Long exceptContactId) {
        List<ClientContact> primaryContacts = contactRepository.findByClientIdAndContactType(clientId, contactType)
                .stream()
                .filter(ClientContact::isPrimary)
                .filter(pc -> exceptContactId == null || !exceptContactId.equals(pc.getId()))
                .toList();

        for (ClientContact pc : primaryContacts) {
            pc.setIsPrimary(false);
            contactRepository.save(pc);
        }
    }

    @Transactional(readOnly = true)
    public Optional<ClientContact> getContact(Long contactId) {
        return contactRepository.findById(contactId);
    }
}


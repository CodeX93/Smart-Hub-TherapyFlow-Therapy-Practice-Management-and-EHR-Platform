package com.smart.therapy.flow.common.validation;

import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.auth.service.AuthIdentityService;
import com.smart.therapy.flow.client.entity.ClientContact;
import com.smart.therapy.flow.client.enums.ContactType;
import com.smart.therapy.flow.client.repository.ClientContactRepository;
import com.smart.therapy.flow.client.service.ClientSearchHelper;
import com.smart.therapy.flow.common.service.BlindIndexService;
import com.smart.therapy.flow.common.tenant.TenantContext;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {

    private final UserRepository userRepository;
    private final ClientContactRepository contactRepository;
    private final AuthIdentityRepository authIdentityRepository;
    private final ClientSearchHelper clientSearchHelper;
    private final BlindIndexService blindIndexService;

    @Override
    public void initialize(UniqueEmail constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.isEmpty()) {
            return true;
        }

        String normalizedEmail = AuthIdentityService.normaliseLoginIdentifier(email);

        boolean userExists = userRepository.existsByEmail(normalizedEmail);

        boolean clientEmailExists = false;
        List<ClientContact> contacts = List.of();
        if (blindIndexService.getSearchMode().writesBlindIndexes()) {
            byte[] digest = blindIndexService.compute(BlindIndexService.Kind.CONTACT, normalizedEmail);
            contacts = contactRepository.findByContactBlindIdx(digest);
        }
        if (contacts.isEmpty()
                && blindIndexService.getSearchMode() != BlindIndexService.SearchMode.BLIND_ONLY) {
            String encryptedEmail = clientSearchHelper.encryptContactLookup(normalizedEmail);
            contacts = contactRepository.findByContactValue(encryptedEmail);
            if (contacts.isEmpty()) {
                contacts = contactRepository.findByContactValue(normalizedEmail);
            }
        }
        for (ClientContact contact : contacts) {
            if (contact.getContactType() == ContactType.EMAIL
                    && contact.getClient() != null
                    && !Boolean.TRUE.equals(contact.getClient().getIsDeleted())) {
                clientEmailExists = true;
                break;
            }
        }

        Long orgId = TenantContext.getOrganisationId();
        boolean authEmailExists;
        if (orgId != null) {
            authEmailExists = authIdentityRepository.existsByOrganisationIdAndNormalisedEmail(
                    orgId, normalizedEmail, null);
        } else {
            authEmailExists = authIdentityRepository.existsPlatformByNormalisedEmail(normalizedEmail, null);
        }

        if (userExists || clientEmailExists || authEmailExists) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Email already exists")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}

package com.smart.therapy.flow.client.service;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientInsurance;
import com.smart.therapy.flow.client.repository.ClientInsuranceRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientInsuranceService {

    private final ClientInsuranceRepository insuranceRepository;
    private final ClientRepository clientRepository;
    private final Validator validator;

    @Transactional(readOnly = true)
    public Optional<ClientInsurance> getInsurance(Long clientId) {
        return insuranceRepository.findByClientId(clientId);
    }

    @Transactional(readOnly = true)
    public Optional<ClientInsurance> getActiveInsurance(Long clientId) {
        return insuranceRepository.findActiveByClientId(clientId);
    }

    @Transactional
    public ClientInsurance createInsurance(Long clientId, ClientInsurance insurance) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(insurance, "Insurance is required");
        
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        if (insuranceRepository.findByClientId(clientId).isPresent()) {
            throw new BadRequestException("Insurance already exists for this client");
        }

        // Check for duplicate policy
        if (insurance.getPolicyNumber() != null && insurance.getInsuranceProvider() != null) {
            if (insuranceRepository.existsByPolicyNumberAndProvider(
                    insurance.getPolicyNumber(), insurance.getInsuranceProvider())) {
                throw new BadRequestException("Insurance policy already exists");
            }
        }

        insurance.setClient(client);
        if (insurance.getIsActive() == null) {
            insurance.setIsActive(true);
        }

        validateInsurance(insurance);
        return insuranceRepository.save(insurance);
    }

    @Transactional
    public ClientInsurance updateInsurance(Long clientId, ClientInsurance updatedInsurance) {
        ClientInsurance insurance = insuranceRepository.findByClientId(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance not found for client"));

        patchInsuranceFields(insurance, updatedInsurance);
        validateInsurance(insurance);
        return insuranceRepository.save(insurance);
    }

    private void patchInsuranceFields(ClientInsurance target, ClientInsurance source) {
        if (source.getInsuranceProvider() != null) {
            target.setInsuranceProvider(source.getInsuranceProvider());
        }
        if (source.getPolicyNumber() != null) {
            target.setPolicyNumber(source.getPolicyNumber());
        }
        if (source.getGroupNumber() != null) {
            target.setGroupNumber(source.getGroupNumber());
        }
        if (source.getSubscriberName() != null) {
            target.setSubscriberName(source.getSubscriberName());
        }
        if (source.getSubscriberRelationship() != null) {
            target.setSubscriberRelationship(source.getSubscriberRelationship());
        }
        if (source.getInsurancePhone() != null) {
            target.setInsurancePhone(source.getInsurancePhone());
        }
        if (source.getInsuranceEmail() != null) {
            target.setInsuranceEmail(source.getInsuranceEmail());
        }
        if (source.getCopayAmount() != null) {
            target.setCopayAmount(source.getCopayAmount());
        }
        if (source.getDeductible() != null) {
            target.setDeductible(source.getDeductible());
        }
        if (source.getDeductibleMet() != null) {
            target.setDeductibleMet(source.getDeductibleMet());
        }
        if (source.getOutOfPocketMax() != null) {
            target.setOutOfPocketMax(source.getOutOfPocketMax());
        }
        if (source.getCoveragePercentage() != null) {
            target.setCoveragePercentage(source.getCoveragePercentage());
        }
        if (source.getEffectiveDate() != null) {
            target.setEffectiveDate(source.getEffectiveDate());
        }
        if (source.getExpiryDate() != null) {
            target.setExpiryDate(source.getExpiryDate());
        }
        if (source.getIsActive() != null) {
            target.setIsActive(source.getIsActive());
        }
        if (source.getIsVerified() != null) {
            target.setIsVerified(source.getIsVerified());
        }
        if (source.getAuthorizationRequired() != null) {
            target.setAuthorizationRequired(source.getAuthorizationRequired());
        }
        if (source.getAuthorizationNumber() != null) {
            target.setAuthorizationNumber(source.getAuthorizationNumber());
        }
        if (source.getAuthorizationExpiresAt() != null) {
            target.setAuthorizationExpiresAt(source.getAuthorizationExpiresAt());
        }
        if (source.getMentalHealthCoverage() != null) {
            target.setMentalHealthCoverage(source.getMentalHealthCoverage());
        }
        if (source.getTelehealthCoverage() != null) {
            target.setTelehealthCoverage(source.getTelehealthCoverage());
        }
        if (source.getSessionsPerYear() != null) {
            target.setSessionsPerYear(source.getSessionsPerYear());
        }
        if (source.getSessionsUsed() != null) {
            target.setSessionsUsed(source.getSessionsUsed());
        }
        if (source.getNotes() != null) {
            target.setNotes(source.getNotes());
        }
    }

    private void validateInsurance(ClientInsurance insurance) {
        Set<ConstraintViolation<ClientInsurance>> violations = validator.validate(insurance);
        if (!violations.isEmpty()) {
            ConstraintViolation<ClientInsurance> first = violations.iterator().next();
            throw new BadRequestException(first.getMessage());
        }
    }

    @Transactional
    public ClientInsurance incrementSessionsUsed(Long clientId) {
        ClientInsurance insurance = insuranceRepository.findByClientId(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance not found for client"));

        int currentSessions = insurance.getSessionsUsed() != null ? insurance.getSessionsUsed() : 0;
        insurance.setSessionsUsed(currentSessions + 1);

        return insuranceRepository.save(insurance);
    }

    @Transactional
    public ClientInsurance updateDeductibleMet(Long clientId, BigDecimal amount) {
        ClientInsurance insurance = insuranceRepository.findByClientId(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance not found for client"));

        BigDecimal currentMet = insurance.getDeductibleMet() != null ? insurance.getDeductibleMet() : BigDecimal.ZERO;
        insurance.setDeductibleMet(currentMet.add(amount));

        return insuranceRepository.save(insurance);
    }

    @Transactional
    public void deleteInsurance(Long clientId) {
        ClientInsurance insurance = insuranceRepository.findByClientId(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Insurance not found"));
        insuranceRepository.delete(insurance);
    }
}


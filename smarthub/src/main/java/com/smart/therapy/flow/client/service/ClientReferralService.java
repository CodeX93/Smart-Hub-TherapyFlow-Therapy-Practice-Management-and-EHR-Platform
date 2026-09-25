package com.smart.therapy.flow.client.service;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientReferral;
import com.smart.therapy.flow.client.enums.ReferralSource;
import com.smart.therapy.flow.client.repository.ClientReferralRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClientReferralService {

    private final ClientReferralRepository referralRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public Optional<ClientReferral> getReferral(Long clientId) {
        return referralRepository.findByClientId(clientId);
    }

    @Transactional(readOnly = true)
    public List<ClientReferral> getReferralsByReferrer(String referrerName) {
        return referralRepository.findByReferrerName(referrerName);
    }

    @Transactional(readOnly = true)
    public List<ClientReferral> getReferralsBySource(String source) {
        try {
            ReferralSource referralSource = ReferralSource.valueOf(source.toUpperCase());
            return referralRepository.findByReferralSource(referralSource);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid referral source: {}", source);
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public List<ClientReferral> getCourtOrderedReferrals() {
        return referralRepository.findCourtOrderedReferrals();
    }

    @Transactional(readOnly = true)
    public List<ClientReferral> getReferralsRequiringReporting() {
        return referralRepository.findReferralsRequiringReporting();
    }

    @Transactional
    public ClientReferral createReferral(Long clientId, ClientReferral referral) {
        Objects.requireNonNull(clientId, "Client ID is required");
        Objects.requireNonNull(referral, "Referral is required");
        
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        if (referralRepository.findByClientId(clientId).isPresent()) {
            throw new BadRequestException("Referral already exists for this client");
        }

        referral.setClient(client);
        return referralRepository.save(referral);
    }

    @Transactional
    public ClientReferral updateReferral(Long clientId, ClientReferral updatedReferral) {
        ClientReferral referral = referralRepository.findByClientId(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Referral not found for client"));

        // Update all fields
        referral.setReferralDate(updatedReferral.getReferralDate());
        referral.setReferralSource(updatedReferral.getReferralSource());
        referral.setReferralType(updatedReferral.getReferralType());
        referral.setReferrerName(updatedReferral.getReferrerName());
        referral.setReferrerTitle(updatedReferral.getReferrerTitle());
        referral.setReferrerOrganization(updatedReferral.getReferrerOrganization());
        referral.setReferrerPhone(updatedReferral.getReferrerPhone());
        referral.setReferrerEmail(updatedReferral.getReferrerEmail());
        referral.setReferenceNumber(updatedReferral.getReferenceNumber());
        referral.setClientSource(updatedReferral.getClientSource());
        referral.setIsCourtOrdered(updatedReferral.getIsCourtOrdered());
        referral.setCourtOrderNumber(updatedReferral.getCourtOrderNumber());
        referral.setCourtJurisdiction(updatedReferral.getCourtJurisdiction());
        referral.setRequiresReporting(updatedReferral.getRequiresReporting());
        referral.setReportingFrequency(updatedReferral.getReportingFrequency());
        referral.setReportingRecipient(updatedReferral.getReportingRecipient());
        referral.setConsentToContactReferrer(updatedReferral.getConsentToContactReferrer());
        referral.setMarketingCampaign(updatedReferral.getMarketingCampaign());
        referral.setPromoCode(updatedReferral.getPromoCode());
        referral.setReferralNotes(updatedReferral.getReferralNotes());
        referral.setIntakeSummary(updatedReferral.getIntakeSummary());

        return referralRepository.save(referral);
    }

    @Transactional
    public void deleteReferral(Long clientId) {
        ClientReferral referral = referralRepository.findByClientId(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Referral not found"));
        referralRepository.delete(referral);
    }
}


package com.smart.therapy.flow.payment.service;

import com.smart.therapy.flow.billing.entity.SessionBilling;
import com.smart.therapy.flow.billing.enums.BillingStatus;
import com.smart.therapy.flow.billing.repository.SessionBillingRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientPortalSession;
import com.smart.therapy.flow.client.repository.ClientPortalSessionRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.exception.UnauthorizedException;
import com.smart.therapy.flow.payment.dto.StripeCheckoutResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StripePaymentApplicationService {

    private final StripeService stripeService;
    private final ClientPortalSessionRepository sessionRepository;
    private final ClientRepository clientRepository;
    private final SessionBillingRepository billingRepository;

    public StripeCheckoutResponse initiatePayment(Long invoiceId, String sessionToken) {
        if (sessionToken == null) {
            throw new UnauthorizedException("Not authenticated");
        }

        ClientPortalSession session = sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired session"));

        Client client = clientRepository.findById(session.getClient().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        SessionBilling billing = billingRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (billing.getSession() == null || !billing.getSession().getClient().getId().equals(client.getId())) {
            throw new UnauthorizedException("Invoice not found or access denied");
        }
        if (billing.getBillingStatus() == BillingStatus.PAID) {
            throw new BadRequestException("Invoice already paid");
        }

        String sessionDate = billing.getSession().getSessionDate() != null
                ? billing.getSession().getSessionDate().toString()
                : "N/A";
        String sessionType = billing.getSession().getSessionType() != null
                ? billing.getSession().getSessionType()
                : "Individual";

        return stripeService.createCheckoutSession(
                invoiceId,
                client.getId(),
                billing.getTotalAmount().toString(),
                billing.getServiceCode(),
                billing.getServiceCode(),
                sessionType,
                sessionDate,
                client.getPrimaryEmail()
        );
    }
}

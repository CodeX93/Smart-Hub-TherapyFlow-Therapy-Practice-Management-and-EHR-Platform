package com.smart.therapy.flow.report.service;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CaseloadScope;
import com.smart.therapy.flow.common.security.CaseloadScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientReportAccessService {

    private final ClientRepository clientRepository;
    private final CaseloadScopeService caseloadScopeService;

    @Transactional(readOnly = true)
    public Client requireClientAccess(Long clientId, AuthPrincipal requester) {
        Client client = clientRepository.findByIdWithTherapist(clientId)
                .orElseGet(() -> clientRepository.findById(clientId)
                        .orElseThrow(() -> new ResourceNotFoundException("Client not found")));
        validateClientAccess(client, requester);
        return client;
    }

    /**
     * PBAC caseload check (CLIENT_VIEW_ALL / TEAM / OWN) for a loaded client entity.
     * Shared by Form, Assessment, Document, and report PHI paths.
     */
    public void validateClientAccess(Client client, AuthPrincipal requester) {
        CaseloadScopeService.ResolvedCaseloadScope resolved = caseloadScopeService.resolve(requester);
        if (resolved.scope() == CaseloadScope.NONE) {
            throw new ForbiddenException("You do not have permission to access clients");
        }
        if (resolved.scope() == CaseloadScope.ALL) {
            return;
        }

        Long therapistId;
        try {
            therapistId = client.getAssignedTherapist() != null ? client.getAssignedTherapist().getId() : null;
        } catch (org.hibernate.LazyInitializationException e) {
            Client clientWithTherapist = clientRepository.findByIdWithTherapist(client.getId()).orElse(client);
            therapistId = clientWithTherapist.getAssignedTherapist() != null
                    ? clientWithTherapist.getAssignedTherapist().getId()
                    : null;
        }

        if (!resolved.includesTherapist(therapistId)) {
            if (resolved.scope() == CaseloadScope.OWN) {
                throw new ForbiddenException("You can only access clients assigned to you");
            }
            throw new ForbiddenException("You can only access clients of therapists you supervise");
        }
    }
}

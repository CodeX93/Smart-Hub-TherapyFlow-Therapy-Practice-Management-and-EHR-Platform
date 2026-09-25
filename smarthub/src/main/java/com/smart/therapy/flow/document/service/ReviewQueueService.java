package com.smart.therapy.flow.document.service;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.document.entity.Document;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewQueueService {

    private final ReviewPolicyService reviewPolicyService;

    public List<Document> getScopedPendingQueue(AuthPrincipal actor, List<Document> documents) {
        return documents.stream()
                .filter(doc -> reviewPolicyService.canViewPendingDocument(actor, doc))
                .filter(doc -> reviewPolicyService.resolveReviewEligibility(actor, doc))
                .toList();
    }
}

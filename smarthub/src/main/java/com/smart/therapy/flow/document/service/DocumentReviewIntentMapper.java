package com.smart.therapy.flow.document.service;

import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.document.dto.ReviewDocumentRequest;
import com.smart.therapy.flow.document.entity.Document;
import com.smart.therapy.flow.document.enums.ReviewStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Component
public class DocumentReviewIntentMapper {

    public ReviewStatus resolveCanonicalStatus(ReviewDocumentRequest request, Document document) {
        if (request.getReviewStatus() != null) {
            return request.getReviewStatus();
        }
        if (!StringUtils.hasText(request.getAction())) {
            throw new BadRequestException("Either reviewStatus or action is required");
        }
        String action = request.getAction().trim().toLowerCase(Locale.ROOT);
        return switch (action) {
            case "reviewed" -> ReviewStatus.APPROVED;
            case "rejected" -> ReviewStatus.REJECTED;
            case "pending_review" -> resolvePendingReviewStatus(document);
            default -> throw new BadRequestException("Unsupported review action: " + request.getAction());
        };
    }

    private ReviewStatus resolvePendingReviewStatus(Document document) {
        if (document == null || document.getReviewStatus() == null || document.getReviewStatus() == ReviewStatus.PENDING) {
            return ReviewStatus.THERAPIST_REVIEW;
        }
        if (document.getReviewStatus() == ReviewStatus.THERAPIST_REVIEW) {
            return ReviewStatus.SUPERVISOR_REVIEW;
        }
        return ReviewStatus.PENDING;
    }
}

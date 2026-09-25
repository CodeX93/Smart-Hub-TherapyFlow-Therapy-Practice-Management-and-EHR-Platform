package com.smart.therapy.flow.document.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.document.entity.Document;
import com.smart.therapy.flow.document.enums.ReviewStatus;
import com.smart.therapy.flow.user.entity.SupervisorAssignment;
import com.smart.therapy.flow.user.repository.SupervisorAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReviewPolicyService {

    private final PermissionChecker permissionChecker;
    private final CurrentUserService currentUserService;
    private final SupervisorAssignmentRepository supervisorAssignmentRepository;

    public boolean canViewPendingDocument(AuthPrincipal actor, Document document) {
        if (permissionChecker.hasRole(actor, "ADMIN") || permissionChecker.hasRole(actor, "SUPER_ADMIN")) {
            return true;
        }
        User user = currentUserService.requireCurrentUser(actor);
        Client client = document.getClient();
        Long assignedTherapistId = client != null && client.getAssignedTherapist() != null
                ? client.getAssignedTherapist().getId() : null;
        if (permissionChecker.hasPermission(actor, "CLIENT_VIEW_ALL")) {
            return true;
        }
        if (permissionChecker.hasPermission(actor, "CLIENT_VIEW_OWN")) {
            return Objects.equals(user.getId(), assignedTherapistId);
        }
        if (permissionChecker.hasPermission(actor, "CLIENT_VIEW_TEAM")) {
            List<Long> therapistIds = supervisorAssignmentRepository.findBySupervisorId(user.getId()).stream()
                    .map(SupervisorAssignment::getTherapist)
                    .filter(Objects::nonNull)
                    .map(User::getId)
                    .filter(Objects::nonNull)
                    .toList();
            return therapistIds.contains(assignedTherapistId);
        }
        return false;
    }

    public boolean resolveReviewEligibility(AuthPrincipal actor, Document document) {
        ReviewStatus status = document.getReviewStatus();
        if (status == null) {
            return false;
        }
        if (permissionChecker.hasRole(actor, "ADMIN") || permissionChecker.hasRole(actor, "SUPER_ADMIN")) {
            return status == ReviewStatus.PENDING || status == ReviewStatus.THERAPIST_REVIEW || status == ReviewStatus.SUPERVISOR_REVIEW;
        }
        if (permissionChecker.hasPermission(actor, "CLIENT_VIEW_TEAM")) {
            return status == ReviewStatus.SUPERVISOR_REVIEW || status == ReviewStatus.PENDING;
        }
        return status == ReviewStatus.THERAPIST_REVIEW || status == ReviewStatus.PENDING;
    }
}

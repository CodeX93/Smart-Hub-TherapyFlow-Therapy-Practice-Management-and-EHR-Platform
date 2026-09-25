package com.smart.therapy.flow.common.security;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.user.entity.SupervisorAssignment;
import com.smart.therapy.flow.user.repository.SupervisorAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Resolves clinical caseload scope (ALL / TEAM / OWN / NONE) for PBAC filtering.
 * Supervisors never fall through to org-wide ALL; empty assignments ⇒ NONE
 * unless they also have THERAPIST / OWN scope.
 */
@Service
@RequiredArgsConstructor
public class CaseloadScopeService {

    private final PermissionChecker permissionChecker;
    private final CurrentUserService currentUserService;
    private final SupervisorAssignmentRepository supervisorAssignmentRepository;

    public record ResolvedCaseloadScope(
            CaseloadScope scope,
            List<Long> supervisedTherapistIds,
            Long currentUserId
    ) {
        public boolean includesTherapist(Long therapistId) {
            if (therapistId == null) {
                return false;
            }
            return switch (scope) {
                case ALL -> true;
                case OWN -> Objects.equals(currentUserId, therapistId);
                case TEAM -> supervisedTherapistIds.contains(therapistId);
                case TEAM_AND_OWN ->
                        Objects.equals(currentUserId, therapistId)
                                || supervisedTherapistIds.contains(therapistId);
                case NONE -> false;
            };
        }

        public boolean isEmpty() {
            return scope == CaseloadScope.NONE;
        }
    }

    public ResolvedCaseloadScope resolve(AuthPrincipal requester) {
        if (requester == null) {
            return new ResolvedCaseloadScope(CaseloadScope.NONE, List.of(), null);
        }

        Long userId = currentUserService.requireCurrentUser(requester).getId();
        boolean isAdmin = permissionChecker.hasRole(requester, "ADMIN")
                || permissionChecker.hasRole(requester, "SUPER_ADMIN");
        boolean isSupervisor = permissionChecker.hasRole(requester, "SUPERVISOR");
        boolean isTherapist = permissionChecker.hasRole(requester, "THERAPIST");
        boolean canViewAll = permissionChecker.hasPermission(requester, "CLIENT_VIEW_ALL");
        boolean canViewTeam = permissionChecker.hasPermission(requester, "CLIENT_VIEW_TEAM");
        boolean canViewOwn = permissionChecker.hasPermission(requester, "CLIENT_VIEW_OWN");

        List<Long> supervisedIds = (isSupervisor || canViewTeam)
                ? getSupervisedTherapistIds(userId)
                : Collections.emptyList();

        // Supervisors: never org-wide ALL. TEAM from assignments; OWN only if also therapist.
        if (isSupervisor && !isAdmin) {
            boolean hasTeam = !supervisedIds.isEmpty();
            boolean hasOwn = isTherapist || canViewOwn;
            if (hasTeam && hasOwn) {
                return new ResolvedCaseloadScope(CaseloadScope.TEAM_AND_OWN, supervisedIds, userId);
            }
            if (hasTeam) {
                return new ResolvedCaseloadScope(CaseloadScope.TEAM, supervisedIds, userId);
            }
            if (hasOwn && isTherapist) {
                return new ResolvedCaseloadScope(CaseloadScope.OWN, List.of(), userId);
            }
            // Pure supervisor with zero assignments — no clinical data
            return new ResolvedCaseloadScope(CaseloadScope.NONE, List.of(), userId);
        }

        if (isAdmin || canViewAll) {
            return new ResolvedCaseloadScope(CaseloadScope.ALL, supervisedIds, userId);
        }

        if (canViewTeam && !supervisedIds.isEmpty()) {
            if (canViewOwn) {
                return new ResolvedCaseloadScope(CaseloadScope.TEAM_AND_OWN, supervisedIds, userId);
            }
            return new ResolvedCaseloadScope(CaseloadScope.TEAM, supervisedIds, userId);
        }

        if (canViewOwn) {
            return new ResolvedCaseloadScope(CaseloadScope.OWN, List.of(), userId);
        }

        if (canViewTeam) {
            return new ResolvedCaseloadScope(CaseloadScope.NONE, List.of(), userId);
        }

        return new ResolvedCaseloadScope(CaseloadScope.NONE, List.of(), userId);
    }

    public List<Long> getSupervisedTherapistIds(Long supervisorId) {
        if (supervisorId == null) {
            return List.of();
        }
        return supervisorAssignmentRepository.findBySupervisorId(supervisorId).stream()
                .map(SupervisorAssignment::getTherapist)
                .filter(Objects::nonNull)
                .map(User::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }
}

package com.smart.therapy.flow.client.service;

import com.smart.therapy.flow.auth.dto.UserStatus;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.dto.ClientFiltersBatchResponse;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.util.RoleName;
import com.smart.therapy.flow.system.dto.OptionCategoryResponse;
import com.smart.therapy.flow.system.service.SystemOptionResolverService;
import com.smart.therapy.flow.task.dto.ChecklistTemplateResponse;
import com.smart.therapy.flow.task.service.ChecklistService;
import com.smart.therapy.flow.user.dto.UserResponse;
import com.smart.therapy.flow.user.entity.SupervisorAssignment;
import com.smart.therapy.flow.user.repository.SupervisorAssignmentRepository;
import com.smart.therapy.flow.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ClientFilterQueryService {

    private final UserRepository userRepository;
    private final SupervisorAssignmentRepository supervisorAssignmentRepository;
    private final SystemOptionResolverService optionResolverService;
    private final ChecklistService checklistService;
    private final UserService userService;
    private final CurrentUserService currentUserService;

    public ClientFiltersBatchResponse getClientFiltersBatch(AuthPrincipal principal) {
        List<UserResponse> therapists = getTherapists(principal);
        List<ChecklistTemplateResponse> checklistTemplates = checklistService.getTemplates();

        List<String> neededCategoryKeys = Arrays.asList(
                "client_type", "referral_sources", "marital_status",
                "employment_status", "education_level", "gender", "preferred_language",
                "client_status", "client_stage");

        Map<String, ClientFiltersBatchResponse.SystemOptionCategory> systemOptions = new HashMap<>();
        for (String categoryKey : neededCategoryKeys) {
            OptionCategoryResponse category = optionResolverService.resolveCategoryWithOptions(categoryKey);
            systemOptions.put(categoryKey, new ClientFiltersBatchResponse.SystemOptionCategory(
                    category,
                    category.getOptions()));
        }

        return new ClientFiltersBatchResponse(therapists, checklistTemplates, systemOptions);
    }

    private List<UserResponse> getTherapists(AuthPrincipal principal) {
        List<User> therapists;
        Long requesterUserId = currentUserService.requireCurrentUser(principal).getId();

        if (hasRole(principal, "SUPERVISOR")) {
            List<SupervisorAssignment> assignments = supervisorAssignmentRepository
                    .findBySupervisorId(requesterUserId);

            if (assignments.isEmpty()) {
                return Collections.emptyList();
            }

            List<Long> supervisedTherapistIds = assignments.stream()
                    .map(SupervisorAssignment::getTherapist)
                    .filter(Objects::nonNull)
                    .map(User::getId)
                    .toList();

            therapists = userRepository.findDistinctByAuthIdentityRolesRoleNameIn(
                            java.util.List.of("THERAPIST", "therapist")).stream()
                    .filter(u -> supervisedTherapistIds.contains(u.getId()) && u.getStatus() == UserStatus.ACTIVE)
                    .toList();
        } else if (hasRole(principal, "THERAPIST")) {
            therapists = userRepository.findById(requesterUserId)
                    .filter(u -> userRepository.findDistinctByAuthIdentityRolesRoleNameIn(
                                    java.util.List.of("THERAPIST", "therapist")).stream()
                            .anyMatch(t -> t.getId().equals(u.getId()))
                            && u.getStatus() == UserStatus.ACTIVE)
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
        } else {
            therapists = userRepository.findDistinctByAuthIdentityRolesRoleNameIn(
                            java.util.List.of("THERAPIST", "therapist")).stream()
                    .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                    .toList();
        }

        return therapists.stream()
                .map(user -> {
                    Long userId = user.getId();
                    if (userId == null) {
                        return null;
                    }
                    return userService.getUser(userId, principal);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Deprecated
    private boolean hasRole(AuthPrincipal principal, String roleName) {
        return principal.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + roleName));
    }
}



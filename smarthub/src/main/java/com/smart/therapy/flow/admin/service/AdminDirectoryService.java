package com.smart.therapy.flow.admin.service;

import com.smart.therapy.flow.admin.dto.DirectoryEntityType;
import com.smart.therapy.flow.admin.dto.DirectoryEntryResponse;
import com.smart.therapy.flow.admin.dto.DirectoryRoleResponse;
import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.AuthIdentityRole;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.dto.ClientFilter;
import com.smart.therapy.flow.client.dto.DirectoryClientSummary;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.service.ClientService;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.user.dto.UserResponse;
import com.smart.therapy.flow.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDirectoryService {

    private final UserService userService;
    private final ClientService clientService;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public PaginatedResponse<DirectoryEntryResponse> listDirectory(
            DirectoryEntityType entityType,
            int page,
            int pageSize,
            String search,
            String role,
            Boolean active,
            String clientStatus,
            AuthPrincipal principal) {

        if (entityType == DirectoryEntityType.CLIENT) {
            return listClients(page, pageSize, search, clientStatus, principal);
        }
        return listUsers(page, pageSize, search, role, active, principal);
    }

    private PaginatedResponse<DirectoryEntryResponse> listUsers(
            int page,
            int pageSize,
            String search,
            String role,
            Boolean active,
            AuthPrincipal principal) {

        PaginatedResponse<UserResponse> users = userService.getUsers(page, pageSize, search, role, active, principal);
        List<Long> userIds = users.getItems().stream()
                .map(UserResponse::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, User> userById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        List<DirectoryEntryResponse> items = users.getItems().stream()
                .map(item -> toUserDirectoryEntry(item, userById.get(item.getId())))
                .toList();

        return PaginatedResponse.of(items, users.getTotalCount(), users.getPage(), users.getPageSize());
    }

    private PaginatedResponse<DirectoryEntryResponse> listClients(
            int page,
            int pageSize,
            String search,
            String clientStatus,
            AuthPrincipal principal) {

        ClientFilter filter = ClientFilter.builder()
                .search(search)
                .status(clientStatus)
                .build();

        // Directory entries need email/phone/DOB/portal-access — use directory variant, not full ClientResponse.
        PaginatedResponse<DirectoryClientSummary> clients = clientService.getClientsForDirectory(
                filter,
                PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")),
                principal);

        List<Long> clientIds = clients.getItems().stream()
                .map(DirectoryClientSummary::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, Client> clientById = clientRepository.findAllById(clientIds).stream()
                .collect(Collectors.toMap(Client::getId, Function.identity(), (a, b) -> a));

        List<DirectoryEntryResponse> items = clients.getItems().stream()
                .map(item -> toClientDirectoryEntry(item, clientById.get(item.getId())))
                .toList();

        return PaginatedResponse.of(items, clients.getTotalCount(), clients.getPage(), clients.getPageSize());
    }

    private DirectoryEntryResponse toUserDirectoryEntry(UserResponse response, User user) {
        List<DirectoryRoleResponse> roles = mapRoles(user != null ? user.getAuthIdentity() : null);
        DirectoryRoleResponse primaryRole = roles.isEmpty() ? null : roles.get(0);

        Map<String, Object> other = new LinkedHashMap<>();
        if (response.getProfile() != null) {
            other.put("timezone", response.getProfile().getTimezone());
            other.put("availabilityStatus", response.getProfile().getAvailabilityStatus());
        }

        return DirectoryEntryResponse.builder()
                .id(response.getId())
                .entityType(DirectoryEntityType.USER.name())
                .role(primaryRole != null ? primaryRole.getRole() : null)
                .roleId(primaryRole != null ? primaryRole.getRoleId() : null)
                .roles(roles)
                .name(response.getFullName())
                .email(response.getEmail())
                .username(response.getUsername())
                .phone(response.getPhone())
                .active(response.getActive())
                .status(Boolean.TRUE.equals(response.getActive()) ? "ACTIVE" : "INACTIVE")
                .profilePicture(response.getProfilePicture())
                .lastLogin(response.getLastLogin())
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .otherFields(other.isEmpty() ? Map.of() : other)
                .build();
    }

    private DirectoryEntryResponse toClientDirectoryEntry(DirectoryClientSummary response, Client client) {
        List<DirectoryRoleResponse> roles = mapRoles(client != null ? client.getAuthIdentity() : null);
        DirectoryRoleResponse primaryRole = roles.isEmpty() ? null : roles.get(0);

        Map<String, Object> other = new LinkedHashMap<>();
        other.put("stage", response.getStage());
        other.put("clientType", response.getClientType());
        other.put("hasPortalAccess", response.getHasPortalAccess());
        other.put("preferredLanguage", response.getPreferredLanguage());
        other.put("dateOfBirth", response.getDateOfBirth());

        return DirectoryEntryResponse.builder()
                .id(response.getId())
                .entityType(DirectoryEntityType.CLIENT.name())
                .role(primaryRole != null ? primaryRole.getRole() : null)
                .roleId(primaryRole != null ? primaryRole.getRoleId() : null)
                .roles(roles)
                .name(response.getFullName())
                .email(response.getEmail())
                .phone(response.getPhone())
                .active("ACTIVE".equalsIgnoreCase(response.getStatus()))
                .status(response.getStatus())
                .clientId(response.getClientId())
                .assignedTherapistId(response.getAssignedTherapistId())
                .assignedTherapistName(response.getAssignedTherapistName())
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .otherFields(other)
                .build();
    }

    private List<DirectoryRoleResponse> mapRoles(AuthIdentity authIdentity) {
        if (authIdentity == null || authIdentity.getRoles() == null || authIdentity.getRoles().isEmpty()) {
            return List.of();
        }
        Long orgId = TenantContext.getOrganisationId();

        Map<Long, DirectoryRoleResponse> uniqueByRoleId = new LinkedHashMap<>();
        List<AuthIdentityRole> sortedRoles = new ArrayList<>(authIdentity.getRoles());
        sortedRoles.sort((a, b) -> {
            String left = a != null && a.getRole() != null ? a.getRole().getName() : "";
            String right = b != null && b.getRole() != null ? b.getRole().getName() : "";
            return left.compareToIgnoreCase(right);
        });

        for (AuthIdentityRole roleLink : sortedRoles) {
            if (roleLink == null || roleLink.getRole() == null || !StringUtils.hasText(roleLink.getRole().getName())) {
                continue;
            }
            if (orgId != null) {
                Long roleOrgId = roleLink.getOrganisation() != null ? roleLink.getOrganisation().getId() : null;
                if (!Objects.equals(orgId, roleOrgId)) {
                    continue;
                }
            }
            Long roleId = roleLink.getRole().getId();
            uniqueByRoleId.putIfAbsent(
                    roleId,
                    DirectoryRoleResponse.builder()
                            .roleId(roleId)
                            .role(roleLink.getRole().getName())
                            .displayName(roleLink.getRole().getDisplayName())
                            .build()
            );
        }

        return new ArrayList<>(uniqueByRoleId.values());
    }
}

package com.smart.therapy.flow.admin.service;

import com.smart.therapy.flow.admin.dto.AdminOrganizationListItem;
import com.smart.therapy.flow.admin.dto.AdminOrganizationSearchResponse;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.repository.UserOrganisationRepository;
import com.smart.therapy.flow.subscription.repository.OrgSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOrganizationQueryService {

    private final OrganisationRepository organisationRepository;
    private final OrgSubscriptionRepository orgSubscriptionRepository;
    private final UserOrganisationRepository userOrganisationRepository;

    public AdminOrganizationSearchResponse searchOrganizations(String search,
                                                               String status,
                                                               String plan,
                                                               Instant createdAtFrom,
                                                               Instant createdAtTo,
                                                               String timezone,
                                                               String region,
                                                               String dataResidency) {
        List<Organisation> organisations = organisationRepository.findAll();
        AdminOrganizationSearchResponse response = new AdminOrganizationSearchResponse();
        if (organisations.isEmpty()) {
            response.setItems(List.of());
            response.setTotal(0L);
            return response;
        }

        List<Long> organisationIds = organisations.stream().map(Organisation::getId).filter(Objects::nonNull).toList();

        Map<Long, String> planByOrgId = orgSubscriptionRepository.findCurrentByOrganisationIds(organisationIds).stream()
                .collect(Collectors.toMap(
                        s -> s.getOrganisation().getId(),
                        s -> s.getPlan().getName(),
                        (left, right) -> left
                ));

        Map<Long, Long> usersByOrgId = userOrganisationRepository.countUsersByOrganisationIds(organisationIds).stream()
                .collect(Collectors.toMap(
                        UserOrganisationRepository.OrganisationUserCountView::getOrganisationId,
                        UserOrganisationRepository.OrganisationUserCountView::getUsersCount
                ));

        String normalizedSearch = normalize(search);
        String normalizedStatus = normalize(status);
        String normalizedPlan = normalize(plan);
        String normalizedTimezone = normalize(timezone);
        String normalizedRegion = normalize(region);
        String normalizedDataResidency = normalize(dataResidency);

        List<AdminOrganizationListItem> items = organisations.stream()
                .filter(org -> matchesSearch(org, normalizedSearch))
                .filter(org -> matchesStatus(org, normalizedStatus))
                .filter(org -> matchesPlan(org, normalizedPlan, planByOrgId))
                .filter(org -> matchesCreatedAtRange(org, createdAtFrom, createdAtTo))
                .filter(org -> matchesTimezone(org, normalizedTimezone))
                .filter(org -> matchesRegion(org, normalizedRegion))
                .filter(org -> matchesDataResidency(org, normalizedDataResidency))
                .sorted(Comparator.comparing(Organisation::getName, String.CASE_INSENSITIVE_ORDER))
                .map(org -> toItem(org, planByOrgId, usersByOrgId))
                .toList();

        response.setItems(items);
        response.setTotal((long) items.size());
        return response;
    }

    private static boolean matchesSearch(Organisation org, String search) {
        if (search == null) {
            return true;
        }
        return normalize(org.getName()).contains(search)
                || normalize(org.getSlug()).contains(search)
                || normalize(org.getSubdomain()).contains(search);
    }

    private static boolean matchesStatus(Organisation org, String status) {
        return status == null || status.equals(normalize(org.getStatus()));
    }

    private static boolean matchesPlan(Organisation org, String plan, Map<Long, String> planByOrgId) {
        if (plan == null) {
            return true;
        }
        String subscriptionPlan = normalize(planByOrgId.get(org.getId()));
        if (plan.equals(subscriptionPlan)) {
            return true;
        }
        return plan.equals(normalize(org.getOrganisationType()));
    }

    private static boolean matchesCreatedAtRange(Organisation org, Instant createdAtFrom, Instant createdAtTo) {
        Instant createdAt = org.getCreatedAt();
        if (createdAt == null) {
            return createdAtFrom == null && createdAtTo == null;
        }
        if (createdAtFrom != null && createdAt.isBefore(createdAtFrom)) {
            return false;
        }
        if (createdAtTo != null && createdAt.isAfter(createdAtTo)) {
            return false;
        }
        return true;
    }

    private static boolean matchesTimezone(Organisation org, String timezone) {
        return timezone == null || timezone.equals(normalize(org.getTimezone()));
    }

    private static boolean matchesRegion(Organisation org, String region) {
        return region == null || region.equals(normalize(org.getRegion()));
    }

    private static boolean matchesDataResidency(Organisation org, String dataResidency) {
        return dataResidency == null || dataResidency.equals(normalize(org.getDataResidency()));
    }

    private static AdminOrganizationListItem toItem(
            Organisation org,
            Map<Long, String> planByOrgId,
            Map<Long, Long> usersByOrgId
    ) {
        AdminOrganizationListItem item = new AdminOrganizationListItem();
        item.setId("org_" + org.getId());
        item.setName(org.getName());
        item.setStatus(normalize(org.getStatus()));
        String plan = planByOrgId.get(org.getId());
        item.setPlan(plan != null ? normalize(plan) : normalize(org.getOrganisationType()));
        item.setUsersCount(usersByOrgId.getOrDefault(org.getId(), 0L));
        item.setCreatedAt(org.getCreatedAt());
        item.setTimezone(org.getTimezone());
        item.setRegion(org.getRegion());
        item.setDataResidency(org.getDataResidency());
        return item;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }
}

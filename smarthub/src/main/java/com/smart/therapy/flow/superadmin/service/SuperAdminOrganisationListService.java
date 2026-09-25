package com.smart.therapy.flow.superadmin.service;

import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.subscription.repository.SubscriptionPlanRepository;
import com.smart.therapy.flow.superadmin.dto.SuperAdminOrganisationListRequest;
import com.smart.therapy.flow.superadmin.dto.SuperAdminOrganisationListResult;
import com.smart.therapy.flow.superadmin.repository.SuperAdminOrganisationListQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SuperAdminOrganisationListService {

    private static final Map<String, String> SORT_SQL = Map.of(
            "name", "o.name",
            "createdat", "o.created_at",
            "userscount", "users_count",
            "plan", "plan_name"
    );

    private static final Map<String, String> EXTERNAL_TO_INTERNAL_STATUS = Map.of(
            "active", "ACTIVE",
            "suspended", "LOCKED",
            "termination_scheduled", "ARCHIVED",
            "terminated", "DELETED"
    );

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SuperAdminOrganisationListQueryRepository queryRepository;

    public SuperAdminOrganisationListResult search(SuperAdminOrganisationListRequest req) {
        validate(req);
        return queryRepository.search(req);
    }

    private void validate(SuperAdminOrganisationListRequest req) {
        if (StringUtils.hasText(req.getSearch()) && req.getSearch().trim().length() < 2) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_FILTER", "search must be at least 2 characters");
        }
        if (req.getPageSize() != null && req.getPageSize() > 100) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_FILTER", "pageSize must be <= 100");
        }
        if (req.getPage() != null && req.getPage() < 1) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_FILTER", "page must be >= 1");
        }
        if (req.getPageSize() != null && req.getPageSize() < 1) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_FILTER", "pageSize must be >= 1");
        }
        if (req.getCreatedFrom() != null && req.getCreatedTo() != null) {
            if (req.getCreatedFrom().isAfter(req.getCreatedTo())) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_FILTER", "createdFrom must be before createdTo");
            }
            if (ChronoUnit.DAYS.between(req.getCreatedFrom(), req.getCreatedTo()) > 365) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_FILTER", "created date range cannot exceed 365 days");
            }
        }

        if (StringUtils.hasText(req.getStatus())) {
            String s = normalizeStatusKey(req.getStatus());
            if (!EXTERNAL_TO_INTERNAL_STATUS.containsKey(s) && !"pending_activation".equals(s)) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_FILTER", "invalid status");
            }
        }
        if (StringUtils.hasText(req.getSort()) && !SORT_SQL.containsKey(req.getSort().trim().toLowerCase(Locale.ROOT))) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_FILTER", "invalid sort");
        }
        if (StringUtils.hasText(req.getOrder())) {
            String order = req.getOrder().trim().toLowerCase(Locale.ROOT);
            if (!"asc".equals(order) && !"desc".equals(order)) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_FILTER", "invalid order");
            }
        }
        if (StringUtils.hasText(req.getPlan())) {
            boolean exists = subscriptionPlanRepository.findByNameIgnoreCase(req.getPlan().trim()).isPresent();
            if (!exists) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_FILTER", "plan not found");
            }
        }
    }

    private static String normalizeStatusKey(String rawStatus) {
        if (!StringUtils.hasText(rawStatus)) {
            return "";
        }
        return rawStatus.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }

}

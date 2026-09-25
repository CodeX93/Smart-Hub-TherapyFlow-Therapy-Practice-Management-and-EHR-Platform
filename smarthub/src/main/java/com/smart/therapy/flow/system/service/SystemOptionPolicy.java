package com.smart.therapy.flow.system.service;

import com.smart.therapy.flow.client.enums.Priority;
import com.smart.therapy.flow.client.enums.ClientType;
import com.smart.therapy.flow.client.enums.EducationLevel;
import com.smart.therapy.flow.client.enums.EmploymentStatus;
import com.smart.therapy.flow.client.enums.Gender;
import com.smart.therapy.flow.client.enums.MaritalStatus;
import com.smart.therapy.flow.client.enums.ReferralSource;
import com.smart.therapy.flow.client.enums.ServiceFrequency;
import com.smart.therapy.flow.client.enums.ServiceType;
import com.smart.therapy.flow.session.enums.SessionStatus;
import com.smart.therapy.flow.session.enums.SessionType;
import com.smart.therapy.flow.system.enums.OptionCategoryOwnershipType;
import com.smart.therapy.flow.task.enums.TaskStatus;

import java.util.Map;
import java.util.Set;

public final class SystemOptionPolicy {

    private static final Set<String> WORKFLOW_STRICT_CATEGORIES = Set.of();

    private static final Map<String, Class<? extends Enum<?>>> CATEGORY_ENUM_BINDINGS = Map.ofEntries(
            Map.entry("task_status", TaskStatus.class),
            Map.entry("task_priority", Priority.class),
            Map.entry("task_priorities", Priority.class),
            Map.entry("session_status", SessionStatus.class),
            Map.entry("session_mode", SessionType.class),
            Map.entry("session_modes", SessionType.class),
            Map.entry("gender", Gender.class),
            Map.entry("marital_status", MaritalStatus.class),
            Map.entry("client_type", ClientType.class),
            Map.entry("service_type", ServiceType.class),
            Map.entry("service_types", ServiceType.class),
            Map.entry("service_frequency", ServiceFrequency.class),
            Map.entry("employment_status", EmploymentStatus.class),
            Map.entry("education_level", EducationLevel.class),
            Map.entry("referral_sources", ReferralSource.class)
    );

    private SystemOptionPolicy() {
    }

    public static OptionCategoryOwnershipType ownershipOf(String categoryKey) {
        if (categoryKey == null) {
            return OptionCategoryOwnershipType.CONFIGURABLE;
        }
        return WORKFLOW_STRICT_CATEGORIES.contains(categoryKey)
                ? OptionCategoryOwnershipType.WORKFLOW_STRICT
                : OptionCategoryOwnershipType.CONFIGURABLE;
    }

    public static boolean isWorkflowStrict(String categoryKey) {
        return ownershipOf(categoryKey) == OptionCategoryOwnershipType.WORKFLOW_STRICT;
    }

    public static Class<? extends Enum<?>> enumBindingFor(String categoryKey) {
        return CATEGORY_ENUM_BINDINGS.get(categoryKey);
    }
}

package com.smart.therapy.flow.system.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SystemOptionCategories {

    public static final String CLIENT_STATUS = "client_status";
    public static final String CLIENT_STAGE = "client_stage";
    public static final String CLIENT_TYPE = "client_type";
    public static final String GENDER = "gender";
    public static final String MARITAL_STATUS = "marital_status";
    public static final String SERVICE_TYPE = "service_type";
    public static final String SERVICE_TYPES = "service_types";
    public static final String SERVICE_FREQUENCY = "service_frequency";
    public static final String EMPLOYMENT_STATUS = "employment_status";
    public static final String EDUCATION_LEVEL = "education_level";
    public static final String REFERRAL_SOURCES = "referral_sources";
    public static final String TASK_STATUS = "task_status";
    public static final String TASK_PRIORITY = "task_priority";
    public static final String TASK_PRIORITIES = "task_priorities";
    public static final String SESSION_STATUS = "session_status";
    public static final String SESSION_TYPE = "session_type";
    public static final String CLIENT_SOURCE = "client_source";
    public static final String PREFERRED_LANGUAGE = "preferred_language";
    public static final String INSURANCE_PROVIDERS = "insurance_providers";
    public static final String INSURANCE_TYPES = "insurance_types";
    public static final String TASK_TITLES = "task_titles";
    public static final String TASK_TYPES = "task_types";
    public static final String TREATMENT_MODALITIES = "treatment_modalities";
    public static final String SESSION_MODE = "session_mode";
    /** @deprecated use {@link #SESSION_MODE}; kept for legacy category keys in existing tenant data */
    public static final String SESSION_MODES = "session_modes";

    private static final Map<String, String> CATEGORY_LOOKUP_ALIASES = Map.of(
            SESSION_MODE, SESSION_MODES,
            SESSION_MODES, SESSION_MODE,
            SERVICE_TYPE, SERVICE_TYPES,
            SERVICE_TYPES, SERVICE_TYPE,
            TASK_PRIORITY, TASK_PRIORITIES,
            TASK_PRIORITIES, TASK_PRIORITY
    );

    private SystemOptionCategories() {
    }

    public static List<String> categoryLookupKeys(String categoryKey) {
        String alias = CATEGORY_LOOKUP_ALIASES.get(categoryKey);
        if (alias == null) {
            return List.of(categoryKey);
        }
        List<String> keys = new ArrayList<>(2);
        keys.add(categoryKey);
        keys.add(alias);
        return keys;
    }
}

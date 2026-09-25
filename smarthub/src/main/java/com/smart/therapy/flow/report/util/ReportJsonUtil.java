package com.smart.therapy.flow.report.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

public final class ReportJsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ReportJsonUtil() {
    }

    public static String toJsonArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(values);
        } catch (Exception ex) {
            return "[]";
        }
    }

    public static List<String> fromJsonArray(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }
}

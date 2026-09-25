package com.smart.therapy.flow.migration.clienthub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.user.entity.ShiftMode;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Parses ClientHubAI {@code user_profiles.working_hours} JSON into normalized day shifts.
 * Supports:
 * <ul>
 *   <li>Object map: {@code {"Monday":{"start":"09:00","end":"17:00"}, ...}}</li>
 *   <li>Array: {@code [{"day":"Monday","start":"09:00","end":"17:00","enabled":true,"mode":"both"}, ...]}</li>
 * </ul>
 */
public final class ClientHubWorkingHoursParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter[] TIME_FORMATS = {
            DateTimeFormatter.ofPattern("H:mm"),
            DateTimeFormatter.ofPattern("HH:mm"),
            DateTimeFormatter.ofPattern("H:mm:ss"),
            DateTimeFormatter.ofPattern("HH:mm:ss")
    };

    private ClientHubWorkingHoursParser() {
    }

    public static List<ParsedShift> parse(String workingHoursJson) {
        if (workingHoursJson == null || workingHoursJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = MAPPER.readTree(workingHoursJson);
            if (root == null || root.isNull()) {
                return List.of();
            }
            if (root.isArray()) {
                return parseArray(root);
            }
            if (root.isObject()) {
                return parseObject(root);
            }
            throw new IllegalArgumentException("working_hours must be a JSON object or array");
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid working_hours JSON", ex);
        }
    }

    public static List<ParsedShift> fromWorkingDaysFallback(List<String> workingDays) {
        if (workingDays == null || workingDays.isEmpty()) {
            return List.of();
        }
        List<ParsedShift> shifts = new ArrayList<>();
        for (String day : workingDays) {
            String normalized = normalizeDay(day);
            if (normalized != null) {
                shifts.add(new ParsedShift(normalized, LocalTime.of(9, 0), LocalTime.of(17, 0), ShiftMode.BOTH));
            }
        }
        return List.copyOf(shifts);
    }

    private static List<ParsedShift> parseArray(JsonNode root) {
        List<ParsedShift> shifts = new ArrayList<>();
        for (JsonNode node : root) {
            if (node == null || !node.isObject()) {
                continue;
            }
            if (node.has("enabled") && !node.get("enabled").asBoolean(true)) {
                continue;
            }
            String day = normalizeDay(text(node, "day"));
            LocalTime start = parseTime(text(node, "start"));
            LocalTime end = parseTime(text(node, "end"));
            if (day == null || start == null || end == null || !end.isAfter(start)) {
                continue;
            }
            shifts.add(new ParsedShift(day, start, end, parseMode(text(node, "mode"))));
        }
        return List.copyOf(shifts);
    }

    private static List<ParsedShift> parseObject(JsonNode root) {
        List<ParsedShift> shifts = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String day = normalizeDay(entry.getKey());
            JsonNode value = entry.getValue();
            if (day == null || value == null || value.isNull()) {
                continue;
            }
            if (value.isArray()) {
                for (JsonNode shiftNode : value) {
                    ParsedShift shift = parseObjectShift(day, shiftNode);
                    if (shift != null) {
                        shifts.add(shift);
                    }
                }
                continue;
            }
            ParsedShift shift = parseObjectShift(day, value);
            if (shift != null) {
                shifts.add(shift);
            }
        }
        return List.copyOf(shifts);
    }

    private static ParsedShift parseObjectShift(String day, JsonNode value) {
        if (value == null || !value.isObject()) {
            return null;
        }
        if (value.has("enabled") && !value.get("enabled").asBoolean(true)) {
            return null;
        }
        LocalTime start = parseTime(text(value, "start"));
        LocalTime end = parseTime(text(value, "end"));
        if (start == null || end == null || !end.isAfter(start)) {
            return null;
        }
        return new ParsedShift(day, start, end, parseMode(text(value, "mode")));
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText(null);
        return text == null || text.isBlank() ? null : text.trim();
    }

    private static LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        for (DateTimeFormatter formatter : TIME_FORMATS) {
            try {
                return LocalTime.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        return null;
    }

    private static ShiftMode parseMode(String value) {
        if (value == null || value.isBlank()) {
            return ShiftMode.BOTH;
        }
        try {
            return ShiftMode.fromValue(value);
        } catch (IllegalArgumentException ex) {
            return ShiftMode.BOTH;
        }
    }

    private static String normalizeDay(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return DayOfWeek.valueOf(normalized).name();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public record ParsedShift(String day, LocalTime startTime, LocalTime endTime, ShiftMode sessionMode) {
    }
}

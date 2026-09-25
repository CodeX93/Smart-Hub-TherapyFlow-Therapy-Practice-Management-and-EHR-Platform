package com.smart.therapy.flow.common.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class SensitiveDataMasker {

    private static final String MASK = "***";

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "newpassword", "oldpassword", "confirmpassword",
            "authorization", "token", "ticket", "accesstoken", "refreshtoken",
            "resettoken", "passwordresettoken", "verificationtoken", "invitecode",
            "apikey", "api_key", "secret", "clientsecret", "jwt",
            "cookie", "set-cookie", "ssn", "nationalid", "creditcard", "cardnumber", "cvv",
            "name", "firstname", "lastname", "fullname", "clientname", "patientname",
            "dob", "dateofbirth", "birthdate",
            "email", "phone", "telephone", "mobile", "address", "street", "postcode", "zipcode",
            "diagnosis", "condition", "symptom", "note", "clinicalnote", "sessionnote",
            "transcript", "message", "comment", "description", "treatment", "medication",
            "history", "allergy", "emergencycontact", "insurance", "medicalrecord",
            "clientmrn", "mrn",
            "policynumber", "groupnumber", "npi", "memberid", "subscriberid",
            "insuranceprovider", "payerid", "planid", "beneficiaryid", "policyid");

    private static final Set<String> LOGGABLE_HEADERS = Set.of(
            "content-type", "content-length", "accept", "user-agent",
            "x-request-id", "x-correlation-id", "traceparent",
            "authorization", "cookie", "set-cookie");

    private static final Set<String> SAFE_METADATA_KEYS = Set.of(
            "id", "clientid", "organisationid", "organizationid", "tenantid", "userid",
            "sessionid", "documentid", "resourceid", "requestid", "correlationid",
            "page", "size", "limit", "offset", "sort", "order", "direction",
            "status", "type", "action", "method", "path", "uri", "code", "count", "version");

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile(
            "(?:\\d+|[0-9a-fA-F]{8}-[0-9a-fA-F-]{27,}|[A-Za-z0-9_-]{1,80})");

    private static final Pattern EMAIL_LIKE = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_LIKE = Pattern.compile("^[+]?[\\d\\s().-]{7,20}$");

    private final ObjectMapper objectMapper;

    /**
     * True if the given field/key name is considered PHI or a credential (per {@link #SENSITIVE_KEYS}).
     * Exposed for callers that must decide whether to mask a value keyed by field name (e.g. bind or
     * deserialization error details) rather than an entire request body.
     */
    public boolean isSensitiveFieldName(String key) {
        return isSensitiveKey(key);
    }

    /**
     * Masks a value intended for API error details/messages when either the field name is
     * known-sensitive, or the value itself looks like PHI (email address or phone number)
     * regardless of field name. Non-string and non-matching values pass through unchanged so
     * that safe context (e.g. numeric rejected values, enum names) is preserved for debugging.
     */
    public Object maskFieldValue(String fieldName, Object value) {
        if (value == null) {
            return null;
        }
        if (isSensitiveKey(fieldName)) {
            return MASK;
        }
        if (value instanceof CharSequence text && looksLikeContactInfo(text.toString())) {
            return MASK;
        }
        return value;
    }

    private boolean looksLikeContactInfo(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        return EMAIL_LIKE.matcher(trimmed).matches() || PHONE_LIKE.matcher(trimmed).matches();
    }

    public String maskHeaders(HttpServletRequest request) {
        Map<String, String> sanitized = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        if (names == null) {
            return "{}";
        }

        for (String headerName : Collections.list(names)) {
            if (!LOGGABLE_HEADERS.contains(headerName.toLowerCase(Locale.ROOT))) {
                continue;
            }
            List<String> values = Collections.list(request.getHeaders(headerName));
            String joined = String.join(",", values);
            sanitized.put(headerName, safeHeaderValue(headerName, joined));
        }
        return toJsonSafe(sanitized);
    }

    public String sanitizeQueryString(String queryString) {
        if (!StringUtils.hasText(queryString)) {
            return "";
        }
        List<String> sanitized = new ArrayList<>();
        for (String pair : queryString.split("&")) {
            int separator = pair.indexOf('=');
            String rawKey = separator >= 0 ? pair.substring(0, separator) : pair;
            String rawValue = separator >= 0 ? pair.substring(separator + 1) : "";
            String key = decode(rawKey);
            if (isSensitiveKey(key)) {
                sanitized.add(rawKey + "=" + MASK);
            } else if (isSafeMetadataKey(key) && isSafeMetadataValue(key, decode(rawValue))) {
                sanitized.add(rawKey + "=" + rawValue);
            } else {
                sanitized.add(rawKey + "=<omitted>");
            }
        }
        return String.join("&", sanitized);
    }

    public String sanitizeBody(String body, String contentType, int maxLength) {
        if (!StringUtils.hasText(body)) {
            return "";
        }

        String masked = body;
        String lowerContentType = contentType != null ? contentType.toLowerCase(Locale.ROOT) : "";

        if (lowerContentType.contains(MediaType.APPLICATION_JSON_VALUE)) {
            masked = sanitizeJson(masked);
        } else if (lowerContentType.contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE)) {
            masked = sanitizeForm(masked);
        } else {
            // Best-effort masking for plain text payloads that may contain secrets.
            masked = sanitizeText(masked);
        }

        return truncate(masked, maxLength);
    }

    public String summarizeArguments(Object[] args, int maxLength) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        return truncate(toJsonSafe(summarizeArgumentValues(args)), maxLength);
    }

    private List<String> summarizeArgumentValues(Object[] args) {
        List<String> summaries = new ArrayList<>(args.length);
        for (Object arg : args) {
            // Never serialize a raw argument: key-based masking cannot identify
            // unlabelled transcripts, tokens, or clinical fields with unknown names.
            summaries.add(summarizeValue(arg));
        }
        return summaries;
    }

    public String createAuditDetails(Object[] args, Throwable error, int maxLength) {
        Map<String, Object> root = new LinkedHashMap<>();
        if (args != null && args.length > 0) {
            root.put("args", summarizeArgumentValues(args));
        }
        if (error != null) {
            // Exception messages can contain request data. Do not read or serialize them.
            root.put("error", Map.of("type", error.getClass().getSimpleName()));
        }
        return root.isEmpty() ? "" : truncate(toJsonSafe(root), maxLength);
    }

    public String summarizeResult(Object result, int maxLength) {
        return truncate(summarizeValue(result), maxLength);
    }

    private String summarizeValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof HttpServletRequest req) {
            return "HttpServletRequest{method=%s,path=%s}".formatted(req.getMethod(), req.getRequestURI());
        }
        if (value instanceof HttpServletResponse res) {
            return "HttpServletResponse{status=%d,contentType=%s}".formatted(res.getStatus(), res.getContentType());
        }
        if (value instanceof StreamingResponseBody) {
            return "StreamingResponseBody{...}";
        }
        if (value instanceof Resource) {
            return "Resource{...}";
        }
        if (value instanceof InputStream) {
            return "InputStream{...}";
        }
        if (value instanceof ResponseEntity<?> entity && entity.getBody() instanceof StreamingResponseBody) {
            return "ResponseEntity{status=%s,body=StreamingResponseBody{...}}"
                    .formatted(entity.getStatusCode());
        }
        if (value instanceof ResponseEntity<?> entity) {
            return "ResponseEntity{status=%s,body=%s}".formatted(
                    entity.getStatusCode(), summarizeValue(entity.getBody()));
        }
        if (value instanceof MultipartFile file) {
            return "MultipartFile{size=%d,contentType=%s}".formatted(file.getSize(), file.getContentType());
        }
        if (value instanceof byte[] bytes) {
            return "byte[%d]".formatted(bytes.length);
        }
        if (value instanceof CharSequence seq) {
            String text = seq.toString();
            return "String{length=" + text.length() + "}";
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof UUID || value instanceof Enum<?>) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> map) {
            return summarizeMap(map);
        }
        if (value instanceof Iterable<?> iterable) {
            int size = value instanceof java.util.Collection<?> collection ? collection.size() : -1;
            return value.getClass().getSimpleName() + "{size=" + (size >= 0 ? size : "unknown") + "}";
        }
        if (value.getClass().isArray()) {
            return value.getClass().getComponentType().getSimpleName() + "["
                    + java.lang.reflect.Array.getLength(value) + "]";
        }
        return summarizeObjectMetadata(value);
    }

    private String summarizeMap(Map<?, ?> map) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        map.forEach((key, value) -> {
            String stringKey = String.valueOf(key);
            if (isSensitiveKey(stringKey)) {
                metadata.put(stringKey, MASK);
            } else if (isSafeMetadataKey(stringKey) && isSafeMetadataValue(stringKey, value)) {
                metadata.put(stringKey, value);
            }
        });
        return map.getClass().getSimpleName() + "{size=" + map.size() + ",metadata=" + metadata + "}";
    }

    private String summarizeObjectMetadata(Object value) {
        if (value == null) {
            return "null";
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        for (Method method : value.getClass().getMethods()) {
            if (!Modifier.isPublic(method.getModifiers()) || method.getParameterCount() != 0) {
                continue;
            }
            String property = getterProperty(method.getName());
            if (!isSafeMetadataKey(property)) {
                continue;
            }
            try {
                if (!method.canAccess(value) && !method.trySetAccessible()) {
                    continue;
                }
                Object propertyValue = method.invoke(value);
                if (isSafeMetadataValue(property, propertyValue)) {
                    metadata.put(property, propertyValue);
                }
            } catch (Exception ignored) {
                // Logging must never affect application behavior.
            }
        }
        return value.getClass().getSimpleName() + (metadata.isEmpty() ? "{...}" : metadata.toString());
    }

    private String getterProperty(String methodName) {
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return methodName.substring(3, 4).toLowerCase(Locale.ROOT) + methodName.substring(4);
        }
        if (methodName.startsWith("is") && methodName.length() > 2) {
            return methodName.substring(2, 3).toLowerCase(Locale.ROOT) + methodName.substring(3);
        }
        return methodName;
    }

    private String sanitizeJson(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode masked = sanitizeNode(node, null);
            return objectMapper.writeValueAsString(masked);
        } catch (Exception ignored) {
            return sanitizeText(json);
        }
    }

    private JsonNode sanitizeNode(JsonNode node, String parentKey) {
        if (node == null) {
            return null;
        }

        if (node.isObject()) {
            var objectNode = objectMapper.createObjectNode();
            node.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                if (isSensitiveKey(key)) {
                    objectNode.put(key, MASK);
                } else {
                    objectNode.set(key, sanitizeNode(value, key));
                }
            });
            return objectNode;
        }

        if (node.isArray()) {
            var arrayNode = objectMapper.createArrayNode();
            node.forEach(item -> arrayNode.add(sanitizeNode(item, parentKey)));
            return arrayNode;
        }

        if (node.isTextual() && isSensitiveKey(parentKey)) {
            return objectMapper.getNodeFactory().textNode(MASK);
        }

        return node;
    }

    private String sanitizeForm(String formEncoded) {
        String[] pairs = formEncoded.split("&");
        List<String> masked = new ArrayList<>(pairs.length);
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                masked.add(pair);
                continue;
            }
            String key = pair.substring(0, idx);
            String value = pair.substring(idx + 1);
            masked.add(key + "=" + maskByKey(key, value));
        }
        return String.join("&", masked);
    }

    private String sanitizeText(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }

        String masked = text;
        masked = masked.replaceAll("(?i)(authorization\\s*[:=]\\s*)(bearer\\s+)?[^\\s,;]+", "$1" + MASK);
        masked = masked.replaceAll("(?i)(\"?(password|token|secret|api[_-]?key|refresh[_-]?token|access[_-]?token|reset[_-]?token|name|dob|date[_-]?of[_-]?birth|email|phone|diagnosis|symptoms?|notes?|transcript)\"?\\s*[:=]\\s*\")([^\"]*)(\")",
                "$1" + MASK + "$4");
        return masked;
    }

    private String safeHeaderValue(String key, String value) {
        if (isSensitiveKey(key)) {
            return MASK;
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        if (normalized.equals("x-request-id") || normalized.equals("x-correlation-id")
                || normalized.equals("traceparent")) {
            return SAFE_IDENTIFIER.matcher(value).matches() ? value : "<omitted>";
        }
        return sanitizeText(value);
    }

    private String maskByKey(String key, String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return isSensitiveKey(key) ? MASK : value;
    }

    private boolean isSensitiveKey(String key) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        for (String sensitive : SENSITIVE_KEYS) {
            String sensitiveNormalized = sensitive.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
            if (normalized.contains(sensitiveNormalized)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSafeMetadataKey(String key) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        // Sensitive keys take precedence even if they end in "id" (e.g. memberId,
        // subscriberId, beneficiaryId are Safe Harbor identifiers, not opaque PKs),
        // otherwise reflection-based summarization below would leak them.
        if (isSensitiveKey(key)) {
            return false;
        }
        String normalized = normalizeKey(key);
        return SAFE_METADATA_KEYS.contains(normalized)
                || (normalized.endsWith("id") && normalized.length() > 2);
    }

    private boolean isSafeMetadataValue(String key, Object value) {
        if (value == null || value instanceof Number || value instanceof Boolean
                || value instanceof UUID || value instanceof Enum<?>) {
            return true;
        }
        if (!(value instanceof CharSequence)) {
            return false;
        }
        String text = value.toString();
        String normalizedKey = normalizeKey(key);
        if (normalizedKey.endsWith("id") || normalizedKey.equals("code")) {
            return SAFE_IDENTIFIER.matcher(text).matches();
        }
        return text.length() <= 80 && SAFE_IDENTIFIER.matcher(text).matches();
    }

    private String normalizeKey(String key) {
        return key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return value;
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...[truncated]";
    }

    private String toJsonSafe(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ignored) {
            return String.valueOf(value);
        }
    }
}

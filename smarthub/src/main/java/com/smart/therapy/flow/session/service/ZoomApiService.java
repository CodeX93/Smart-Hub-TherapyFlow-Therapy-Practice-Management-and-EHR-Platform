package com.smart.therapy.flow.session.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.integration.zoom.ZoomOauthUrls;
import com.smart.therapy.flow.session.dto.ZoomMeetingResponse;
import com.smart.therapy.flow.user.entity.UserIntegration;
import com.smart.therapy.flow.user.repository.UserIntegrationRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(value = "zoom.enabled", havingValue = "true", matchIfMissing = true)
public class ZoomApiService implements SessionService.ZoomService {

    private static final String DEFAULT_TIMEZONE = "UTC";
    private static final DateTimeFormatter START_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final RestTemplate restTemplate;
    private final UserRepository userRepository;
    private final UserIntegrationRepository userIntegrationRepository;
    private final String apiBaseUrl;
    private final String oauthBaseUrl;

    public ZoomApiService(RestTemplateBuilder restTemplateBuilder,
            UserRepository userRepository,
            UserIntegrationRepository userIntegrationRepository,
            @Value("${zoom.api.connect-timeout-ms:5000}") int connectTimeout,
            @Value("${zoom.api.read-timeout-ms:15000}") int readTimeout,
            @Value("${zoom.api.base-url:https://api.zoom.us/v2}") String apiBaseUrl,
            @Value("${zoom.oauth.base-url:https://zoom.us}") String oauthBaseUrl) {
        this.userRepository = userRepository;
        this.userIntegrationRepository = userIntegrationRepository;
        this.apiBaseUrl = apiBaseUrl.endsWith("/") ? apiBaseUrl.substring(0, apiBaseUrl.length() - 1) : apiBaseUrl;
        this.oauthBaseUrl = oauthBaseUrl;
        this.restTemplate = restTemplateBuilder
                .requestFactory(() -> {
                    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
                    factory.setConnectTimeout(connectTimeout);
                    factory.setReadTimeout(readTimeout);
                    return factory;
                })
                .build();
    }

    @Override
    public boolean isTherapistConfigured(User therapist) {
        if (therapist == null) {
            return false;
        }

        return userIntegrationRepository.findByUserAndIntegrationType(therapist, "zoom")
                .map(integration -> integration.getIsActive() &&
                        StringUtils.hasText(integration.getExternalUserId()) &&
                        integration.getSettings() != null)
                .orElse(false);
    }

    @Override
    public ZoomMeetingResponse createMeeting(Map<String, Object> request, User therapist) {
        validateTherapist(therapist);

        String accessToken = ensureAccessToken(therapist);
        Map<String, Object> payload = buildCreatePayload(request);

        HttpHeaders headers = buildAuthHeaders(accessToken);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiBaseUrl + "/users/me/meetings",
                    HttpMethod.POST,
                    entity,
                    Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("Empty Zoom response when creating meeting");
            }

            ZoomMeetingResponse result = toZoomMeetingResponse(body);
            if (!StringUtils.hasText(result.getPassword())) {
                result.setPassword((String) payload.get("password"));
            }
            log.info("Zoom meeting created successfully: therapistId={}, correlationId={}",
                    therapist.getId(), MDC.get("correlationId"));
            return result;
        } catch (RestClientException ex) {
            log.error("Zoom API error: therapistId={}, correlationId={}",
                    therapist.getId(), MDC.get("correlationId"), ex);
            throw handleZoomException("create", ex);
        }
    }

    public ZoomMeetingResponse createMeetingFallback(Map<String, Object> request, User therapist,
            Exception ex) {
        log.error("Zoom circuit breaker fallback triggered: therapistId={}, correlationId={}",
                therapist != null ? therapist.getId() : "unknown", MDC.get("correlationId"), ex);
        throw new IllegalStateException("Zoom service temporarily unavailable. Please try again later.");
    }

    @Override
    @CircuitBreaker(name = "zoom", fallbackMethod = "updateMeetingFallback")
    @Retry(name = "zoom")
    @TimeLimiter(name = "zoom")
    public ZoomMeetingResponse updateMeeting(String meetingId, Map<String, Object> request,
            User therapist) {
        validateTherapist(therapist);
        if (!StringUtils.hasText(meetingId)) {
            throw new IllegalArgumentException("Meeting id is required for Zoom update");
        }

        String accessToken = ensureAccessToken(therapist);
        Map<String, Object> payload = buildUpdatePayload(request);

        HttpHeaders headers = buildAuthHeaders(accessToken);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            restTemplate.exchange(
                    apiBaseUrl + "/meetings/" + meetingId,
                    HttpMethod.PATCH,
                    entity,
                    Void.class);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiBaseUrl + "/meetings/" + meetingId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("Empty Zoom response when retrieving meeting");
            }

            log.info("Zoom meeting updated successfully: meetingId={}, therapistId={}, correlationId={}",
                    meetingId, therapist.getId(), MDC.get("correlationId"));
            return toZoomMeetingResponse(body);
        } catch (RestClientException ex) {
            log.error("Zoom API error updating meeting: meetingId={}, therapistId={}, correlationId={}",
                    meetingId, therapist.getId(), MDC.get("correlationId"), ex);
            throw handleZoomException("update", ex);
        }
    }

    public ZoomMeetingResponse updateMeetingFallback(String meetingId, Map<String, Object> request,
            User therapist, Exception ex) {
        log.error("Zoom circuit breaker fallback triggered: meetingId={}, therapistId={}, correlationId={}",
                meetingId, therapist != null ? therapist.getId() : "unknown", MDC.get("correlationId"), ex);
        throw new IllegalStateException("Zoom service temporarily unavailable. Please try again later.");
    }

    @Override
    @CircuitBreaker(name = "zoom", fallbackMethod = "cancelMeetingFallback")
    @Retry(name = "zoom")
    public boolean cancelMeeting(String meetingId, User therapist) {
        validateTherapist(therapist);
        if (!StringUtils.hasText(meetingId)) {
            throw new IllegalArgumentException("Meeting id is required for Zoom cancellation");
        }

        String accessToken = ensureAccessToken(therapist);
        HttpHeaders headers = buildAuthHeaders(accessToken);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    apiBaseUrl + "/meetings/" + meetingId,
                    HttpMethod.DELETE,
                    new HttpEntity<>(headers),
                    Void.class);
            boolean success = response.getStatusCode().is2xxSuccessful() || response.getStatusCode() == HttpStatus.NO_CONTENT;
            if (success) {
                log.info("Zoom meeting cancelled successfully: meetingId={}, therapistId={}, correlationId={}",
                        meetingId, therapist.getId(), MDC.get("correlationId"));
            }
            return success;
        } catch (RestClientException ex) {
            // 404/410 = meeting already gone on Zoom side; treat as cancelled successfully.
            Integer statusCode = null;
            if (ex instanceof org.springframework.web.client.HttpStatusCodeException httpEx) {
                statusCode = httpEx.getStatusCode().value();
                if (statusCode == 404 || statusCode == 410) {
                    log.info(
                            "Zoom meeting already removed: meetingId={}, therapistId={}, status={}, correlationId={}",
                            meetingId, therapist.getId(), statusCode, MDC.get("correlationId"));
                    return true;
                }
            }
            log.error(
                    "Zoom API error cancelling meeting: meetingId={}, therapistId={}, status={}, correlationId={}",
                    meetingId, therapist.getId(), statusCode, MDC.get("correlationId"), ex);
            // Do not throw — callers must be able to cancel the session even if Zoom fails.
            return false;
        }
    }

    public boolean cancelMeetingFallback(String meetingId, User therapist, Exception ex) {
        log.error("Zoom cancel fallback triggered: meetingId={}, therapistId={}, correlationId={}",
                meetingId, therapist != null ? therapist.getId() : "unknown", MDC.get("correlationId"), ex);
        return false;
    }

    @Override
    @CircuitBreaker(name = "zoom", fallbackMethod = "testIntegrationFallback")
    @Retry(name = "zoom")
    @TimeLimiter(name = "zoom")
    public boolean testIntegration(User therapist) {
        validateTherapist(therapist);
        String accessToken = ensureAccessToken(therapist);
        HttpHeaders headers = buildAuthHeaders(accessToken);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiBaseUrl + "/users/me",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException ex) {
            log.error("Zoom integration test failed: therapistId={}, correlationId={}",
                    therapist != null ? therapist.getId() : null, MDC.get("correlationId"), ex);
            throw handleZoomException("test", ex);
        }
    }

    public boolean testIntegrationFallback(User therapist, Exception ex) {
        log.error("Zoom integration test fallback triggered: therapistId={}, correlationId={}",
                therapist != null ? therapist.getId() : "unknown", MDC.get("correlationId"), ex);
        return false;
    }

    private void validateTherapist(User therapist) {
        if (!isTherapistConfigured(therapist)) {
            throw new IllegalStateException(
                    "Therapist must configure Zoom OAuth credentials before scheduling sessions");
        }
    }

    private HttpHeaders buildAuthHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return headers;
    }

    private String ensureAccessToken(User therapist) {
        UserIntegration zoomIntegration = userIntegrationRepository
                .findByUserAndIntegrationType(therapist, "zoom")
                .orElseThrow(() -> new IllegalStateException("Zoom integration not configured"));

        Instant now = Instant.now();
        if (StringUtils.hasText(zoomIntegration.getAccessToken())
                && zoomIntegration.getTokenExpiresAt() != null
                && zoomIntegration.getTokenExpiresAt().isAfter(now.plusSeconds(60))) {
            return zoomIntegration.getAccessToken();
        }

        ZoomToken token = fetchAccessToken(zoomIntegration);
        zoomIntegration.setAccessToken(token.value());
        zoomIntegration.setTokenExpiresAt(now.plusSeconds(token.expiresIn() - 60));
        userIntegrationRepository.save(zoomIntegration);
        return token.value();
    }

    private ZoomToken fetchAccessToken(UserIntegration integration) {
        // Extract credentials from settings JSON
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String clientId;
        String clientSecret;

        try {
            if (integration.getSettings() != null) {
                clientId = integration.getSettings().get("clientId").asText();
                clientSecret = integration.getSettings().get("clientSecret").asText();
            } else {
                throw new IllegalStateException("Zoom settings not found");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to extract Zoom credentials from settings", e);
        }

        String credentials = clientId + ":" + clientSecret;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + encoded);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "account_credentials");
        body.add("account_id", integration.getExternalUserId());

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    ZoomOauthUrls.tokenUrl(oauthBaseUrl),
                    HttpMethod.POST,
                    entity,
                    Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                throw new IllegalStateException("Empty Zoom OAuth response");
            }

            String accessToken = (String) responseBody.get("access_token");
            Number expiresNumber = responseBody.get("expires_in") instanceof Number
                    ? (Number) responseBody.get("expires_in")
                    : null;

            if (!StringUtils.hasText(accessToken) || expiresNumber == null) {
                throw new IllegalStateException("Invalid Zoom OAuth response: missing token or expiry");
            }

            return new ZoomToken(accessToken, expiresNumber.longValue());
        } catch (RestClientException ex) {
            throw handleZoomException("fetch access token", ex);
        }
    }

    private Map<String, Object> buildCreatePayload(Map<String, Object> request) {
        Map<String, Object> payload = buildCommonPayload(request);
        payload.putIfAbsent("type", 2);
        payload.put("password", generateMeetingPassword());
        return payload;
    }

    private Map<String, Object> buildUpdatePayload(Map<String, Object> request) {
        Map<String, Object> payload = buildCommonPayload(request);
        payload.remove("password");
        return payload;
    }

    private Map<String, Object> buildCommonPayload(Map<String, Object> request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("topic", String.valueOf(request.getOrDefault("topic", "Therapy Session")));
        payload.put("start_time", formatStartTime(request.get("startTime"), request.get("timezone")));
        payload.put("duration", toInteger(request.get("duration"), 60));
        payload.put("timezone", String.valueOf(request.getOrDefault("timezone", DEFAULT_TIMEZONE)));
        payload.put("settings", buildSettings(request.get("settings")));
        return payload;
    }

    private Map<String, Object> buildSettings(Object settingsObj) {
        Map<String, Object> settings = new HashMap<>();
        settings.put("host_video", true);
        settings.put("participant_video", true);
        settings.put("join_before_host", false);
        settings.put("mute_upon_entry", true);
        settings.put("waiting_room", true);
        settings.put("auto_recording", "none");

        if (settingsObj instanceof Map<?, ?> provided) {
            if (provided.containsKey("video_host")) {
                settings.put("host_video", toBoolean(provided.get("video_host"), true));
            }
            if (provided.containsKey("video_participant")) {
                settings.put("participant_video", toBoolean(provided.get("video_participant"), true));
            }
            if (provided.containsKey("mute_upon_entry")) {
                settings.put("mute_upon_entry", toBoolean(provided.get("mute_upon_entry"), true));
            }
            if (provided.containsKey("waiting_room")) {
                settings.put("waiting_room", toBoolean(provided.get("waiting_room"), true));
            }
        }
        return settings;
    }

    private String formatStartTime(Object rawStartTime, Object timezoneObj) {
        Instant instant = toInstant(rawStartTime);
        ZoneId zoneId = ZoneId.of(timezoneObj != null ? timezoneObj.toString() : DEFAULT_TIMEZONE);
        return START_TIME_FORMATTER.format(instant.atZone(zoneId));
    }

    private Instant toInstant(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Number number) {
            return Instant.ofEpochMilli(number.longValue());
        }
        if (value instanceof String str) {
            try {
                return Instant.parse(str);
            } catch (Exception ignored) {
            }
        }
        return Instant.now();
    }

    private boolean toBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String str) {
            return Boolean.parseBoolean(str);
        }
        return defaultValue;
    }

    private int toInteger(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private ZoomMeetingResponse toZoomMeetingResponse(Map<String, Object> body) {
        ZoomMeetingResponse response = new ZoomMeetingResponse();
        if (body.get("id") != null) {
            response.setMeetingId(String.valueOf(body.get("id")));
        }
        response.setJoinUrl((String) body.get("join_url"));
        response.setPassword((String) body.get("password"));
        return response;
    }

    private IllegalStateException handleZoomException(String action, RestClientException ex) {
        log.error("Zoom API {} failed", action, ex);
        return new IllegalStateException("Zoom API " + action + " failed: " + ex.getMessage(), ex);
    }

    private String generateMeetingPassword() {
        final String characters = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            int index = (int) (Math.random() * characters.length());
            builder.append(characters.charAt(index));
        }
        return builder.toString();
    }

    private record ZoomToken(String value, long expiresIn) {
    }
}

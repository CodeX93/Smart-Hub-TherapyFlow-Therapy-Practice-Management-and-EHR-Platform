package com.smart.therapy.flow.integration.zoom.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.integration.zoom.ZoomOauthUrls;
import com.smart.therapy.flow.integration.zoom.dto.ZoomCredentials;
import com.smart.therapy.flow.integration.zoom.dto.ZoomMeetingRequest;
import com.smart.therapy.flow.integration.zoom.dto.ZoomMeetingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class ZoomService {

    @Value("${zoom.api.base-url:https://api.zoom.us/v2}")
    private String zoomApiBaseUrl;

    @Value("${zoom.oauth.base-url:https://zoom.us}")
    private String zoomOauthBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    public boolean isTherapistConfigured(ZoomCredentials credentials) {
        return credentials != null &&
                credentials.getAccountId() != null && !credentials.getAccountId().isEmpty() &&
                credentials.getClientId() != null && !credentials.getClientId().isEmpty() &&
                credentials.getClientSecret() != null && !credentials.getClientSecret().isEmpty();
    }

    public ZoomMeetingResponse createMeeting(ZoomMeetingRequest request, ZoomCredentials credentials) {
        Objects.requireNonNull(request, "Meeting request is required");
        Objects.requireNonNull(credentials, "Zoom credentials are required");

        if (!isTherapistConfigured(credentials)) {
            throw new IllegalArgumentException("Therapist must configure their own Zoom OAuth credentials in their profile to create Zoom meetings");
        }

        try {
            // Get or refresh access token
            String accessToken = getOrRefreshAccessToken(credentials);

            // Build meeting request
            Map<String, Object> meetingData = new HashMap<>();
            // Get therapist's timezone from request or use default
            String timezone = request.getTimezone() != null ? request.getTimezone() : "America/New_York";
            ZoneId timezoneId = ZoneId.of(timezone);
            
            meetingData.put("topic", "Therapy Session with " + request.getTherapistName());
            meetingData.put("type", 2); // Scheduled meeting
            meetingData.put("start_time", formatZoomDateTime(request.getSessionDate(), timezoneId));
            meetingData.put("duration", request.getDuration() != null ? request.getDuration() : 60);
            meetingData.put("timezone", timezone);
            meetingData.put("password", generateMeetingPassword());

            Map<String, Object> settings = new HashMap<>();
            settings.put("host_video", true);
            settings.put("participant_video", true);
            settings.put("join_before_host", false);
            settings.put("mute_upon_entry", true);
            settings.put("waiting_room", true);
            settings.put("auto_recording", "none");
            meetingData.put("settings", settings);

            // Create meeting
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(meetingData, headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    zoomApiBaseUrl + "/users/me/meetings",
                    HttpMethod.POST,
                    entity,
                    JsonNode.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Failed to create Zoom meeting: " + response.getStatusCode());
            }

            JsonNode result = response.getBody();
            return ZoomMeetingResponse.builder()
                    .meetingId(result.get("id").asLong())
                    .hostId(result.get("host_id").asText())
                    .topic(result.get("topic").asText())
                    .startTime(result.get("start_time").asText())
                    .duration(result.get("duration").asInt())
                    .timezone(result.get("timezone").asText())
                    .joinUrl(result.get("join_url").asText())
                    .password(result.has("password") ? result.get("password").asText() : null)
                    .build();

        } catch (Exception e) {
            log.error("Failed to create Zoom meeting", e);
            throw new RuntimeException("Failed to create Zoom meeting: " + e.getMessage(), e);
        }
    }

    private String getOrRefreshAccessToken(ZoomCredentials credentials) {
        // Check if token is valid
        if (credentials.getAccessToken() != null && credentials.getTokenExpiry() != null) {
            if (credentials.getTokenExpiry().isAfter(Instant.now().plusSeconds(300))) { // 5 min buffer
                return credentials.getAccessToken();
            }
        }

        // Refresh token
        return refreshAccessToken(credentials);
    }

    private String refreshAccessToken(ZoomCredentials credentials) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(credentials.getClientId(), credentials.getClientSecret());

            String body = "grant_type=account_credentials&account_id=" + credentials.getAccountId();

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    ZoomOauthUrls.tokenUrl(zoomOauthBaseUrl),
                    HttpMethod.POST,
                    entity,
                    JsonNode.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Failed to refresh Zoom access token");
            }

            JsonNode result = response.getBody();
            return result.get("access_token").asText();
        } catch (Exception e) {
            log.error("Failed to refresh Zoom access token", e);
            throw new RuntimeException("Failed to refresh Zoom access token: " + e.getMessage(), e);
        }
    }

    private String formatZoomDateTime(Instant instant, ZoneId timezone) {
        return instant.atZone(timezone)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    }

    private String generateMeetingPassword() {
        // Generate 8-character alphanumeric password
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }
}


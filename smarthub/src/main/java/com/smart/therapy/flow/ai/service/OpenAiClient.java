package com.smart.therapy.flow.ai.service;

import com.smart.therapy.flow.common.exception.TranscriptionServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@Slf4j
public class OpenAiClient {

    private static final String CLINICAL_TRANSCRIPTION_PROMPT =
            "Therapy session recording. Clinical vocabulary: therapist, client, anxiety, depression, trauma, "
                    + "cognitive behavioral therapy, mindfulness, coping skills, medication, diagnosis, treatment plan, "
                    + "session notes, mental health, psychotherapy, intervention, symptoms, progress.";

    private static final Pattern WORD_SPLIT = Pattern.compile("\\s+");

    private final RestTemplate restTemplate;

    @Value("${ais.integrations.openai.api-key}")
    private String apiKey;

    @Value("${ais.integrations.openai.whisper-api-key:}")
    private String whisperApiKey;

    @Value("${ais.integrations.openai.fallback-api-key:}")
    private String fallbackApiKey;

    @Value("${ais.integrations.openai.base-url}")
    private String baseUrl;

    @Value("${app.transcripts.transcription-model:gpt-4o-mini-transcribe}")
    private String transcriptionModel;

    @Value("${app.transcripts.mock-transcription:false}")
    private boolean mockTranscription;

    @Value("${ai.models.session-notes:gpt-4o}")
    private String translationModel;

    @Value("${app.env:dev}")
    private String appEnvironment;

    @Value("${app.ai.phi-processing-approved:false}")
    private boolean phiProcessingApproved;

    public OpenAiClient(RestTemplateBuilder builder,
                        @Value("${ais.integrations.openai.api-key}") String apiKey,
                        @Value("${ais.integrations.openai.base-url}") String baseUrl,
                        @Value("${ai.openai.connect-timeout-ms:5000}") int connectTimeout,
                        @Value("${ai.openai.read-timeout-ms:60000}") int readTimeout) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;

        this.restTemplate = builder
                .requestFactory(() -> {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(connectTimeout);
                    factory.setReadTimeout(readTimeout);
                    return factory;
                })
                .build();
    }


    @SuppressWarnings("unchecked")
    @CircuitBreaker(name = "openai", fallbackMethod = "createChatCompletionFallback")
    @Retry(name = "openai")
    public String createChatCompletion(String model,
                                       List<Map<String, String>> messages,
                                       Double temperature,
                                       Integer maxTokens) {
        return createChatCompletion(model, messages, temperature, maxTokens, null);
    }

    @SuppressWarnings("unchecked")
    @CircuitBreaker(name = "openai", fallbackMethod = "createChatCompletionWithOptionsFallback")
    @Retry(name = "openai")
    public String createChatCompletion(String model,
                                       List<Map<String, String>> messages,
                                       Double temperature,
                                       Integer maxTokens,
                                       Map<String, Object> extraPayload) {
        requireApprovedPhiProcessing();
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("OpenAI API key is not configured");
        }

        if (CollectionUtils.isEmpty(messages)) {
            throw new IllegalArgumentException("OpenAI request requires at least one message");
        }

        log.debug("OpenAI API call: model={}, correlationId={}", model, MDC.get("correlationId"));

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("messages", messages);
        if (temperature != null) {
            payload.put("temperature", temperature);
        }
        if (maxTokens != null) {
            payload.put("max_tokens", maxTokens);
        }
        if (!CollectionUtils.isEmpty(extraPayload)) {
            payload.putAll(extraPayload);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/chat/completions",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("Empty response from OpenAI");
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
            if (CollectionUtils.isEmpty(choices)) {
                throw new IllegalStateException("OpenAI response contains no choices");
            }

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) {
                throw new IllegalStateException("OpenAI response missing message content");
            }

            Object content = message.get("content");
            if (content == null) {
                throw new IllegalStateException("OpenAI response content was null");
            }

            return content.toString().trim();
        } catch (HttpClientErrorException ex) {
            log.error("OpenAI chat completion HTTP error: status={}, model={}, correlationId={}",
                    ex.getStatusCode().value(), model, MDC.get("correlationId"));
            throw new IllegalStateException(
                    "OpenAI chat completion failed with HTTP " + ex.getStatusCode().value(), ex);
        } catch (RestClientException ex) {
            log.error("OpenAI chat completion request failed: model={}, correlationId={}, errorType={}",
                    model, MDC.get("correlationId"), ex.getClass().getSimpleName());
            throw new IllegalStateException("Failed to communicate with approved AI provider", ex);
        }
    }

    public String createChatCompletionWithOptionsFallback(String model,
                                                          List<Map<String, String>> messages,
                                                          Double temperature,
                                                          Integer maxTokens,
                                                          Map<String, Object> extraPayload,
                                                          Exception ex) {
        return createChatCompletionFallback(model, messages, temperature, maxTokens, ex);
    }

    public String createChatCompletionFallback(String model,
                                                List<Map<String, String>> messages,
                                                Double temperature,
                                                Integer maxTokens,
                                                Exception ex) {
        Throwable rootCause = findRootCause(ex);
        HttpClientErrorException httpError = findHttpClientError(ex);
        if (httpError != null) {
            log.error("OpenAI chat completion fallback triggered after HTTP error: status={}, model={}, correlationId={}",
                    httpError.getStatusCode().value(),
                    model,
                    MDC.get("correlationId"));
        } else {
            log.error("OpenAI chat completion fallback triggered: model={}, correlationId={}, rootCauseType={}",
                    model,
                    MDC.get("correlationId"),
                    rootCause != null ? rootCause.getClass().getSimpleName() : "unknown");
        }
        throw new IllegalStateException("AI service temporarily unavailable. Please try again later.");
    }

    private HttpClientErrorException findHttpClientError(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof HttpClientErrorException httpClientErrorException) {
                return httpClientErrorException;
            }
            current = current.getCause();
        }
        return null;
    }

    private Throwable findRootCause(Throwable ex) {
        Throwable current = ex;
        Throwable root = ex;
        while (current != null) {
            root = current;
            current = current.getCause();
        }
        return root;
    }

    /**
     * Transcribe audio using OpenAI Whisper API
     * @param audioData The audio file bytes
     * @param fileName The original filename (for format detection)
     * @return The transcribed text
     */
    @CircuitBreaker(name = "openai", fallbackMethod = "transcribeAudioFallback")
    @Retry(name = "openai")
    public String transcribeAudio(byte[] audioData, String fileName) {
        return transcribeSessionChunk(audioData, fileName, null, null, false);
    }

    /**
     * Transcribe one session recording chunk with clinical prompt and optional continuity from previous chunk text.
     */
    @CircuitBreaker(name = "openai", fallbackMethod = "transcribeSessionChunkFallback")
    @Retry(name = "openai")
    public String transcribeSessionChunk(byte[] audioData,
                                         String fileName,
                                         String language,
                                         String previousText,
                                         boolean translateToEnglish) {
        requireApprovedPhiTranscription();
        if (audioData == null || audioData.length == 0) {
            throw new IllegalArgumentException("Audio data is required");
        }

        if (mockTranscription) {
            log.warn("Mock transcription enabled; skipping OpenAI call for fileName={}, size={} bytes",
                    fileName, audioData.length);
            return "[Mock transcription] Audio chunk received (" + audioData.length + " bytes).";
        }

        String transcript = transcribeWithKeyFallback(audioData, fileName, language, previousText);

        if (translateToEnglish && StringUtils.hasText(transcript)) {
            transcript = translateToEnglish(transcript);
        }

        return transcript;
    }

    public String transcribeSessionChunkFallback(byte[] audioData,
                                                 String fileName,
                                                 String language,
                                                 String previousText,
                                                 boolean translateToEnglish,
                                                 Exception ex) {
        TranscriptionServiceUnavailableException unavailable = findTranscriptionServiceUnavailable(ex);
        if (unavailable != null) {
            throw unavailable;
        }
        log.error("OpenAI session chunk transcription fallback triggered: correlationId={}, errorType={}",
                MDC.get("correlationId"), ex.getClass().getSimpleName());
        throw new TranscriptionServiceUnavailableException(
                "Audio transcription service temporarily unavailable. Please try again later.", ex);
    }

    public String transcribeAudioFallback(byte[] audioData, String fileName, Exception ex) {
        return transcribeSessionChunkFallback(audioData, fileName, null, null, false, ex);
    }

    private static TranscriptionServiceUnavailableException findTranscriptionServiceUnavailable(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof TranscriptionServiceUnavailableException unavailable) {
                return unavailable;
            }
            current = current.getCause();
        }
        return null;
    }

    private record TranscriptionKeyCandidate(String envVar, String value) {
    }

    private String transcribeWithKeyFallback(byte[] audioData, String fileName, String language, String previousText) {
        List<TranscriptionKeyCandidate> keys = resolveTranscriptionKeys();
        if (keys.isEmpty()) {
            throw new TranscriptionServiceUnavailableException(
                    "AI transcription is not configured. Set OPENAI_WHISPER_API_KEY or AI_INTEGRATIONS_OPENAI_API_KEY.");
        }

        List<String> attemptErrors = new ArrayList<>();
        for (int index = 0; index < keys.size(); index++) {
            TranscriptionKeyCandidate candidate = keys.get(index);
            boolean hasAlternateKey = index < keys.size() - 1;
            try {
                return callTranscriptionApi(candidate, audioData, fileName, language, previousText);
            } catch (IllegalStateException ex) {
                attemptErrors.add(formatTranscriptionAttemptError(candidate.envVar(), ex.getMessage()));
                if (hasAlternateKey && isRetriableWithNextKey(ex)) {
                    log.warn("Transcription failed with key source {}; trying next configured key, errorType={}",
                            candidate.envVar(), ex.getClass().getSimpleName());
                    continue;
                }
                throw new TranscriptionServiceUnavailableException(buildTranscriptionFailureMessage(attemptErrors), ex);
            }
        }

        throw new TranscriptionServiceUnavailableException(buildTranscriptionFailureMessage(attemptErrors));
    }

    private List<TranscriptionKeyCandidate> resolveTranscriptionKeys() {
        List<TranscriptionKeyCandidate> keys = new ArrayList<>();
        addTranscriptionKey(keys, "OPENAI_WHISPER_API_KEY", whisperApiKey);
        addTranscriptionKey(keys, "AI_INTEGRATIONS_OPENAI_API_KEY", apiKey);
        addTranscriptionKey(keys, "AI_INTEGRATIONS_OPENAI_FALLBACK_API_KEY", fallbackApiKey);
        return keys;
    }

    private static void addTranscriptionKey(List<TranscriptionKeyCandidate> keys, String envVar, String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        boolean alreadyPresent = keys.stream().anyMatch(candidate -> candidate.value().equals(key));
        if (!alreadyPresent) {
            keys.add(new TranscriptionKeyCandidate(envVar, key));
        }
    }

    private static String formatTranscriptionAttemptError(String envVar, String message) {
        return envVar + ": " + (StringUtils.hasText(message) ? message : "request failed");
    }

    private static String buildTranscriptionFailureMessage(List<String> attemptErrors) {
        String attempts = String.join("; ", attemptErrors);
        return "AI transcription failed after trying configured API keys. "
                + attempts
                + ". Check OPENAI_WHISPER_API_KEY and AI_INTEGRATIONS_OPENAI_API_KEY, billing, and quota.";
    }

    private boolean isRetriableWithNextKey(IllegalStateException ex) {
        String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        if (message.contains("http 400") || message.contains("http 413") || message.contains("http 422")) {
            return false;
        }
        return true;
    }

    private String callTranscriptionApi(TranscriptionKeyCandidate keyCandidate,
                                        byte[] audioData,
                                        String fileName,
                                        String language,
                                        String previousText) {
        log.debug("OpenAI transcription call: model={}, fileName={}, size={} bytes, correlationId={}",
                transcriptionModel, fileName, audioData.length, MDC.get("correlationId"));

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            ByteArrayResource fileResource = new ByteArrayResource(audioData) {
                @Override
                public String getFilename() {
                    return fileName != null ? fileName : "audio.webm";
                }
            };
            body.add("file", fileResource);
            body.add("model", transcriptionModel);
            body.add("response_format", "text");
            body.add("temperature", "0");

            String prompt = buildTranscriptionPrompt(previousText);
            if (StringUtils.hasText(prompt)) {
                body.add("prompt", prompt);
            }

            if (StringUtils.hasText(language)) {
                String openAiLanguage = normalizeOpenAiLanguage(language);
                if (StringUtils.hasText(openAiLanguage)) {
                    body.add("language", openAiLanguage);
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(keyCandidate.value());

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/audio/transcriptions",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            String transcription = response.getBody();
            if (transcription == null) {
                return "";
            }

            return transcription.trim();
        } catch (HttpClientErrorException ex) {
            String responseBody = ex.getResponseBodyAsString();
            log.error("OpenAI transcription HTTP error: status={}, model={}, correlationId={}",
                    ex.getStatusCode().value(), transcriptionModel, MDC.get("correlationId"));
            if (ex.getStatusCode().value() == 429 || containsQuotaError(responseBody)) {
                throw new IllegalStateException("HTTP 429 quota exceeded for " + keyCandidate.envVar()
                        + ". Add billing/credits or set app.transcripts.mock-transcription=true for local testing.", ex);
            }
            if (ex.getStatusCode().value() == 401) {
                throw new IllegalStateException("HTTP 401 invalid API key for " + keyCandidate.envVar(), ex);
            }
            if (ex.getStatusCode().value() == 403) {
                throw new IllegalStateException("HTTP 403 access denied for " + keyCandidate.envVar(), ex);
            }
            throw new IllegalStateException(
                    "Transcription provider returned HTTP " + ex.getStatusCode().value(), ex);
        } catch (RestClientException ex) {
            log.error("OpenAI transcription request failed: model={}, correlationId={}, errorType={}",
                    transcriptionModel, MDC.get("correlationId"), ex.getClass().getSimpleName());
            throw new IllegalStateException("Failed to communicate with approved transcription provider", ex);
        }
    }

    private void requireApprovedPhiProcessing() {
        if (isProduction() && !phiProcessingApproved) {
            log.error("AI PHI processing blocked because provider approval is not configured");
            throw new IllegalStateException("AI processing is unavailable pending provider compliance approval");
        }
    }

    private void requireApprovedPhiTranscription() {
        if (isProduction() && !phiProcessingApproved) {
            log.error("AI transcription blocked because provider approval is not configured");
            throw new TranscriptionServiceUnavailableException(
                    "Audio transcription is unavailable pending provider compliance approval");
        }
    }

    private boolean isProduction() {
        return "prod".equalsIgnoreCase(appEnvironment)
                || "production".equalsIgnoreCase(appEnvironment);
    }

    /**
     * OpenAI Whisper expects ISO 639-1 base codes (e.g. {@code en}), not regional tags like {@code en-us}.
     */
    static String normalizeOpenAiLanguage(String language) {
        if (!StringUtils.hasText(language)) {
            return null;
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        if ("auto".equals(normalized) || "multi".equals(normalized)) {
            return null;
        }
        int dash = normalized.indexOf('-');
        if (dash > 0) {
            normalized = normalized.substring(0, dash);
        }
        return normalized;
    }

    private static boolean containsQuotaError(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return false;
        }
        String lower = responseBody.toLowerCase();
        return lower.contains("insufficient_quota") || lower.contains("exceeded your current quota");
    }

    private String buildTranscriptionPrompt(String previousText) {
        StringBuilder prompt = new StringBuilder(CLINICAL_TRANSCRIPTION_PROMPT);
        if (StringUtils.hasText(previousText)) {
            prompt.append(" Previous context: ").append(tailWords(previousText, 200));
        }
        return prompt.toString();
    }

    private String tailWords(String text, int maxWords) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String[] words = WORD_SPLIT.split(text.trim());
        if (words.length <= maxWords) {
            return text.trim();
        }
        return String.join(" ", Arrays.copyOfRange(words, words.length - maxWords, words.length));
    }

    private String translateToEnglish(String transcript) {
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content",
                        "Translate the following therapy session transcript to English. Preserve clinical meaning and speaker intent."),
                Map.of("role", "user", "content", transcript)
        );
        return createChatCompletion(translationModel, messages, 0.0, null);
    }
}

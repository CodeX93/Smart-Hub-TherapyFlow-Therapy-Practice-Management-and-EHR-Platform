package com.smart.therapy.flow.transcription.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

@Component
@Slf4j
public class DeepgramLiveClient {

    private static final Set<String> FALLBACK_LANGUAGES = Set.of(
            "auto", "multi", "en", "en-us", "en-gb", "ur", "ar", "es", "fr", "de", "it", "pt", "tr", "hi", "ja", "nl", "ru"
    );

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String deepgramApiKey;
    private final String deepgramBaseWsUrl;
    private final Set<String> languageAllowlist;

    public DeepgramLiveClient(
            @Value("${deepgram.api.key:}") String deepgramApiKey,
            @Value("${deepgram.live.ws-url:wss://api.deepgram.com/v1/listen}") String deepgramBaseWsUrl,
            @Value("${app.transcripts.language-allowlist:auto,multi,en,en-us,en-gb,es,fr,de,it,pt,nl,ru,hi,zh,ja,ko,tr,pl,ar,ur}") String languageAllowlist,
            ObjectMapper objectMapper
    ) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = objectMapper;
        this.deepgramApiKey = deepgramApiKey;
        this.deepgramBaseWsUrl = deepgramBaseWsUrl;
        this.languageAllowlist = parseAllowlist(languageAllowlist);
    }

    public LiveStreamConnection connect(String language, String uploadId, LiveStreamListener streamListener) {
        if (!StringUtils.hasText(deepgramApiKey)) {
            throw new IllegalStateException("Deepgram API key is not configured");
        }

        String normalizedLanguage = normalizeLanguage(language);
        URI uri = URI.create(buildDeepgramUrl(normalizedLanguage));
        LiveStreamConnection connection = new LiveStreamConnection();
        log.info("Connecting Deepgram live stream: uploadId={} uri={}", uploadId, uri);

        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public void onOpen(WebSocket webSocket) {
                webSocket.request(1);
                log.debug("Deepgram stream open for uploadId={}", uploadId);
                connection.markReady();
                streamListener.onOpen(connection);
            }

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                try {
                    LiveTranscriptMessage message = toTranscriptMessage(data.toString(), uploadId);
                    if (message != null) {
                        streamListener.onTranscript(message);
                    }
                } catch (Exception ex) {
                    log.warn("Failed to process Deepgram event for uploadId={}: {}", uploadId, ex.getMessage());
                    streamListener.onError("Deepgram response processing failed: " + safeMessage(ex));
                } finally {
                    webSocket.request(1);
                }
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                log.debug("Deepgram stream closed for uploadId={} status={} reason={}", uploadId, statusCode, reason);
                streamListener.onClose(statusCode, reason);
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                log.warn("Deepgram ws error for uploadId={}: {}", uploadId, error.getMessage());
                streamListener.onError("Deepgram websocket error: " + safeMessage(error));
            }
        };

        WebSocket socket = httpClient.newWebSocketBuilder()
                .header("Authorization", "Token " + deepgramApiKey)
                .buildAsync(uri, listener)
                .handle((webSocket, error) -> {
                    if (error != null) {
                        throw unwrapHandshakeFailure(error, uri, uploadId);
                    }
                    return webSocket;
                })
                .join();
        connection.setSocket(socket);
        return connection;
    }

    public CompletableFuture<WebSocket> sendAudio(WebSocket socket, ByteBuffer payload) {
        return socket.sendBinary(payload, true);
    }

    public CompletableFuture<WebSocket> sendFinalize(WebSocket socket) {
        return socket.sendText("{\"type\":\"Finalize\"}", true);
    }

    public CompletableFuture<WebSocket> close(WebSocket socket) {
        return socket.sendClose(WebSocket.NORMAL_CLOSURE, "Client closed stream");
    }

    private String buildDeepgramUrl(String language) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(deepgramBaseWsUrl)
                .queryParam("model", "nova-3-general")
                .queryParam("interim_results", true)
                .queryParam("punctuate", true)
                .queryParam("smart_format", true)
                .queryParam("endpointing", 300);

        // Browser live preview sends MediaRecorder webm/opus chunks, not raw opus frames.
        // Deepgram should infer the containerized audio stream; forcing raw encoding/sample_rate
        // can cause the websocket handshake/request to be rejected.
        builder.queryParam("language", normalizeStreamingLanguage(language));

        return builder.build(true).toUriString();
    }

    private String normalizeStreamingLanguage(String language) {
        return "auto".equals(language) ? "multi" : language;
    }

    private String safeMessage(Throwable error) {
        if (error == null || !StringUtils.hasText(error.getMessage())) {
            return error == null ? "unknown error" : error.getClass().getSimpleName();
        }
        return error.getMessage();
    }

    private RuntimeException unwrapHandshakeFailure(Throwable error, URI uri, String uploadId) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null
                ? error.getCause()
                : error;
        if (cause instanceof java.net.http.WebSocketHandshakeException handshake) {
            HttpResponse<?> response = handshake.getResponse();
            int status = response != null ? response.statusCode() : -1;
            String details = "Deepgram websocket handshake failed: status="
                    + status
                    + ", uri="
                    + uri;
            log.warn("{} uploadId={}", details, uploadId);
            return new IllegalStateException(details, handshake);
        }
        String details = "Deepgram websocket connection failed: " + safeMessage(cause) + ", uri=" + uri;
        log.warn("{} uploadId={}", details, uploadId);
        return new IllegalStateException(details, cause);
    }

    private String normalizeLanguage(String language) {
        String v = StringUtils.hasText(language) ? language.trim().toLowerCase() : "auto";
        if (!languageAllowlist.contains(v)) {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
        return v;
    }

    private Set<String> parseAllowlist(String allowlistRaw) {
        if (!StringUtils.hasText(allowlistRaw)) {
            return FALLBACK_LANGUAGES;
        }
        java.util.Set<String> parsed = new java.util.HashSet<>();
        String[] pieces = allowlistRaw.split(",");
        for (String piece : pieces) {
            String value = piece == null ? null : piece.trim().toLowerCase();
            if (StringUtils.hasText(value)) {
                parsed.add(value);
            }
        }
        return parsed.isEmpty() ? FALLBACK_LANGUAGES : Set.copyOf(parsed);
    }

    private LiveTranscriptMessage toTranscriptMessage(String rawPayload, String uploadId) throws Exception {
        JsonNode root = objectMapper.readTree(rawPayload);
        JsonNode channel = root.path("channel");
        JsonNode alternatives = channel.path("alternatives");
        if (!alternatives.isArray() || alternatives.isEmpty()) {
            return null;
        }

        String text = alternatives.get(0).path("transcript").asText("");
        boolean isFinal = root.path("is_final").asBoolean(false);
        boolean speechFinal = root.path("speech_final").asBoolean(false);

        if (!StringUtils.hasText(text)) {
            return null;
        }

        log.debug("Deepgram transcript event uploadId={} isFinal={} speechFinal={} chars={}",
                uploadId, isFinal, speechFinal, text.length());
        return LiveTranscriptMessage.transcript(text.trim(), isFinal, speechFinal);
    }

    public interface LiveStreamListener {
        void onOpen(LiveStreamConnection connection);

        void onTranscript(LiveTranscriptMessage message);

        void onError(String message);

        void onClose(int statusCode, String reason);
    }

    public static final class LiveStreamConnection {
        private volatile WebSocket socket;
        private volatile boolean ready;

        public WebSocket socket() {
            return socket;
        }

        public boolean isReady() {
            return ready;
        }

        private void setSocket(WebSocket socket) {
            this.socket = socket;
        }

        private void markReady() {
            this.ready = true;
        }
    }
}

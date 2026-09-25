package com.smart.therapy.flow.transcription.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.session.entity.SessionTranscript;
import com.smart.therapy.flow.session.service.SessionTranscriptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.http.WebSocket;
import java.util.ArrayDeque;
import java.util.Deque;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class TranscribeLiveWebSocketHandler extends TextWebSocketHandler {

    private static final String ATTR_UPLOAD_ID = "uploadId";
    private static final int MAX_UPLOAD_ID_LENGTH = 64;

    private final SessionTranscriptService sessionTranscriptService;
    private final DeepgramLiveClient deepgramLiveClient;
    private final ObjectMapper objectMapper;

    @Value("${deepgram.live.max-frame-bytes:262144}")
    private int maxFrameBytes;

    @Value("${deepgram.live.max-buffered-bytes:1048576}")
    private int maxBufferedBytes;

    @Value("${deepgram.live.idle-timeout-seconds:90}")
    private long idleTimeoutSeconds;

    private final Map<String, BridgeState> bridges = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        AuthPrincipal principal = readPrincipal(session);
        String tenantSchema = (String) session.getAttributes().get(TranscribeLiveHandshakeInterceptor.ATTR_TENANT_SCHEMA);
        Long orgId = (Long) session.getAttributes().get(TranscribeLiveHandshakeInterceptor.ATTR_ORG_ID);

        String uploadId = queryParam(session, "uploadId");
        if (!StringUtils.hasText(uploadId)) {
            session.close(CloseStatus.BAD_DATA.withReason("Missing uploadId"));
            return;
        }
        String normalizedUploadId = uploadId.trim();
        if (!normalizedUploadId.startsWith("srv-") || normalizedUploadId.length() > MAX_UPLOAD_ID_LENGTH) {
            session.close(CloseStatus.BAD_DATA.withReason("Invalid uploadId"));
            return;
        }

        String language = queryParam(session, "language");

        SessionTranscript transcript = withTenantContext(tenantSchema, orgId,
                () -> sessionTranscriptService.validateLiveUploadAccess(normalizedUploadId, principal));

        if (StringUtils.hasText(language)
                && StringUtils.hasText(transcript.getLanguage())
                && !transcript.getLanguage().equalsIgnoreCase(language)) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("language must match started upload"));
            return;
        }

        BridgeState state = new BridgeState(normalizedUploadId, Instant.now());
        bridges.put(session.getId(), state);
        session.getAttributes().put(ATTR_UPLOAD_ID, normalizedUploadId);

        try {
            DeepgramLiveClient.LiveStreamConnection connection = deepgramLiveClient.connect(
                    language,
                    normalizedUploadId,
                    new DeepgramLiveClient.LiveStreamListener() {
                        @Override
                        public void onOpen(DeepgramLiveClient.LiveStreamConnection upstreamConnection) {
                            state.markReady(upstreamConnection);
                            log.info("Deepgram upstream connected: uploadId={}", normalizedUploadId);
                            flushPendingFrames(session, state, transcript.getLanguage());
                        }

                        @Override
                        public void onTranscript(LiveTranscriptMessage message) {
                            state.incrementTranscriptEvent(message);
                            forwardTranscriptMessage(session, state, message);
                        }

                        @Override
                        public void onError(String message) {
                            log.warn("Deepgram upstream/provider error: uploadId={} message={}", normalizedUploadId, message);
                            sendErrorAndClose(session, state, message, CloseStatus.SERVER_ERROR);
                        }

                        @Override
                        public void onClose(int statusCode, String reason) {
                            if (state.isClosed() || !session.isOpen()) {
                                return;
                            }
                            log.warn("Deepgram upstream closed unexpectedly: uploadId={} status={} reason={}",
                                    normalizedUploadId, statusCode, reason);
                            sendErrorAndClose(session, state,
                                    "Deepgram stream closed during live preview"
                                            + " (status=" + statusCode + ", reason=" + normalizeReason(reason) + ")",
                                    CloseStatus.SERVER_ERROR);
                        }
                    });

            if (connection.isReady()) {
                state.markReady(connection);
                log.info("Deepgram upstream connected: uploadId={}", normalizedUploadId);
            }
        } catch (Exception ex) {
            sendErrorAndClose(session, state,
                    "Deepgram unavailable for live preview: " + safeMessage(ex),
                    CloseStatus.SERVER_ERROR);
            return;
        }

        log.info("Live transcript WS connected: sessionId={}, uploadId={}, language={}",
                session.getId(), normalizedUploadId, transcript.getLanguage());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        BridgeState state = bridges.get(session.getId());
        if (state == null) {
            closeQuietly(session, CloseStatus.SERVER_ERROR.withReason("Proxy state missing"));
            return;
        }

        if (Duration.between(state.lastActivity(), Instant.now()).toSeconds() > idleTimeoutSeconds) {
            closeQuietly(session, CloseStatus.POLICY_VIOLATION.withReason("Idle timeout"));
            return;
        }

        int size = message.getPayloadLength();
        if (size <= 0 || size > maxFrameBytes) {
            sendErrorAndClose(session, state,
                    "Invalid live audio frame size: received " + size + " bytes, max " + maxFrameBytes,
                    CloseStatus.TOO_BIG_TO_PROCESS.withReason("Invalid frame size"));
            return;
        }

        if (state.totalBufferedBytes() + size > maxBufferedBytes) {
            log.warn("Live transcript buffer exceeded: uploadId={} bufferedBytes={} incomingBytes={}",
                    state.uploadId(), state.totalBufferedBytes(), size);
            sendErrorAndClose(session, state,
                    "Buffered live audio limit exceeded: buffered " + state.totalBufferedBytes()
                            + " bytes, incoming " + size + " bytes, max " + maxBufferedBytes,
                    CloseStatus.POLICY_VIOLATION.withReason("Buffered audio limit exceeded"));
            return;
        }

        state.recordFrame();
        if (state.totalFramesReceived() == 1) {
            log.info("First audio frame received: uploadId={} bytes={}", state.uploadId(), size);
        }

        byte[] payloadBytes = new byte[size];
        message.getPayload().asReadOnlyBuffer().get(payloadBytes);
        state.enqueuePendingFrame(payloadBytes);
        if (!state.isReady()) {
            log.debug("Buffered early live frame: uploadId={} pendingFrames={} pendingBytes={}",
                    state.uploadId(), state.pendingFrameCount(), state.pendingBytes());
            return;
        }

        drainQueuedFrames(session, state);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        BridgeState state = bridges.get(session.getId());
        if (state == null) {
            session.close(CloseStatus.SERVER_ERROR.withReason("Proxy state missing"));
            return;
        }

        String payload = message.getPayload();
        if ("ping".equalsIgnoreCase(payload)) {
            sendJson(session, LiveTranscriptMessage.builder().type("pong").build());
            state.setLastActivity(Instant.now());
            return;
        }

        if ("finalize".equalsIgnoreCase(payload)) {
            if (state.deepgramSocket() != null) {
                deepgramLiveClient.sendFinalize(state.deepgramSocket());
            }
            state.setLastActivity(Instant.now());
            return;
        }

        session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unsupported text message"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        BridgeState state = bridges.remove(session.getId());
        if (state != null) {
            try {
                if (state.deepgramSocket() != null) {
                    deepgramLiveClient.sendFinalize(state.deepgramSocket());
                    deepgramLiveClient.close(state.deepgramSocket());
                }
            } catch (Exception ex) {
                log.debug("Deepgram close ignored for uploadId={}: {}", state.uploadId(), ex.getMessage());
            }
        }
        if (state != null) {
            log.info("Live transcript WS closed: sessionId={}, uploadId={}, reason={}, frames={}, interimEvents={}, finalEvents={}, forwardedEvents={}",
                    session.getId(),
                    state.uploadId(),
                    status,
                    state.totalFramesReceived(),
                    state.interimTranscriptEvents(),
                    state.finalTranscriptEvents(),
                    state.forwardedTranscriptEvents());
        } else {
            log.info("Live transcript WS closed: sessionId={}, reason={}", session.getId(), status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("Live transcript WS transport error: sessionId={}, error={}", session.getId(), exception.getMessage());
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (Exception ignored) {
            // no-op
        }
    }

    private AuthPrincipal readPrincipal(WebSocketSession session) {
        Object value = session.getAttributes().get(TranscribeLiveHandshakeInterceptor.ATTR_AUTH_PRINCIPAL);
        if (!(value instanceof AuthPrincipal principal)) {
            throw new IllegalStateException("Missing authenticated principal in websocket session");
        }
        return principal;
    }

    private String queryParam(WebSocketSession session, String key) {
        if (session.getUri() == null) {
            return null;
        }
        return UriComponentsBuilder.fromUri(session.getUri())
                .build()
                .getQueryParams()
                .getFirst(key);
    }

    private <T> T withTenantContext(String schemaName, Long orgId, java.util.function.Supplier<T> supplier) {
        try {
            TenantContext.setSchemaName(schemaName);
            TenantContext.setOrganisationId(orgId);
            return supplier.get();
        } finally {
            TenantContext.clear();
        }
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) {
                session.close(status);
            }
        } catch (Exception ignored) {
            // no-op
        }
    }

    private void flushPendingFrames(WebSocketSession session, BridgeState state, String language) {
        if (state.pendingFrameCount() > 0) {
            log.info("Flushing buffered live frames: uploadId={} frameCount={} bytes={}",
                    state.uploadId(), state.pendingFrameCount(), state.pendingBytes());
        }
        drainQueuedFrames(session, state);
    }

    private void drainQueuedFrames(WebSocketSession session, BridgeState state) {
        if (!session.isOpen() || state.isClosed() || !state.isReady() || !state.tryStartFrameSend()) {
            return;
        }

        byte[] payloadBytes = state.pollPendingFrame();
        if (payloadBytes == null) {
            state.finishFrameSend();
            return;
        }

        sendFrameToDeepgram(session, state, payloadBytes);
    }

    private void sendFrameToDeepgram(WebSocketSession session, BridgeState state, byte[] payloadBytes) {
        if (state.deepgramSocket() == null) {
            state.enqueuePendingFrame(payloadBytes);
            return;
        }

        int size = payloadBytes.length;
        state.incrementInflightBytes(size);
        deepgramLiveClient.sendAudio(state.deepgramSocket(), ByteBuffer.wrap(payloadBytes))
                .whenComplete((ignored, ex) -> {
                    state.setLastActivity(Instant.now());
                    state.decrementInflightBytes(size);
                    if (ex != null) {
                        state.finishFrameSend();
                        log.warn("Failed forwarding live audio frame: uploadId={} error={}", state.uploadId(), ex.getMessage());
                        sendErrorAndClose(session, state,
                                "Deepgram frame forwarding failed: " + safeMessage(ex),
                                CloseStatus.SERVER_ERROR);
                        return;
                    }
                    state.finishFrameSend();
                    drainQueuedFrames(session, state);
                });
    }

    private void forwardTranscriptMessage(WebSocketSession session, BridgeState state, LiveTranscriptMessage message) {
        try {
            sendJson(session, message);
            state.incrementForwardedTranscriptEvents();
            log.debug("Transcript event forwarded to client: uploadId={} isFinal={} speechFinal={} chars={}",
                    state.uploadId(), message.getIsFinal(), message.getSpeechFinal(),
                    message.getText() != null ? message.getText().length() : 0);
        } catch (IOException ex) {
            log.warn("Failed to forward transcript event to client: uploadId={} error={}", state.uploadId(), ex.getMessage());
            sendErrorAndClose(session, state,
                    "Failed to deliver live transcript event: " + safeMessage(ex),
                    CloseStatus.SERVER_ERROR);
        }
    }

    private void sendJson(WebSocketSession session, Object payload) throws IOException {
        if (!session.isOpen()) {
            return;
        }
        synchronized (session) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        }
    }

    private void sendErrorAndClose(WebSocketSession session, BridgeState state, String message, CloseStatus status) {
        if (state != null && state.markClosed()) {
            try {
                sendJson(session, LiveTranscriptMessage.error(message));
            } catch (Exception ex) {
                log.debug("Failed to send live transcript error payload: {}", ex.getMessage());
            }
            log.info("Live transcript error emitted: uploadId={} reason={}", state.uploadId(), status);
        }
        closeQuietly(session, status);
    }

    private String safeMessage(Throwable error) {
        if (error == null || !StringUtils.hasText(error.getMessage())) {
            return error == null ? "unknown error" : error.getClass().getSimpleName();
        }
        return error.getMessage();
    }

    private String normalizeReason(String reason) {
        return StringUtils.hasText(reason) ? reason : "n/a";
    }

    private static final class BridgeState {
        private final String uploadId;
        private volatile Instant lastActivity;
        private volatile WebSocket deepgramSocket;
        private volatile boolean ready;
        private volatile boolean closed;
        private volatile int inflightBytes;
        private volatile int pendingBytes;
        private volatile long totalFramesReceived;
        private volatile long interimTranscriptEvents;
        private volatile long finalTranscriptEvents;
        private volatile long forwardedTranscriptEvents;
        private boolean frameSendInProgress;
        private final Deque<byte[]> pendingFrames;

        private BridgeState(String uploadId, Instant lastActivity) {
            this.uploadId = uploadId;
            this.lastActivity = lastActivity;
            this.pendingFrames = new ArrayDeque<>();
        }

        public String uploadId() {
            return uploadId;
        }

        public WebSocket deepgramSocket() {
            return deepgramSocket;
        }

        public void markReady(DeepgramLiveClient.LiveStreamConnection connection) {
            this.deepgramSocket = connection.socket();
            this.ready = true;
        }

        public boolean isReady() {
            return ready;
        }

        public Instant lastActivity() {
            return lastActivity;
        }

        public void setLastActivity(Instant lastActivity) {
            this.lastActivity = lastActivity;
        }

        public synchronized void enqueuePendingFrame(byte[] bytes) {
            this.pendingFrames.addLast(bytes);
            this.pendingBytes += bytes.length;
        }

        public synchronized int pendingFrameCount() {
            return pendingFrames.size();
        }

        public int pendingBytes() {
            return pendingBytes;
        }

        public synchronized byte[] pollPendingFrame() {
            byte[] next = pendingFrames.pollFirst();
            if (next != null) {
                pendingBytes = Math.max(0, pendingBytes - next.length);
            }
            return next;
        }

        public synchronized boolean tryStartFrameSend() {
            if (frameSendInProgress) {
                return false;
            }
            frameSendInProgress = true;
            return true;
        }

        public synchronized void finishFrameSend() {
            frameSendInProgress = false;
        }

        public void incrementInflightBytes(int size) {
            inflightBytes += size;
        }

        public void decrementInflightBytes(int size) {
            inflightBytes = Math.max(0, inflightBytes - size);
        }

        public int totalBufferedBytes() {
            return pendingBytes + inflightBytes;
        }

        public void recordFrame() {
            totalFramesReceived++;
            lastActivity = Instant.now();
        }

        public long totalFramesReceived() {
            return totalFramesReceived;
        }

        public void incrementTranscriptEvent(LiveTranscriptMessage message) {
            if (Boolean.TRUE.equals(message.getIsFinal())) {
                finalTranscriptEvents++;
            } else {
                interimTranscriptEvents++;
            }
        }

        public long interimTranscriptEvents() {
            return interimTranscriptEvents;
        }

        public long finalTranscriptEvents() {
            return finalTranscriptEvents;
        }

        public void incrementForwardedTranscriptEvents() {
            forwardedTranscriptEvents++;
        }

        public long forwardedTranscriptEvents() {
            return forwardedTranscriptEvents;
        }

        public synchronized boolean markClosed() {
            if (closed) {
                return false;
            }
            closed = true;
            return true;
        }

        public boolean isClosed() {
            return closed;
        }
    }
}

package com.smart.therapy.flow.transcription.ws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TranscribeLiveWebSocketHandler transcribeLiveWebSocketHandler;
    private final TranscribeLiveHandshakeInterceptor transcribeLiveHandshakeInterceptor;
    private final String allowedOrigins;

    public WebSocketConfig(
            TranscribeLiveWebSocketHandler transcribeLiveWebSocketHandler,
            TranscribeLiveHandshakeInterceptor transcribeLiveHandshakeInterceptor,
            @Value("${CORS_ALLOWED_ORIGINS:}") String allowedOrigins
    ) {
        this.transcribeLiveWebSocketHandler = transcribeLiveWebSocketHandler;
        this.transcribeLiveHandshakeInterceptor = transcribeLiveHandshakeInterceptor;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = resolveAllowedOriginPatterns();
        registry.addHandler(transcribeLiveWebSocketHandler, "/ws/transcribe-live")
                .addInterceptors(transcribeLiveHandshakeInterceptor)
                .setAllowedOriginPatterns(origins);
    }

    @Bean
    @ConditionalOnProperty(
            name = "app.transcription.websocket-container.enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public ServletServerContainerFactoryBean webSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // Browser audio frames are ~16 KB, so raise the container limit above Tomcat's default 8 KB.
        container.setMaxBinaryMessageBufferSize(262_144);
        container.setMaxTextMessageBufferSize(262_144);
        container.setAsyncSendTimeout(30_000L);
        return container;
    }

    private String[] resolveAllowedOriginPatterns() {
        if (StringUtils.hasText(allowedOrigins)) {
            return java.util.Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toArray(String[]::new);
        }
        return new String[]{
                "http://localhost:3000",
                "http://localhost:8080",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:8080",
                "https://trappy-flow-frontend.vercel.app"
        };
    }
}

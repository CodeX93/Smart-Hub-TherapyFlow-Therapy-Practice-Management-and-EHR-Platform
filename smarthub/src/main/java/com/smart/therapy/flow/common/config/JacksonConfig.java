package com.smart.therapy.flow.common.config;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Objects;

/**
 * Jackson configuration for HTTP request/response JSON parsing.
 * Registers a lenient JSON message converter (allows unescaped control chars in strings)
 * only for JSON; the shared builder is unchanged so XML and other converters still work.
 */
@Configuration
public class JacksonConfig {

    /**
     * Lenient ObjectMapper for JSON: allows unescaped control characters (e.g. newlines)
     * in string values, for clients that send them in fields like {@code workingHours}.
     */
    @Bean
    public ObjectMapper lenientJsonObjectMapper() {
        return JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .addModule(new JavaTimeModule())
            .build();
    }

    @Bean
    public MappingJackson2HttpMessageConverter lenientJackson2HttpMessageConverter(
            ObjectMapper lenientJsonObjectMapper) {
        return new MappingJackson2HttpMessageConverter(lenientJsonObjectMapper) {
            @Override
            public boolean canWrite(@NonNull Class<?> clazz, @Nullable MediaType mediaType) {
                // Keep ByteArrayHttpMessageConverter in charge of byte[] responses
                // (e.g. springdoc /v3/api-docs), otherwise Jackson encodes them as base64 strings.
                if (byte[].class == clazz) {
                    return false;
                }
                return super.canWrite(clazz, mediaType);
            }
        };
    }

    /**
     * Prepend the lenient JSON converter so it is used for application/json instead of
     * the default one. Does not affect XML or other converters.
     */
    @Bean
    public WebMvcConfigurer lenientJsonConverterConfigurer(
            MappingJackson2HttpMessageConverter lenientJackson2HttpMessageConverter) {
        return new WebMvcConfigurer() {
            @Override
            public void extendMessageConverters(@NonNull List<org.springframework.http.converter.HttpMessageConverter<?>> converters) {
                converters.add(0, lenientJackson2HttpMessageConverter);
            }
        };
    }
}

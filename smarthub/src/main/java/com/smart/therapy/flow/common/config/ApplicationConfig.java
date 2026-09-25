package com.smart.therapy.flow.common.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.web.client.RestTemplate;

/**
 * Application-wide infrastructure configuration.
 *
 * <p>Note: Beans that implement {@link org.springframework.beans.factory.config.BeanPostProcessor}
 * (such as {@link MethodValidationPostProcessor}) should be declared via {@code static} @Bean
 * methods so that they can be instantiated early and participate in processing all other beans
 * without triggering "not eligible for auto-proxying" warnings.</p>
 */
@Configuration
public class ApplicationConfig {

    /**
     * Configure method-level validation support (@Validated on beans).
     * Uses Spring Boot's auto-configured {@link javax.validation.Validator}.
     */
    @Bean
    public static MethodValidationPostProcessor methodValidationPostProcessor() {
        return new MethodValidationPostProcessor();
    }

    /**
     * Shared RestTemplate bean for HTTP client usage (e.g. UserService, external APIs).
     * Uses RestTemplateBuilder for sensible defaults (timeouts, message converters).
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}

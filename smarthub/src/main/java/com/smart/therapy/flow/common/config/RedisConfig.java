package com.smart.therapy.flow.common.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smart.therapy.flow.common.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis is optional. Azure/dev environments may run without Redis
 * ({@code app.redis.enabled=false}); caching and locks then fail open.
 * <p>
 * <b>PHI:</b> Cached values must not store decrypted PHI DTOs (e.g. full
 * {@code ClientResponse} with name/email/phone/DOB). Prefer ids, opaque keys,
 * or non-PHI aggregates. The {@code clients} cache region is configured below
 * but ClientService currently only uses {@code @CacheEvict} (no {@code @Cacheable}
 * on ClientResponse) — keep it that way unless a non-PHI cache key strategy is designed.
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    private final Environment environment;

    @Value("${app.redis.enabled:false}")
    private boolean redisEnabled;

    @Value("${app.redis.fail-open:true}")
    private boolean redisFailOpen;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean redisSslEnabled;

    public RedisConfig(Environment environment) {
        this.environment = environment;
    }

    /**
     * Jackson Redis serializer must support java.time types used by cached DTOs
     * (e.g. Instant on assessment/library/user responses). The default
     * {@link GenericJackson2JsonRedisSerializer} does not register JavaTimeModule
     * and fails cache puts with SerializationException, which surfaces as HTTP 500.
     */
    private static GenericJackson2JsonRedisSerializer redisJsonSerializer() {
        return new GenericJackson2JsonRedisSerializer().configure(mapper -> {
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        });
    }

    @Override
    @Nullable
    public CacheErrorHandler errorHandler() {
        if (!redisFailOpen) {
            return CachingConfigurer.super.errorHandler();
        }
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis cache GET failed (fail-open) cache={} key={} error={}",
                        cache != null ? cache.getName() : null, key, exception.toString());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, @Nullable Object value) {
                log.warn("Redis cache PUT failed (fail-open) cache={} key={} error={}",
                        cache != null ? cache.getName() : null, key, exception.toString());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis cache EVICT failed (fail-open) cache={} key={} error={}",
                        cache != null ? cache.getName() : null, key, exception.toString());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Redis cache CLEAR failed (fail-open) cache={} error={}",
                        cache != null ? cache.getName() : null, exception.toString());
            }
        };
    }

    private String resolveHost() {
        String fromEnv = firstNonBlank(
                environment.getProperty("REDIS_HOST"),
                environment.getProperty("SPRING_DATA_REDIS_HOST"),
                redisHost);
        return fromEnv != null ? fromEnv : "localhost";
    }

    private int resolvePort() {
        String fromEnv = firstNonBlank(
                environment.getProperty("REDIS_PORT"),
                environment.getProperty("SPRING_DATA_REDIS_PORT"));
        if (fromEnv != null) {
            try {
                return Integer.parseInt(fromEnv.trim());
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return redisPort > 0 ? redisPort : 6379;
    }

    private String resolvePassword() {
        return firstNonBlank(
                environment.getProperty("REDIS_PASSWORD"),
                environment.getProperty("SPRING_DATA_REDIS_PASSWORD"),
                redisPassword);
    }

    /**
     * Azure Cache for Redis uses TLS on 6380. Prefer explicit config, but also honor
     * REDIS_SSL_ENABLED / SPRING_DATA_REDIS_SSL_ENABLED and auto-enable SSL on 6380.
     */
    private boolean resolveSslEnabled(int port) {
        if (port == 6380 || redisSslEnabled) {
            return true;
        }
        return Boolean.parseBoolean(environment.getProperty("REDIS_SSL_ENABLED", "false"))
                || Boolean.parseBoolean(environment.getProperty("SPRING_DATA_REDIS_SSL_ENABLED", "false"));
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @Bean
    @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
    public RedisConnectionFactory redisConnectionFactory() {
        String host = resolveHost();
        int port = resolvePort();
        String password = resolvePassword();
        boolean ssl = resolveSslEnabled(port);
        boolean passwordPresent = password != null && !password.isEmpty();
        log.info("Configuring Redis Lettuce client host={} port={} ssl={} passwordPresent={}",
                host, port, ssl, passwordPresent);

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);
        if (passwordPresent) {
            config.setPassword(password);
        }
        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfig =
                LettuceClientConfiguration.builder()
                        .commandTimeout(Duration.ofSeconds(10));
        if (ssl) {
            clientConfig.useSsl();
        }
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig.build());
        factory.afterPropertiesSet();
        return factory;
    }

    @Bean
    @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer jsonSerializer = redisJsonSerializer();
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer);
        return template;
    }

    @Bean
    @Primary
    public CacheManager cacheManager(Optional<RedisConnectionFactory> connectionFactory) {
        if (!redisEnabled || connectionFactory.isEmpty()) {
            log.info("Redis disabled (app.redis.enabled=false). Using NoOpCacheManager.");
            return new NoOpCacheManager();
        }

        try {
            var connection = connectionFactory.get().getConnection();
            connection.ping();
            connection.close();
            log.info("Redis connection successful. Using Redis cache manager.");

            GenericJackson2JsonRedisSerializer jsonSerializer = redisJsonSerializer();
            RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofHours(1))
                    .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                    .computePrefixWith(cacheName -> {
                        String schema = TenantContext.getSchemaName();
                        Long orgId = TenantContext.getOrganisationId();
                        String schemaPart = (schema != null && !schema.isBlank()) ? schema : "public";
                        String orgPart = orgId != null ? orgId.toString() : "no-org";
                        return cacheName + "::tenant:" + schemaPart + ":org:" + orgPart + "::";
                    })
                    .disableCachingNullValues();

            return RedisCacheManager.builder(connectionFactory.get())
                    .cacheDefaults(config)
                    .withCacheConfiguration("clients", config.entryTtl(Duration.ofMinutes(30)))
                    .withCacheConfiguration("users", config.entryTtl(Duration.ofMinutes(30)))
                    .withCacheConfiguration("sessions", config.entryTtl(Duration.ofMinutes(15)))
                    .withCacheConfiguration("services", config.entryTtl(Duration.ofHours(1)))
                    .withCacheConfiguration("systemOptions", config.entryTtl(Duration.ofHours(24)))
                    .withCacheConfiguration("library", config.entryTtl(Duration.ofHours(12)))
                    .withCacheConfiguration("documents", config.entryTtl(Duration.ofMinutes(30)))
                    .withCacheConfiguration("tasks", config.entryTtl(Duration.ofMinutes(15)))
                    .withCacheConfiguration("assessments", config.entryTtl(Duration.ofHours(1)))
                    .withCacheConfiguration("forms", config.entryTtl(Duration.ofHours(1)))
                    .withCacheConfiguration("superAdminKpis", config.entryTtl(Duration.ofMinutes(5)))
                    .withCacheConfiguration("superAdminDashboard", config.entryTtl(Duration.ofMinutes(5)))
                    .withCacheConfiguration("checklists", config.entryTtl(Duration.ofHours(1)))
                    .build();
        } catch (Exception e) {
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            String host = resolveHost();
            int port = resolvePort();
            log.warn(
                    "Redis connection failed host={} port={} ssl={} passwordPresent={} message={} root={} ({})",
                    host,
                    port,
                    resolveSslEnabled(port),
                    resolvePassword() != null && !resolvePassword().isEmpty(),
                    e.getMessage(),
                    root.getClass().getSimpleName(),
                    root.getMessage());
            return new NoOpCacheManager();
        }
    }
}

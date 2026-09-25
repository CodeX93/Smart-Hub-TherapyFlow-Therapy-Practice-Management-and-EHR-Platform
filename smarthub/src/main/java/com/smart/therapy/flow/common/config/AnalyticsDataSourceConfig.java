package com.smart.therapy.flow.common.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

/**
 * Optional analytics read-replica data source.
 * Falls back to primary if not configured.
 */
@Configuration
public class AnalyticsDataSourceConfig {

    public static final String ANALYTICS_DATASOURCE = "analyticsDataSource";
    public static final String ANALYTICS_JDBC_TEMPLATE = "analyticsJdbcTemplate";

    @Value("${app.analytics.datasource.url:}")
    private String url;

    @Value("${app.analytics.datasource.username:}")
    private String username;

    @Value("${app.analytics.datasource.password:}")
    private String password;

    @Value("${app.analytics.datasource.pool-size:5}")
    private int poolSize;

    @Value("${app.analytics.datasource.minimum-idle:1}")
    private int minimumIdle;

    @Value("${app.analytics.datasource.connection-timeout-ms:30000}")
    private long connectionTimeoutMs;

    @Value("${app.analytics.datasource.idle-timeout-ms:300000}")
    private long idleTimeoutMs;

    @Value("${app.analytics.datasource.max-lifetime-ms:600000}")
    private long maxLifetimeMs;

    @Bean(name = ANALYTICS_DATASOURCE)
    @ConditionalOnProperty(prefix = "app.analytics.datasource", name = "url")
    public DataSource analyticsDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        if (username != null && !username.isBlank()) {
            config.setUsername(username);
        }
        if (password != null && !password.isBlank()) {
            config.setPassword(password);
        }
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(minimumIdle);
        config.setPoolName("AnalyticsReadReplicaPool");
        config.setConnectionTimeout(connectionTimeoutMs);
        config.setIdleTimeout(idleTimeoutMs);
        config.setMaxLifetime(maxLifetimeMs);
        return new HikariDataSource(config);
    }

    @Bean(name = ANALYTICS_JDBC_TEMPLATE)
    @ConditionalOnBean(name = ANALYTICS_DATASOURCE)
    public NamedParameterJdbcTemplate analyticsJdbcTemplate(
            @Qualifier(ANALYTICS_DATASOURCE) DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}

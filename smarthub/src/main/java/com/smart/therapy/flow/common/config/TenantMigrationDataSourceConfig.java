package com.smart.therapy.flow.common.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Dedicated small pool for tenant provisioning and Flyway tenant migrations.
 * Never use the main request pool for DDL or mass migration to avoid exhaustion.
 */
@Configuration
public class TenantMigrationDataSourceConfig {

    public static final String TENANT_MIGRATION_DATASOURCE = "tenantMigrationDataSource";

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username:postgres}")
    private String username;

    @Value("${spring.datasource.password:}")
    private String password;

    @Value("${tenant.migration.pool-size:10}")
    private int poolSize;

    @Value("${tenant.migration.minimum-idle:2}")
    private int minimumIdle;

    @Value("${tenant.migration.connection-timeout-ms:60000}")
    private long connectionTimeoutMs;

    @Value("${tenant.migration.idle-timeout-ms:300000}")
    private long idleTimeoutMs;

    @Value("${tenant.migration.max-lifetime-ms:600000}")
    private long maxLifetimeMs;

    @Bean(name = TENANT_MIGRATION_DATASOURCE)
    public DataSource tenantMigrationDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(poolSize);
        config.setMinimumIdle(minimumIdle);
        config.setPoolName("TenantMigrationPool");
        config.setConnectionTimeout(connectionTimeoutMs);
        config.setIdleTimeout(idleTimeoutMs);
        config.setMaxLifetime(maxLifetimeMs);
        return new HikariDataSource(config);
    }
}

package com.smart.therapy.flow.common.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** Keep request traffic on its configured pool when auxiliary datasource beans are present. */
@Configuration(proxyBeanMethods = false)
public class MainDataSourceConfig {
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource dataSource(DataSourceProperties properties) {
        // Declaring a migration/analytics datasource makes Boot back off from creating this pool.
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }
}

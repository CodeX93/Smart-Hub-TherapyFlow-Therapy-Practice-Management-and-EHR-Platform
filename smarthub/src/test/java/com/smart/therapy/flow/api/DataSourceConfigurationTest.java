package com.smart.therapy.flow.api;

import com.smart.therapy.flow.common.BaseTenantApiTest;
import com.smart.therapy.flow.common.tenant.SchemaMultiTenantConnectionProvider;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class DataSourceConfigurationTest extends BaseTenantApiTest {
    @Autowired private DataSource primaryDataSource;
    @Autowired @Qualifier("tenantMigrationDataSource") private DataSource migrationDataSource;
    @Autowired private SchemaMultiTenantConnectionProvider tenantConnections;

    @Test
    void shouldKeepTenantRequestsOnConfiguredPrimaryPool() throws Exception {
        assertThat(primaryDataSource).isNotSameAs(migrationDataSource).isInstanceOf(HikariDataSource.class);
        assertThat(((HikariDataSource) primaryDataSource).getMaximumPoolSize()).isEqualTo(201);
        assertThat(((HikariDataSource) migrationDataSource).getMaximumPoolSize()).isEqualTo(10);
        assertThat(ReflectionTestUtils.getField(tenantConnections, "dataSource")).isSameAs(primaryDataSource);
        try (var requestConnection = primaryDataSource.getConnection();
             var migrationConnection = migrationDataSource.getConnection()) {
            assertThat(requestConnection.isValid(5)).isTrue();
            assertThat(migrationConnection.getCatalog()).isEqualTo(requestConnection.getCatalog());
        }
    }
}

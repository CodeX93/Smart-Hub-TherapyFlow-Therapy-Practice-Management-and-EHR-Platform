package com.smart.therapy.flow.common;

import org.springframework.test.context.DynamicPropertyRegistry;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Separate Hibernate DDL contexts must not recreate each other's public identity tables. */
final class TenantTestDatabase {
    private static final Map<String, String> DATABASE_URLS = new HashMap<>();

    private TenantTestDatabase() {
    }

    static synchronized void register(DynamicPropertyRegistry registry, String family) {
        if (!family.matches("[a-z]+")) {
            throw new IllegalArgumentException("Tenant fixture family must contain lowercase letters only");
        }
        ConnectionSettings settings = resolveSettings(environment());
        String url = DATABASE_URLS.computeIfAbsent(family, ignored -> createDatabase(settings, family));
        registry.add("spring.datasource.url", () -> url);
    }

    private static String createDatabase(ConnectionSettings settings, String family) {
        String source = settings.url();
        String name = "qa_" + family + "_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(source, settings.username(), settings.password());
             var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + name);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to create isolated tenant fixture database", ex);
        }
        // These databases share only the disposable server and are removed with its run-owned container.
        return source.substring(0, source.lastIndexOf('/') + 1) + name;
    }

    static ConnectionSettings resolveSettings(Map<String, String> values) {
        String source = required(values, "DB_URL");
        String owner = required(values, "QA_MANAGED_DATABASE");
        if (!owner.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalStateException("Tenant fixture database owner marker is invalid");
        }
        try {
            URI uri = new URI(source.substring("jdbc:".length()));
            if (!"postgresql".equals(uri.getScheme())
                    || !isLoopback(uri.getHost())
                    || uri.getPort() < 1
                    || !"/therapyflow_test".equals(uri.getPath())) {
                throw new IllegalStateException("Tenant fixture database must be a local disposable PostgreSQL instance");
            }
        } catch (URISyntaxException | IndexOutOfBoundsException exception) {
            throw new IllegalStateException("Tenant fixture database must be a local disposable PostgreSQL instance", exception);
        }
        return new ConnectionSettings(source, required(values, "DB_USERNAME"), required(values, "DB_PASSWORD"));
    }

    private static boolean isLoopback(String host) {
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    }

    private static String required(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Tenant fixture requires " + key + " from the disposable database runner");
        }
        return value;
    }

    private static Map<String, String> environment() {
        Map<String, String> values = new HashMap<>();
        for (String key : new String[]{"DB_URL", "DB_USERNAME", "DB_PASSWORD", "QA_MANAGED_DATABASE"}) {
            String property = System.getProperty(key);
            String environment = System.getenv(key);
            values.put(key, property != null && !property.isBlank() ? property : environment);
        }
        return values;
    }

    record ConnectionSettings(String url, String username, String password) {
    }
}

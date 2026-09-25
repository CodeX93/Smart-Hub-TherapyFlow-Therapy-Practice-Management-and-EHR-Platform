package com.smart.therapy.flow.tools;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * One-shot tenant migration runner (Flyway + default seeds).
 * Usage:
 * <ul>
 *   <li>{@code DB_PASSWORD=... java -cp ... TenantMigrationTrigger migrate <orgId>} — Flyway + seeds for one org</li>
 *   <li>{@code DB_PASSWORD=... java -cp ... TenantMigrationTrigger seed <orgId>} — seeds only (after manual clone)</li>
 *   <li>{@code DB_PASSWORD=... java -cp ... TenantMigrationTrigger} — migrate all tenants</li>
 * </ul>
 * Prefer {@code POST /api/v1/super-admin/organisations/{id}/reprovision} when the API is running (includes staff bootstrap).
 */
public final class TenantMigrationTrigger {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SEED_RESOURCE = "system-options/default-tenant-categories.json";
    private static final String LIBRARY_SEED_RESOURCE = "library/default-tenant-library.json";
    private static final List<String> LIBRARY_ROOT_INSERT_ORDER = List.of(
            "Session Focus",
            "Symptoms",
            "Short-term Goals",
            "Interventions",
            "Progress",
            "Anxiety",
            "Depression");

    private TenantMigrationTrigger() {
    }

    public static void main(String[] args) throws Exception {
        String url = env("DB_URL", "jdbc:postgresql://client-hub.postgres.database.azure.com:5432/therapyflow?sslmode=require");
        String user = env("DB_USERNAME", "clientHub");
        String password = env("DB_PASSWORD", null);
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("DB_PASSWORD is required");
        }

        if (args.length >= 2 && "seed".equalsIgnoreCase(args[0])) {
            long organisationId = Long.parseLong(args[1]);
            runSeedForOrganisation(url, user, password, organisationId);
            return;
        }

        if (args.length >= 2 && "migrate".equalsIgnoreCase(args[0])) {
            long organisationId = Long.parseLong(args[1]);
            SeedFile seedFile = loadSeedFile();
            LibrarySeedFile librarySeedFile = loadLibrarySeedFile();
            try (Connection connection = DriverManager.getConnection(url, user, password)) {
                String schemaName = loadSchemaName(connection, organisationId);
                System.out.println("Migrating org " + organisationId + " schema " + schemaName);
                runFlyway(url, user, password, schemaName);
                seedTenantDefaults(connection, organisationId, schemaName, seedFile, librarySeedFile);
                updateSchemaVersion(connection, organisationId, schemaName, url, user, password);
            }
            System.out.println("Tenant migration complete for org " + organisationId);
            return;
        }

        SeedFile seedFile = loadSeedFile();
        LibrarySeedFile librarySeedFile = loadLibrarySeedFile();
        List<TenantRef> tenants = loadTenants(url, user, password);

        int success = 0;
        int failed = 0;
        for (TenantRef tenant : tenants) {
            try {
                runFlyway(url, user, password, tenant.schemaName());
                try (Connection connection = DriverManager.getConnection(url, user, password)) {
                    seedTenantDefaults(connection, tenant.organisationId(), tenant.schemaName(), seedFile, librarySeedFile);
                    updateSchemaVersion(connection, tenant.organisationId(), tenant.schemaName(), url, user, password);
                }
                System.out.println("Migrated org " + tenant.organisationId() + " schema " + tenant.schemaName());
                success++;
            } catch (Exception ex) {
                failed++;
                System.err.println("Failed org " + tenant.organisationId() + " schema " + tenant.schemaName() + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }

        System.out.println("Tenant migration trigger complete: success=" + success + " failed=" + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void runFlyway(String url, String user, String password, String schemaName) {
        Flyway flyway = Flyway.configure()
                .dataSource(url, user, password)
                .schemas(schemaName)
                .defaultSchema(schemaName)
                .createSchemas(true)
                .table("tenant_flyway_schema_history")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .outOfOrder(true)
                .locations("classpath:db/tenant_migration")
                .validateOnMigrate(false)
                .ignoreMigrationPatterns("*:missing")
                .load();
        var result = flyway.migrate();
        if (result != null && result.migrationsExecuted > 0) {
            System.out.println("  Flyway executed " + result.migrationsExecuted + " migration(s) for " + schemaName);
        }
    }

    private static void runSeedForOrganisation(String url, String user, String password, long organisationId) throws Exception {
        SeedFile seedFile = loadSeedFile();
        LibrarySeedFile librarySeedFile = loadLibrarySeedFile();
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            String schemaName = loadSchemaName(connection, organisationId);
            System.out.println("Seeding defaults for org " + organisationId + " schema " + schemaName);
            seedTenantDefaults(connection, organisationId, schemaName, seedFile, librarySeedFile);
        }
        System.out.println("Tenant defaults seed complete for org " + organisationId);
        System.out.println("Note: staff profiles require POST /api/v1/super-admin/organisations/"
                + organisationId + "/staff-profiles/bootstrap or synchronous reprovision API.");
    }

    private static void seedTenantDefaults(
            Connection connection,
            long organisationId,
            String schemaName,
            SeedFile seedFile,
            LibrarySeedFile librarySeedFile
    ) throws SQLException {
        seedSystemOptions(connection, schemaName, seedFile);
        seedLibrary(connection, schemaName, librarySeedFile);
        seedBillingServices(connection, schemaName);
    }

    private static void seedSystemOptions(Connection connection, String schemaName, SeedFile seedFile) throws SQLException {

        String optionsTable = qualified(schemaName, "system_options");
        String categoriesTable = qualified(schemaName, "option_categories");

        try (Statement statement = connection.createStatement()) {
            statement.execute("DELETE FROM " + optionsTable);
            statement.execute("DELETE FROM " + categoriesTable);
        }

        Instant now = Instant.now();
        Timestamp timestamp = Timestamp.from(now);
        for (SeedCategory category : seedFile.categories()) {
            long categoryId;
            try (PreparedStatement insertCategory = connection.prepareStatement("""
                    INSERT INTO %s
                    (createdat, created_by, deleted_at, is_deleted, updatedat, updated_by, version,
                     category_key, category_name, description, is_active, is_system)
                    VALUES (?, 0, NULL, false, ?, 0, 0, ?, ?, ?, ?, ?)
                    RETURNING id
                    """.formatted(categoriesTable))) {
                insertCategory.setTimestamp(1, timestamp);
                insertCategory.setTimestamp(2, timestamp);
                insertCategory.setString(3, category.categoryKey());
                insertCategory.setString(4, category.categoryName());
                insertCategory.setString(5, category.description());
                insertCategory.setBoolean(6, category.isActive());
                insertCategory.setBoolean(7, category.isSystem());
                try (ResultSet keys = insertCategory.executeQuery()) {
                    if (!keys.next()) {
                        throw new SQLException("Failed to insert category " + category.categoryKey());
                    }
                    categoryId = keys.getLong(1);
                }
            }

            if (category.options() == null || category.options().isEmpty()) {
                continue;
            }

            try (PreparedStatement insertOption = connection.prepareStatement("""
                    INSERT INTO %s
                    (createdat, created_by, is_deleted, updatedat, updated_by, version,
                     is_active, is_default, is_system, option_key, option_label, price, sort_order, category_id)
                    VALUES (?, 0, false, ?, 0, 0, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.formatted(optionsTable))) {
                for (SeedOption option : category.options()) {
                    insertOption.setTimestamp(1, timestamp);
                    insertOption.setTimestamp(2, timestamp);
                    insertOption.setBoolean(3, option.isActive());
                    insertOption.setBoolean(4, option.isDefault());
                    insertOption.setBoolean(5, option.isSystem());
                    insertOption.setString(6, option.optionKey());
                    insertOption.setString(7, option.optionLabel());
                    insertOption.setBigDecimal(8, option.price() != null ? option.price() : BigDecimal.ZERO);
                    insertOption.setInt(9, option.sortOrder());
                    insertOption.setLong(10, categoryId);
                    insertOption.addBatch();
                }
                insertOption.executeBatch();
            }
        }
    }

    private static void seedLibrary(Connection connection, String schemaName, LibrarySeedFile librarySeedFile) throws SQLException {
        if (!hasTable(connection, schemaName, "library_categories") || !hasTable(connection, schemaName, "library_entries")) {
            return;
        }

        String entriesTable = qualified(schemaName, "library_entries");
        try (Statement countStatement = connection.createStatement();
             ResultSet countResult = countStatement.executeQuery("SELECT COUNT(*) FROM " + entriesTable)) {
            if (countResult.next() && countResult.getLong(1) > 0) {
                System.out.println("  Library already seeded for " + schemaName + "; skipping");
                return;
            }
        }

        Long seedUserId = findFirstTenantUserId(connection, schemaName);
        if (seedUserId == null) {
            System.out.println("  Library seed deferred for " + schemaName + ": no tenant user found");
            return;
        }

        Instant now = Instant.now();
        Timestamp timestamp = Timestamp.from(now);
        java.util.Map<Integer, Long> categoryIdBySource = new java.util.HashMap<>();

        java.util.List<LibrarySeedCategory> roots = new ArrayList<>(librarySeedFile.categories().stream()
                .filter(category -> category.parentSourceId() == null)
                .toList());
        roots.sort((left, right) -> Integer.compare(
                libraryRootOrder(left.name()),
                libraryRootOrder(right.name())));

        String categoriesTable = qualified(schemaName, "library_categories");
        for (LibrarySeedCategory category : roots) {
            categoryIdBySource.put(category.sourceId(), insertLibraryCategory(
                    connection, categoriesTable, timestamp, category, null));
        }
        for (LibrarySeedCategory category : librarySeedFile.categories()) {
            if (category.parentSourceId() == null) {
                continue;
            }
            Long parentId = categoryIdBySource.get(category.parentSourceId());
            categoryIdBySource.put(category.sourceId(), insertLibraryCategory(
                    connection, categoriesTable, timestamp, category, parentId));
        }

        java.util.Map<Integer, Long> entryIdBySource = new java.util.HashMap<>();
        try (PreparedStatement insertEntry = connection.prepareStatement("""
                INSERT INTO %s
                (createdat, created_by, deleted_at, is_deleted, updatedat, updated_by, version,
                 category_id, title, content, created_by_id, is_active, sort_order, usage_count)
                VALUES (?, 0, NULL, false, ?, 0, 0, ?, ?, ?, ?, ?, ?, 0)
                RETURNING id
                """.formatted(entriesTable))) {
            for (LibrarySeedEntry entry : librarySeedFile.entries()) {
                Long categoryId = categoryIdBySource.get(entry.categorySourceId());
                if (categoryId == null) {
                    continue;
                }
                insertEntry.setTimestamp(1, timestamp);
                insertEntry.setTimestamp(2, timestamp);
                insertEntry.setLong(3, categoryId);
                insertEntry.setString(4, entry.title());
                insertEntry.setString(5, entry.content());
                insertEntry.setLong(6, seedUserId);
                insertEntry.setBoolean(7, entry.isActive());
                insertEntry.setInt(8, entry.sortOrder());
                try (ResultSet keys = insertEntry.executeQuery()) {
                    if (!keys.next()) {
                        throw new SQLException("Failed to insert library entry " + entry.title());
                    }
                    entryIdBySource.put(entry.sourceId(), keys.getLong(1));
                }
            }
        }

        String connectionsTable = qualified(schemaName, "library_entry_connections");
        try (PreparedStatement insertConnection = connection.prepareStatement("""
                INSERT INTO %s
                (createdat, created_by, deleted_at, is_deleted, updatedat, updated_by, version,
                 from_entry_id, to_entry_id, connection_type, description, strength, is_active, created_by_id)
                VALUES (?, 0, NULL, false, ?, 0, 0, ?, ?, ?, ?, ?, ?, ?)
                """.formatted(connectionsTable))) {
            for (LibrarySeedConnection connectionSeed : librarySeedFile.connections()) {
                Long fromId = entryIdBySource.get(connectionSeed.fromSourceId());
                Long toId = entryIdBySource.get(connectionSeed.toSourceId());
                if (fromId == null || toId == null) {
                    continue;
                }
                insertConnection.setTimestamp(1, timestamp);
                insertConnection.setTimestamp(2, timestamp);
                insertConnection.setLong(3, fromId);
                insertConnection.setLong(4, toId);
                insertConnection.setString(5, mapConnectionType(connectionSeed.connectionType()));
                insertConnection.setString(6, connectionSeed.description());
                insertConnection.setInt(7, connectionSeed.strength());
                insertConnection.setBoolean(8, connectionSeed.isActive());
                insertConnection.setLong(9, seedUserId);
                insertConnection.addBatch();
            }
            insertConnection.executeBatch();
        }

        System.out.println("  Seeded library for " + schemaName + ": categories="
                + categoryIdBySource.size() + " entries=" + entryIdBySource.size());
    }

    private static long insertLibraryCategory(
            Connection connection,
            String categoriesTable,
            Timestamp timestamp,
            LibrarySeedCategory category,
            Long parentId
    ) throws SQLException {
        try (PreparedStatement insertCategory = connection.prepareStatement("""
                INSERT INTO %s
                (createdat, created_by, deleted_at, is_deleted, updatedat, updated_by, version,
                 name, description, parent_category_id, sort_order, is_active)
                VALUES (?, 0, NULL, false, ?, 0, 0, ?, ?, ?, ?, ?)
                RETURNING id
                """.formatted(categoriesTable))) {
            insertCategory.setTimestamp(1, timestamp);
            insertCategory.setTimestamp(2, timestamp);
            insertCategory.setString(3, category.name());
            insertCategory.setString(4, category.description());
            if (parentId == null) {
                insertCategory.setNull(5, java.sql.Types.BIGINT);
            } else {
                insertCategory.setLong(5, parentId);
            }
            insertCategory.setInt(6, category.sortOrder());
            insertCategory.setBoolean(7, category.isActive());
            try (ResultSet keys = insertCategory.executeQuery()) {
                if (!keys.next()) {
                    throw new SQLException("Failed to insert library category " + category.name());
                }
                return keys.getLong(1);
            }
        }
    }

    private static Long findFirstTenantUserId(Connection connection, String schemaName) throws SQLException {
        if (!hasTable(connection, schemaName, "users")) {
            return null;
        }
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT id FROM " + qualified(schemaName, "users") + " WHERE is_deleted = false ORDER BY id LIMIT 1")) {
            if (!resultSet.next()) {
                return null;
            }
            return resultSet.getLong(1);
        }
    }

    private static int libraryRootOrder(String name) {
        int index = LIBRARY_ROOT_INSERT_ORDER.indexOf(name);
        return index >= 0 ? index : Integer.MAX_VALUE;
    }

    private static String mapConnectionType(String value) {
        if (value == null || value.isBlank() || "relates_to".equalsIgnoreCase(value)) {
            return "RELATED";
        }
        return value.trim().toUpperCase();
    }

    private static void updateSchemaVersion(
            Connection connection,
            long organisationId,
            String schemaName,
            String url,
            String user,
            String password
    ) throws SQLException {
        Flyway flyway = Flyway.configure()
                .dataSource(url, user, password)
                .schemas(schemaName)
                .defaultSchema(schemaName)
                .table("tenant_flyway_schema_history")
                .locations("classpath:db/tenant_migration")
                .validateOnMigrate(false)
                .ignoreMigrationPatterns("*:missing")
                .load();
        String version = flyway.info().current() != null ? flyway.info().current().getVersion().getVersion() : "0";
        Instant now = Instant.now();
        Timestamp timestamp = Timestamp.from(now);

        try (PreparedStatement update = connection.prepareStatement("""
                UPDATE public.tenant_schema_versions
                SET version = ?, status = 'SUCCESS', error_message = NULL, migrated_at = ?
                WHERE organisation_id = ?
                """)) {
            update.setString(1, version);
            update.setTimestamp(2, timestamp);
            update.setLong(3, organisationId);
            int updated = update.executeUpdate();
            if (updated == 0) {
                try (PreparedStatement insert = connection.prepareStatement("""
                        INSERT INTO public.tenant_schema_versions
                        (organisation_id, version, status, error_message, migrated_at)
                        VALUES (?, ?, 'SUCCESS', NULL, ?)
                        """)) {
                    insert.setLong(1, organisationId);
                    insert.setString(2, version);
                    insert.setTimestamp(3, timestamp);
                    insert.executeUpdate();
                }
            }
        }
    }

    private static String loadSchemaName(Connection connection, long organisationId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT schema_name FROM public.organisations WHERE id = ?")) {
            statement.setLong(1, organisationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Organisation not found: " + organisationId);
                }
                String schemaName = resultSet.getString("schema_name");
                if (schemaName == null || schemaName.isBlank()) {
                    throw new IllegalStateException("Organisation has no schema: " + organisationId);
                }
                return schemaName;
            }
        }
    }

    private static void seedBillingServices(Connection connection, String schemaName) throws SQLException {
        if (!hasTable(connection, schemaName, "services")) {
            return;
        }
        String servicesTable = qualified(schemaName, "services");
        try (Statement countStatement = connection.createStatement();
             ResultSet countResult = countStatement.executeQuery("SELECT COUNT(*) FROM " + servicesTable)) {
            if (countResult.next() && countResult.getLong(1) > 0) {
                System.out.println("  Billing services already seeded for " + schemaName + "; skipping");
                return;
            }
        }
        Instant now = Instant.now();
        Timestamp timestamp = Timestamp.from(now);
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO %s
                (service_code, service_name, description, duration, base_rate, category,
                 is_active, therapist_visible, client_portal_visible,
                 createdat, updatedat, created_by, updated_by, version, is_deleted)
                VALUES (?, ?, ?, ?, ?, ?, true, true, ?, ?, ?, 0, 0, 0, false)
                """.formatted(servicesTable))) {
            insertService(insert, timestamp, "90834", "Individual Psychotherapy 45 min",
                    "Individual psychotherapy, 45 minutes", 45, new BigDecimal("150.00"), "Psychotherapy", true);
            insertService(insert, timestamp, "90837", "Individual Psychotherapy 60 min",
                    "Individual psychotherapy, 60 minutes", 60, new BigDecimal("200.00"), "Psychotherapy", true);
            insertService(insert, timestamp, "90847", "Family Psychotherapy",
                    "Family psychotherapy with patient present", 50, new BigDecimal("175.00"), "Psychotherapy", false);
            insert.executeBatch();
        }
        System.out.println("  Seeded default billing services for " + schemaName);
    }

    private static void insertService(PreparedStatement insert,
                                      Timestamp timestamp,
                                      String code,
                                      String name,
                                      String description,
                                      int duration,
                                      BigDecimal rate,
                                      String category,
                                      boolean clientPortalVisible) throws SQLException {
        insert.setString(1, code);
        insert.setString(2, name);
        insert.setString(3, description);
        insert.setInt(4, duration);
        insert.setBigDecimal(5, rate);
        insert.setString(6, category);
        insert.setBoolean(7, clientPortalVisible);
        insert.setTimestamp(8, timestamp);
        insert.setTimestamp(9, timestamp);
        insert.addBatch();
    }

    private static List<TenantRef> loadTenants(String url, String user, String password) throws SQLException {
        List<TenantRef> tenants = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT id, schema_name
                     FROM public.organisations
                     WHERE schema_name IS NOT NULL
                       AND BTRIM(schema_name) <> ''
                       AND LOWER(schema_name) <> 'public'
                     ORDER BY id
                     """)) {
            while (resultSet.next()) {
                tenants.add(new TenantRef(resultSet.getLong("id"), resultSet.getString("schema_name")));
            }
        }
        return tenants;
    }

    private static boolean hasTable(Connection connection, String schemaName, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1
                FROM information_schema.tables
                WHERE table_schema = ? AND table_name = ?
                """)) {
            statement.setString(1, schemaName);
            statement.setString(2, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static SeedFile loadSeedFile() throws Exception {
        try (InputStream input = TenantMigrationTrigger.class.getClassLoader().getResourceAsStream(SEED_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing seed resource: " + SEED_RESOURCE);
            }
            SeedFile seedFile = OBJECT_MAPPER.readValue(input, SeedFile.class);
            if (seedFile.categories() == null || seedFile.categories().isEmpty()) {
                throw new IllegalStateException("Seed resource is empty: " + SEED_RESOURCE);
            }
            return seedFile;
        }
    }

    private static LibrarySeedFile loadLibrarySeedFile() throws Exception {
        try (InputStream input = TenantMigrationTrigger.class.getClassLoader().getResourceAsStream(LIBRARY_SEED_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing seed resource: " + LIBRARY_SEED_RESOURCE);
            }
            LibrarySeedFile seedFile = OBJECT_MAPPER.readValue(input, LibrarySeedFile.class);
            if (seedFile.categories() == null || seedFile.categories().isEmpty()) {
                throw new IllegalStateException("Library seed resource is empty: " + LIBRARY_SEED_RESOURCE);
            }
            return seedFile;
        }
    }

    private static String qualified(String schemaName, String tableName) {
        return schemaName + "." + tableName;
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private record TenantRef(long organisationId, String schemaName) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SeedFile(List<SeedCategory> categories) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SeedCategory(
            String categoryKey,
            String categoryName,
            String description,
            boolean isSystem,
            boolean isActive,
            List<SeedOption> options
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SeedOption(
            String optionKey,
            String optionLabel,
            int sortOrder,
            boolean isDefault,
            boolean isSystem,
            boolean isActive,
            BigDecimal price
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LibrarySeedFile(
            List<LibrarySeedCategory> categories,
            List<LibrarySeedEntry> entries,
            List<LibrarySeedConnection> connections
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LibrarySeedCategory(
            int sourceId,
            String name,
            String description,
            Integer parentSourceId,
            int sortOrder,
            boolean isActive
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LibrarySeedEntry(
            int sourceId,
            int categorySourceId,
            String title,
            String content,
            int sortOrder,
            int usageCount,
            boolean isActive
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LibrarySeedConnection(
            int fromSourceId,
            int toSourceId,
            String connectionType,
            int strength,
            String description,
            boolean isActive
    ) {
    }
}

package com.smart.therapy.flow.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.assertThat;

class MigrationGuardrailTest {
    private static final Map<String, String> SCOPES = Map.of(
            "migration", "-- PLATFORM", "tenant_migration", "-- TENANT");
    private static final Pattern VERSION = Pattern.compile("V(\\d+)__.+\\.sql");
    @TempDir Path temporaryRoot;

    @Test
    void migrationHistoryIsImmutableAndNewFilesDeclareScope() throws IOException {
        Map<String, String> baseline = new LinkedHashMap<>();
        for (String line : Files.readAllLines(Path.of("src/test/resources/migration-baseline.sha256"))) {
            if (line.isBlank() || line.startsWith("#")) continue;
            String[] fields = line.split("  ", 2);
            assertThat(fields).hasSize(2);
            assertThat(fields[0]).matches("[0-9a-f]{64}");
            assertThat(fields[1]).matches("(migration|tenant_migration)/V[0-9]+__[^/]+\\.sql");
            assertThat(baseline.put(fields[1], fields[0])).as("unique baseline path").isNull();
        }
        assertThat(baseline).isNotEmpty();
        assertThat(violations(Path.of("src/main/resources/db"), baseline)).isEmpty();
    }

    private static List<String> violations(Path root, Map<String, String> baseline) throws IOException {
        List<String> errors = new ArrayList<>();
        for (var entry : baseline.entrySet()) {
            Path file = root.resolve(entry.getKey());
            if (!Files.isRegularFile(file)) errors.add("Missing historical migration: " + entry.getKey());
            else if (!sha256(file).equals(entry.getValue())) errors.add("Historical migration changed: " + entry.getKey());
        }
        for (var scope : SCOPES.entrySet()) {
            Path folder = root.resolve(scope.getKey());
            if (!Files.isDirectory(folder)) {
                errors.add("Missing migration directory: " + scope.getKey());
                continue;
            }
            BigInteger highestHistoricalVersion = baseline.keySet().stream()
                    .filter(path -> path.startsWith(scope.getKey() + "/"))
                    .map(path -> version(Path.of(path))).max(BigInteger::compareTo).orElse(BigInteger.ZERO);
            Map<BigInteger, Path> versions = new LinkedHashMap<>();
            try (var files = Files.walk(folder)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".sql")).sorted().toList()) {
                    String relative = root.relativize(file).toString();
                    if (!file.getParent().equals(folder) || !VERSION.matcher(file.getFileName().toString()).matches()) {
                        errors.add("Invalid migration location or name: " + relative);
                        continue;
                    }
                    BigInteger currentVersion = version(file);
                    if (versions.put(currentVersion, file) != null) errors.add("Duplicate migration version: " + relative);
                    if (baseline.containsKey(relative)) continue;
                    if (currentVersion.compareTo(highestHistoricalVersion) <= 0)
                        errors.add("New migration must follow historical versions: " + relative);
                    String first = Files.readAllLines(file).stream().map(String::trim)
                            .filter(line -> !line.isEmpty()).findFirst().orElse("");
                    if (!scope.getValue().equals(first)) errors.add("Migration must start with " + scope.getValue() + ": " + relative);
                }
            }
        }
        return errors;
    }

    private static BigInteger version(Path file) {
        var matcher = VERSION.matcher(file.getFileName().toString());
        if (!matcher.matches()) throw new IllegalArgumentException("Invalid migration name: " + file);
        return new BigInteger(matcher.group(1));
    }

    private static String sha256(Path file) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private Map<String, String> historicalFixture() throws IOException {
        Files.createDirectories(temporaryRoot.resolve("migration"));
        Files.createDirectories(temporaryRoot.resolve("tenant_migration"));
        Path old = temporaryRoot.resolve("migration/V2__historical.sql");
        Files.writeString(old, "SELECT 1;\n");
        return Map.of("migration/V2__historical.sql", sha256(old));
    }

    @Test
    void acceptsUnmodifiedHistoryAndNewTaggedMigrationsInBothScopes() throws IOException {
        var baseline = historicalFixture();
        Files.writeString(temporaryRoot.resolve("migration/V3__new.sql"), "-- PLATFORM\nSELECT 2;\n");
        Files.writeString(temporaryRoot.resolve("tenant_migration/V1__new.sql"), "-- TENANT\nSELECT 2;\n");
        assertThat(violations(temporaryRoot, baseline)).isEmpty();
    }

    @Test
    void rejectsEvenCommentOnlyEditsToHistory() throws IOException {
        var baseline = historicalFixture();
        Files.writeString(temporaryRoot.resolve("migration/V2__historical.sql"), "-- PLATFORM\nSELECT 1;\n");
        assertThat(violations(temporaryRoot, baseline)).anyMatch(s -> s.contains("Historical migration changed"));
    }

    @Test
    void rejectsDeletedOrRelocatedHistory() throws IOException {
        var baseline = historicalFixture();
        Files.move(temporaryRoot.resolve("migration/V2__historical.sql"), temporaryRoot.resolve("tenant_migration/V2__historical.sql"));
        assertThat(violations(temporaryRoot, baseline)).anyMatch(s -> s.contains("Missing historical migration"));
    }

    @Test
    void rejectsNewUntaggedAndWrongScopeMigrations() throws IOException {
        var baseline = historicalFixture();
        Files.writeString(temporaryRoot.resolve("migration/V3__new.sql"), "SELECT 2;\n");
        Files.writeString(temporaryRoot.resolve("tenant_migration/V1__wrong.sql"), "-- PLATFORM\nSELECT 2;\n");
        assertThat(violations(temporaryRoot, baseline)).hasSize(2).allMatch(s -> s.contains("Migration must start with"));
    }

    @Test
    void rejectsDuplicateAndBackdatedVersions() throws IOException {
        var baseline = historicalFixture();
        Files.writeString(temporaryRoot.resolve("migration/V02__duplicate.sql"), "-- PLATFORM\nSELECT 2;\n");
        Files.writeString(temporaryRoot.resolve("migration/V1__backdated.sql"), "-- PLATFORM\nSELECT 2;\n");
        assertThat(violations(temporaryRoot, baseline)).anyMatch(s -> s.contains("Duplicate migration version"))
                .anyMatch(s -> s.contains("New migration must follow historical versions"));
    }

    @Test
    void rejectsMissingDirectoriesAndNestedSql() throws IOException {
        var baseline = historicalFixture();
        Files.delete(temporaryRoot.resolve("tenant_migration"));
        Files.createDirectories(temporaryRoot.resolve("migration/nested"));
        Files.writeString(temporaryRoot.resolve("migration/nested/V3__new.sql"), "-- PLATFORM\nSELECT 2;\n");
        assertThat(violations(temporaryRoot, baseline)).anyMatch(s -> s.contains("Missing migration directory"))
                .anyMatch(s -> s.contains("Invalid migration location"));
    }
}

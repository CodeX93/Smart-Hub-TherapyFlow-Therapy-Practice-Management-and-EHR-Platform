package com.smart.therapy.flow.migration.clienthub;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

public final class ClientHubIdentifier {

    private static final Pattern SAFE_POSTGRES_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private ClientHubIdentifier() {
    }

    public static String quote(String identifier) {
        if (identifier == null || !SAFE_POSTGRES_IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException("Unsafe PostgreSQL identifier");
        }
        return "\"" + identifier + "\"";
    }

    public static String qualified(String schemaName, String tableName) {
        return quote(schemaName) + "." + quote(tableName);
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}

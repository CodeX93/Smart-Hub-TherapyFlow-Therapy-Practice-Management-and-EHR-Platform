package com.smart.therapy.flow.migration.clienthub;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

import java.sql.Array;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.jdbc.core.RowCallbackHandler;

@Service
@Slf4j
public class ClientHubSourceInventoryService {

    SourceInventory inspectSource(ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                SourceInventory inventory = inspectSource(connection, properties.getSourceSchema());
                connection.rollback();
                return inventory;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    SourceStaffAuthInventory inspectStaffAuthSource(ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String usersTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "users");
                SourceStaffAuthInventory inventory = new SourceStaffAuthInventory(
                        count(connection, "SELECT COUNT(*) FROM " + usersTable),
                        groupedCounts(connection, "SELECT LOWER(COALESCE(role, '')) AS key, COUNT(*) FROM "
                                + usersTable + " GROUP BY LOWER(COALESCE(role, '')) ORDER BY key"),
                        groupedCounts(connection, "SELECT LOWER(COALESCE(status, '')) AS key, COUNT(*) FROM "
                                + usersTable + " GROUP BY LOWER(COALESCE(status, '')) ORDER BY key"),
                        count(connection, "SELECT COUNT(*) FROM " + usersTable
                                + " WHERE password IS NOT NULL AND password ~ '^\\$2[aby]\\$'"),
                        count(connection, "SELECT COUNT(*) FROM " + usersTable
                                + " WHERE password IS NULL OR password !~ '^\\$2[aby]\\$'"),
                        count(connection, "SELECT COUNT(*) FROM " + usersTable
                                + " WHERE email IS NULL OR BTRIM(email) = ''"),
                        count(connection, "SELECT COUNT(*) FROM " + usersTable
                                + " WHERE username IS NULL OR BTRIM(username) = ''"),
                        countDuplicateGroups(connection, usersTable, "email"),
                        countDuplicateGroups(connection, usersTable,
                                ClientHubStaffAuthOverrides.usernameSqlExpression()));
                connection.rollback();
                return inventory;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceStaffUserRef> loadStaffUserRefs(ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String usersTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "users");
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT id, " + ClientHubStaffAuthOverrides.usernameSqlExpression()
                                + " AS username, email, full_name, phone, role, status, password, email_verified, "
                                + "CASE WHEN password IS NOT NULL AND password ~ '^\\$2[aby]\\$' THEN true ELSE false END AS password_compatible "
                                + "FROM " + usersTable + " ORDER BY id")) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceStaffUserRef> users = new ArrayList<>();
                        while (rows.next()) {
                            users.add(new SourceStaffUserRef(
                                    String.valueOf(rows.getLong("id")),
                                    rows.getString("username"),
                                    rows.getString("email"),
                                    normalise(rows.getString("username")),
                                    normalise(rows.getString("email")),
                                    rows.getString("full_name"),
                                    rows.getString("phone"),
                                    normalise(rows.getString("role")),
                                    normalise(rows.getString("status")),
                                    rows.getString("password"),
                                    rows.getBoolean("email_verified"),
                                    rows.getBoolean("password_compatible")));
                        }
                        connection.rollback();
                        return users;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    SourceClientInventory inspectClientSource(ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String clientsTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "clients");
                SourceClientInventory inventory = new SourceClientInventory(
                        count(connection, "SELECT COUNT(*) FROM " + clientsTable),
                        countWhere(connection, clientsTable, "client_id IS NULL OR BTRIM(client_id) = ''"),
                        countWhere(connection, clientsTable, "full_name IS NULL OR BTRIM(full_name) = ''"),
                        countDuplicateGroups(connection, clientsTable, "client_id"),
                        countWhere(connection, clientsTable, "assigned_therapist_id IS NOT NULL"),
                        countWhere(connection, clientsTable, "email IS NOT NULL AND BTRIM(email) <> ''"),
                        countWhere(connection, clientsTable, "phone IS NOT NULL AND BTRIM(phone) <> ''"),
                        countWhere(connection, clientsTable, "emergency_contact_name IS NOT NULL AND BTRIM(emergency_contact_name) <> ''"),
                        countWhere(connection, clientsTable,
                                "street_address_1 IS NOT NULL OR address IS NOT NULL OR city IS NOT NULL"),
                        countWhere(connection, clientsTable,
                                "insurance_provider IS NOT NULL AND BTRIM(insurance_provider) <> '' "
                                        + "AND policy_number IS NOT NULL AND BTRIM(policy_number) <> ''"),
                        countWhere(connection, clientsTable,
                                "(insurance_provider IS NOT NULL AND BTRIM(insurance_provider) <> '' "
                                        + "AND (policy_number IS NULL OR BTRIM(policy_number) = '')) "
                                        + "OR (policy_number IS NOT NULL AND BTRIM(policy_number) <> '' "
                                        + "AND (insurance_provider IS NULL OR BTRIM(insurance_provider) = ''))"),
                        countWhere(connection, clientsTable,
                                "referrer_name IS NOT NULL OR referral_source IS NOT NULL OR referring_person IS NOT NULL"),
                        countWhere(connection, clientsTable,
                                "employment_status IS NOT NULL OR education_level IS NOT NULL OR dependents IS NOT NULL"));
                connection.rollback();
                return inventory;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceClientRef> loadClientRefs(ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String clientsTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "clients");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, client_id, full_name, assigned_therapist_id
                        FROM %s
                        ORDER BY id
                        """.formatted(clientsTable))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceClientRef> clients = new ArrayList<>();
                        while (rows.next()) {
                            Object assignedTherapistId = rows.getObject("assigned_therapist_id");
                            clients.add(new SourceClientRef(
                                    String.valueOf(rows.getLong("id")),
                                    rows.getString("client_id"),
                                    rows.getString("full_name"),
                                    assignedTherapistId == null ? null : String.valueOf(((Number) assignedTherapistId).longValue())));
                        }
                        connection.rollback();
                        return clients;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    SourceSessionInventory inspectSessionSource(ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String sessionsTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "sessions");
                String notesTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "session_notes");
                SourceSessionInventory inventory = new SourceSessionInventory(
                        count(connection, "SELECT COUNT(*) FROM " + sessionsTable),
                        countWhere(connection, sessionsTable, "client_id IS NULL"),
                        countWhere(connection, sessionsTable, "therapist_id IS NULL"),
                        countWhere(connection, sessionsTable, "service_id IS NULL"),
                        countWhere(connection, sessionsTable, "session_date IS NULL"),
                        countWhere(connection, sessionsTable, "session_type IS NULL OR BTRIM(session_type) = ''"),
                        count(connection, "SELECT COUNT(*) FROM " + notesTable),
                        countWhere(connection, notesTable, "session_id IS NULL"),
                        countWhere(connection, notesTable, "client_id IS NULL"),
                        countWhere(connection, notesTable, "therapist_id IS NULL"),
                        countWhere(connection, notesTable, "date IS NULL"));
                connection.rollback();
                return inventory;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    SourceBillingInventory inspectBillingSource(ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String billingTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "session_billing");
                String transactionTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "payment_transactions");
                SourceBillingInventory inventory = new SourceBillingInventory(
                        count(connection, "SELECT COUNT(*) FROM " + billingTable),
                        countWhere(connection, billingTable, "session_id IS NULL"),
                        countWhere(connection, billingTable, "service_code IS NULL OR BTRIM(service_code) = ''"),
                        countWhere(connection, billingTable, "rate_per_unit IS NULL"),
                        countWhere(connection, billingTable, "total_amount IS NULL"),
                        count(connection, "SELECT COUNT(*) FROM " + transactionTable),
                        countWhere(connection, transactionTable, "session_billing_id IS NULL"),
                        countWhere(connection, transactionTable, "source IS NULL OR BTRIM(source) = ''"),
                        countWhere(connection, transactionTable, "amount IS NULL"));
                connection.rollback();
                return inventory;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    SourceDocumentInventory inspectDocumentSource(ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String documentsTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "documents");
                SourceDocumentInventory inventory = new SourceDocumentInventory(
                        count(connection, "SELECT COUNT(*) FROM " + documentsTable),
                        countWhere(connection, documentsTable, "client_id IS NULL"),
                        countWhere(connection, documentsTable, "file_name IS NULL OR BTRIM(file_name) = ''"),
                        countWhere(connection, documentsTable, "original_name IS NULL OR BTRIM(original_name) = ''"),
                        countWhere(connection, documentsTable, "file_size IS NULL"),
                        countWhere(connection, documentsTable, "mime_type IS NULL OR BTRIM(mime_type) = ''"),
                        countWhere(connection, documentsTable, "category IS NULL OR BTRIM(category) = ''"),
                        countWhere(connection, documentsTable, "uploaded_by_id IS NOT NULL"),
                        countWhere(connection, documentsTable, "reviewed_by_id IS NOT NULL"),
                        countWhere(connection, documentsTable, "is_shared_in_portal = true"));
                connection.rollback();
                return inventory;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    SourceRoomIntegrationInventory inspectRoomIntegrationSource(ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String roomsTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "rooms");
                String sessionsTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "sessions");
                SourceRoomIntegrationInventory inventory = new SourceRoomIntegrationInventory(
                        count(connection, "SELECT COUNT(*) FROM " + roomsTable),
                        countWhere(connection, roomsTable, "room_number IS NULL OR BTRIM(room_number) = ''"),
                        countWhere(connection, roomsTable, "room_name IS NULL OR BTRIM(room_name) = ''"),
                        countDuplicateGroups(connection, roomsTable, "room_number"),
                        countWhere(connection, sessionsTable, "room_id IS NOT NULL"),
                        countWhere(connection, sessionsTable, "zoom_enabled = true"),
                        countWhere(connection, sessionsTable,
                                "zoom_enabled = true AND (zoom_meeting_id IS NULL OR BTRIM(zoom_meeting_id) = '')"));
                connection.rollback();
                return inventory;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceRoomRecord> loadRoomRecords(ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String roomsTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "rooms");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, room_number, room_name, capacity, equipment, is_active
                        FROM %s
                        ORDER BY id
                        """.formatted(roomsTable))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceRoomRecord> rooms = new ArrayList<>();
                        while (rows.next()) {
                            rooms.add(new SourceRoomRecord(
                                    String.valueOf(rows.getLong("id")),
                                    rows.getString("room_number"),
                                    rows.getString("room_name"),
                                    nullableInteger(rows, "capacity"),
                                    rows.getString("equipment"),
                                    rows.getBoolean("is_active")));
                        }
                        connection.rollback();
                        return rooms;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceSessionIntegrationRecord> loadSessionIntegrationRecords(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String sessionsTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "sessions");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, zoom_meeting_id, zoom_join_url, zoom_password
                        FROM %s
                        WHERE zoom_enabled = true
                        ORDER BY id
                        """.formatted(sessionsTable))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceSessionIntegrationRecord> integrations = new ArrayList<>();
                        while (rows.next()) {
                            integrations.add(new SourceSessionIntegrationRecord(
                                    String.valueOf(rows.getLong("id")),
                                    rows.getString("zoom_meeting_id"),
                                    rows.getString("zoom_join_url"),
                                    rows.getString("zoom_password")));
                        }
                        connection.rollback();
                        return integrations;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceDocumentRef> loadDocumentRefs(ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String documentsTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "documents");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, client_id, uploaded_by_id, reviewed_by_id, file_name,
                               original_name, file_size, mime_type, category
                        FROM %s
                        ORDER BY id
                        """.formatted(documentsTable))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceDocumentRef> documents = new ArrayList<>();
                        while (rows.next()) {
                            documents.add(new SourceDocumentRef(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "client_id"),
                                    nullableLongString(rows, "uploaded_by_id"),
                                    nullableLongString(rows, "reviewed_by_id"),
                                    rows.getString("file_name"),
                                    rows.getString("original_name"),
                                    nullableInteger(rows, "file_size"),
                                    rows.getString("mime_type"),
                                    rows.getString("category")));
                        }
                        connection.rollback();
                        return documents;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceDocumentRecord> loadDocumentRecords(ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String documentsTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "documents");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, client_id, uploaded_by_id, file_name, original_name, file_size,
                               mime_type, category, is_shared_in_portal, download_count,
                               requires_therapist_review, requires_supervisor_review, review_status,
                               reviewed_by_id, reviewed_at, review_notes, created_at
                        FROM %s
                        ORDER BY id
                        """.formatted(documentsTable))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceDocumentRecord> documents = new ArrayList<>();
                        while (rows.next()) {
                            documents.add(new SourceDocumentRecord(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "client_id"),
                                    nullableLongString(rows, "uploaded_by_id"),
                                    rows.getString("file_name"),
                                    rows.getString("original_name"),
                                    nullableInteger(rows, "file_size"),
                                    rows.getString("mime_type"),
                                    rows.getString("category"),
                                    rows.getBoolean("is_shared_in_portal"),
                                    nullableInteger(rows, "download_count"),
                                    rows.getBoolean("requires_therapist_review"),
                                    rows.getBoolean("requires_supervisor_review"),
                                    rows.getString("review_status"),
                                    nullableLongString(rows, "reviewed_by_id"),
                                    instant(rows, "reviewed_at", properties),
                                    rows.getString("review_notes"),
                                    instant(rows, "created_at", properties)));
                        }
                        connection.rollback();
                        return documents;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceBillingRef> loadBillingRefs(ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String billingTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "session_billing");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, session_id, service_code, rate_per_unit, total_amount
                        FROM %s
                        ORDER BY id
                        """.formatted(billingTable))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceBillingRef> billings = new ArrayList<>();
                        while (rows.next()) {
                            billings.add(new SourceBillingRef(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "session_id"),
                                    rows.getString("service_code"),
                                    rows.getBigDecimal("rate_per_unit"),
                                    rows.getBigDecimal("total_amount")));
                        }
                        connection.rollback();
                        return billings;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourcePaymentTransactionRef> loadPaymentTransactionRefs(ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String transactionTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "payment_transactions");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, session_billing_id, source, amount
                        FROM %s
                        ORDER BY id
                        """.formatted(transactionTable))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourcePaymentTransactionRef> payments = new ArrayList<>();
                        while (rows.next()) {
                            payments.add(new SourcePaymentTransactionRef(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "session_billing_id"),
                                    rows.getString("source"),
                                    rows.getBigDecimal("amount")));
                        }
                        connection.rollback();
                        return payments;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceBillingRecord> loadBillingRecords(ClientHubMigrationProperties properties) throws SQLException {
        return loadBillingRecords(properties, sourceZone(properties));
    }

    List<SourceBillingRecord> loadBillingRecords(
            ClientHubMigrationProperties properties,
            ZoneId billingDateZone) throws SQLException {
        ZoneId zone = billingDateZone == null ? sourceZone(properties) : billingDateZone;
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String billingTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "session_billing");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, session_id, service_code, units, rate_per_unit, total_amount,
                               insurance_covered, copay_amount, billing_date, payment_status,
                               discount_type, discount_value, discount_amount, payment_amount,
                               client_paid_amount, insurance_paid_amount, payment_date,
                               payment_reference, payment_method, payment_notes
                        FROM %s
                        ORDER BY id
                        """.formatted(billingTable))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceBillingRecord> billings = new ArrayList<>();
                        while (rows.next()) {
                            billings.add(new SourceBillingRecord(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "session_id"),
                                    rows.getString("service_code"),
                                    nullableInteger(rows, "units"),
                                    rows.getBigDecimal("rate_per_unit"),
                                    rows.getBigDecimal("total_amount"),
                                    rows.getBoolean("insurance_covered"),
                                    rows.getBigDecimal("copay_amount"),
                                    localDateInZone(rows, "billing_date", zone),
                                    rows.getString("payment_status"),
                                    rows.getString("discount_type"),
                                    rows.getBigDecimal("discount_value"),
                                    rows.getBigDecimal("discount_amount"),
                                    rows.getBigDecimal("payment_amount"),
                                    rows.getBigDecimal("client_paid_amount"),
                                    rows.getBigDecimal("insurance_paid_amount"),
                                    localDate(rows, "payment_date"),
                                    rows.getString("payment_reference"),
                                    rows.getString("payment_method"),
                                    rows.getString("payment_notes")));
                        }
                        connection.rollback();
                        return billings;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourcePaymentTransactionRecord> loadPaymentTransactionRecords(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String transactionTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "payment_transactions");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, session_billing_id, source, amount, payment_method,
                               reference_number, notes, payment_date, recorded_at
                        FROM %s
                        ORDER BY id
                        """.formatted(transactionTable))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourcePaymentTransactionRecord> payments = new ArrayList<>();
                        while (rows.next()) {
                            payments.add(new SourcePaymentTransactionRecord(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "session_billing_id"),
                                    rows.getString("source"),
                                    rows.getBigDecimal("amount"),
                                    rows.getString("payment_method"),
                                    rows.getString("reference_number"),
                                    rows.getString("notes"),
                                    localDate(rows, "payment_date"),
                                    instant(rows, "recorded_at", properties)));
                        }
                        connection.rollback();
                        return payments;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    SourceServiceInventory inspectServiceSource(ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String servicesTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "services");
                SourceServiceInventory inventory = new SourceServiceInventory(
                        count(connection, "SELECT COUNT(*) FROM " + servicesTable),
                        countWhere(connection, servicesTable, "service_code IS NULL OR BTRIM(service_code) = ''"),
                        countWhere(connection, servicesTable, "service_name IS NULL OR BTRIM(service_name) = ''"),
                        countWhere(connection, servicesTable, "duration IS NULL"),
                        countWhere(connection, servicesTable, "base_rate IS NULL"),
                        countDuplicateGroups(connection, servicesTable, "BTRIM(service_code)"));
                connection.rollback();
                return inventory;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceServiceRecord> loadServiceRecords(ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String servicesTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "services");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, service_code, service_name, description, duration, base_rate,
                               category, is_active, therapist_visible, client_portal_visible
                        FROM %s
                        ORDER BY id
                        """.formatted(servicesTable))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceServiceRecord> services = new ArrayList<>();
                        while (rows.next()) {
                            services.add(new SourceServiceRecord(
                                    String.valueOf(rows.getLong("id")),
                                    rows.getString("service_code"),
                                    rows.getString("service_name"),
                                    rows.getString("description"),
                                    nullableInteger(rows, "duration"),
                                    rows.getBigDecimal("base_rate"),
                                    rows.getString("category"),
                                    rows.getBoolean("is_active"),
                                    rows.getBoolean("therapist_visible"),
                                    rows.getBoolean("client_portal_visible")));
                        }
                        connection.rollback();
                        return services;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceSessionRef> loadSessionRefs(ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String sessionsTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "sessions");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, client_id, therapist_id, service_id, session_date, session_type
                        FROM %s
                        ORDER BY id
                        """.formatted(sessionsTable))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceSessionRef> sessions = new ArrayList<>();
                        while (rows.next()) {
                            sessions.add(new SourceSessionRef(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "client_id"),
                                    nullableLongString(rows, "therapist_id"),
                                    nullableLongString(rows, "service_id"),
                                    instant(rows, "session_date", properties) != null,
                                    hasText(rows.getString("session_type"))));
                        }
                        connection.rollback();
                        return sessions;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceSessionNoteRef> loadSessionNoteRefs(ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String notesTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "session_notes");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, session_id, client_id, therapist_id, date
                        FROM %s
                        ORDER BY id
                        """.formatted(notesTable))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceSessionNoteRef> notes = new ArrayList<>();
                        while (rows.next()) {
                            notes.add(new SourceSessionNoteRef(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "session_id"),
                                    nullableLongString(rows, "client_id"),
                                    nullableLongString(rows, "therapist_id"),
                                    instant(rows, "date", properties) != null));
                        }
                        connection.rollback();
                        return notes;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceSessionRecord> loadSessionRecords(ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String sessionsTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "sessions");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, client_id, therapist_id, service_id, room_id, session_date, session_type, status,
                               duration, notes, calculated_rate, insurance_applicable, billing_notes,
                               zoom_enabled, recurrence_group_id
                        FROM %s
                        ORDER BY id
                        """.formatted(sessionsTable))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceSessionRecord> sessions = new ArrayList<>();
                        while (rows.next()) {
                            sessions.add(new SourceSessionRecord(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "client_id"),
                                    nullableLongString(rows, "therapist_id"),
                                    nullableLongString(rows, "service_id"),
                                    nullableLongString(rows, "room_id"),
                                    instant(rows, "session_date", properties),
                                    rows.getString("session_type"),
                                    rows.getString("status"),
                                    nullableInteger(rows, "duration"),
                                    rows.getString("notes"),
                                    rows.getBigDecimal("calculated_rate"),
                                    rows.getBoolean("insurance_applicable"),
                                    rows.getString("billing_notes"),
                                    rows.getBoolean("zoom_enabled"),
                                    rows.getString("recurrence_group_id")));
                        }
                        connection.rollback();
                        return sessions;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceSessionNoteRecord> loadSessionNoteRecords(ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String notesTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "session_notes");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, session_id, client_id, therapist_id, date, session_focus, symptoms,
                               short_term_goals, intervention, progress, remarks, recommendations,
                               client_rating, therapist_rating, progress_toward_goals, mood_before, mood_after,
                               risk_suicidal_ideation, risk_self_harm, risk_homicidal_ideation, risk_psychosis,
                               risk_substance_use, risk_impulsivity, risk_aggression, risk_trauma_symptoms,
                               risk_non_adherence, risk_support_system, generated_content, draft_content,
                               final_content, is_draft, is_finalized, finalized_at, ai_enabled,
                               custom_ai_prompt, ai_processing_status, voice_transcription
                        FROM %s
                        ORDER BY id
                        """.formatted(notesTable))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceSessionNoteRecord> notes = new ArrayList<>();
                        while (rows.next()) {
                            notes.add(new SourceSessionNoteRecord(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "session_id"),
                                    nullableLongString(rows, "client_id"),
                                    nullableLongString(rows, "therapist_id"),
                                    instant(rows, "date", properties),
                                    rows.getString("session_focus"),
                                    rows.getString("symptoms"),
                                    rows.getString("short_term_goals"),
                                    rows.getString("intervention"),
                                    rows.getString("progress"),
                                    rows.getString("remarks"),
                                    rows.getString("recommendations"),
                                    nullableInteger(rows, "client_rating"),
                                    nullableInteger(rows, "therapist_rating"),
                                    nullableInteger(rows, "progress_toward_goals"),
                                    nullableInteger(rows, "mood_before"),
                                    nullableInteger(rows, "mood_after"),
                                    nullableInteger(rows, "risk_suicidal_ideation"),
                                    nullableInteger(rows, "risk_self_harm"),
                                    nullableInteger(rows, "risk_homicidal_ideation"),
                                    nullableInteger(rows, "risk_psychosis"),
                                    nullableInteger(rows, "risk_substance_use"),
                                    nullableInteger(rows, "risk_impulsivity"),
                                    nullableInteger(rows, "risk_aggression"),
                                    nullableInteger(rows, "risk_trauma_symptoms"),
                                    nullableInteger(rows, "risk_non_adherence"),
                                    nullableInteger(rows, "risk_support_system"),
                                    rows.getString("generated_content"),
                                    rows.getString("draft_content"),
                                    rows.getString("final_content"),
                                    rows.getBoolean("is_draft"),
                                    rows.getBoolean("is_finalized"),
                                    instant(rows, "finalized_at", properties),
                                    rows.getBoolean("ai_enabled"),
                                    rows.getString("custom_ai_prompt"),
                                    rows.getString("ai_processing_status"),
                                    rows.getString("voice_transcription")));
                        }
                        connection.rollback();
                        return notes;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceClientRecord> loadClientRecords(ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String clientsTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "clients");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, client_id, full_name, date_of_birth, gender, marital_status,
                               preferred_language, pronouns, NULL AS timezone, start_date, service_type,
                               service_frequency, client_type, status, stage, last_update_date, notes,
                               assigned_therapist_id, email, phone, emergency_contact_name,
                               emergency_contact_phone, emergency_contact_relationship,
                               street_address_1, street_address_2, city, province, postal_code,
                               country, address, state, zip_code, insurance_provider, policy_number,
                               group_number, insurance_phone, copay_amount, deductible,
                               referrer_name, referral_date, reference_number, client_source,
                               referral_source, referral_type, referring_person, referral_notes,
                               employment_status, education_level, dependents
                        FROM %s
                        ORDER BY id
                        """.formatted(clientsTable))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceClientRecord> clients = new ArrayList<>();
                        while (rows.next()) {
                            Object assignedTherapistId = rows.getObject("assigned_therapist_id");
                            clients.add(new SourceClientRecord(
                                    String.valueOf(rows.getLong("id")),
                                    rows.getString("client_id"),
                                    rows.getString("full_name"),
                                    localDate(rows, "date_of_birth"),
                                    rows.getString("gender"),
                                    rows.getString("marital_status"),
                                    rows.getString("preferred_language"),
                                    rows.getString("pronouns"),
                                    rows.getString("timezone"),
                                    localDate(rows, "start_date"),
                                    rows.getString("service_type"),
                                    rows.getString("service_frequency"),
                                    rows.getString("client_type"),
                                    rows.getString("status"),
                                    rows.getString("stage"),
                                    instant(rows, "last_update_date", properties),
                                    rows.getString("notes"),
                                    assignedTherapistId == null ? null : String.valueOf(((Number) assignedTherapistId).longValue()),
                                    rows.getString("email"),
                                    rows.getString("phone"),
                                    rows.getString("emergency_contact_name"),
                                    rows.getString("emergency_contact_phone"),
                                    rows.getString("emergency_contact_relationship"),
                                    rows.getString("street_address_1"),
                                    rows.getString("street_address_2"),
                                    rows.getString("city"),
                                    rows.getString("province"),
                                    rows.getString("postal_code"),
                                    rows.getString("country"),
                                    rows.getString("address"),
                                    rows.getString("state"),
                                    rows.getString("zip_code"),
                                    rows.getString("insurance_provider"),
                                    rows.getString("policy_number"),
                                    rows.getString("group_number"),
                                    rows.getString("insurance_phone"),
                                    rows.getBigDecimal("copay_amount"),
                                    rows.getBigDecimal("deductible"),
                                    rows.getString("referrer_name"),
                                    localDate(rows, "referral_date"),
                                    rows.getString("reference_number"),
                                    rows.getString("client_source"),
                                    rows.getString("referral_source"),
                                    rows.getString("referral_type"),
                                    rows.getString("referring_person"),
                                    rows.getString("referral_notes"),
                                    rows.getString("employment_status"),
                                    rows.getString("education_level"),
                                    nullableInteger(rows, "dependents")));
                        }
                        connection.rollback();
                        return clients;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    TargetInventory inspectTarget(JdbcTemplate jdbcTemplate, ClientHubMigrationProperties properties) {
        List<String> predicates = new ArrayList<>();
        List<Object> args = new ArrayList<>();

        if (hasText(properties.getTargetOrganisationSlug())) {
            predicates.add("slug = ?");
            args.add(properties.getTargetOrganisationSlug());
        }
        if (hasText(properties.getTargetSchemaName())) {
            predicates.add("schema_name = ?");
            args.add(properties.getTargetSchemaName());
        }

        List<Map<String, Object>> matchingRows = jdbcTemplate.queryForList(
                "SELECT id, schema_name, timezone FROM public.organisations WHERE "
                        + String.join(" OR ", predicates),
                args.toArray());

        Integer demoTenants = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM public.organisations
                WHERE slug IN ('northstar-wellness', 'harbor-mental-health')
                  AND schema_name IN ('tenant_northstar', 'tenant_harbor')
                """, Integer.class);

        Long organisationId = null;
        String schemaName = null;
        String timezone = null;
        if (matchingRows.size() == 1) {
            Object id = matchingRows.get(0).get("id");
            organisationId = id instanceof Number number ? number.longValue() : Long.valueOf(id.toString());
            Object schema = matchingRows.get(0).get("schema_name");
            schemaName = schema == null ? null : schema.toString();
            Object tz = matchingRows.get(0).get("timezone");
            timezone = tz == null || tz.toString().isBlank() ? null : tz.toString().trim();
        }

        return new TargetInventory(
                matchingRows.size(),
                demoTenants == null ? 0 : demoTenants,
                organisationId,
                schemaName,
                timezone);
    }

    Set<String> loadPlatformRoleNames(JdbcTemplate jdbcTemplate) {
        return Set.copyOf(jdbcTemplate.query(
                "SELECT name FROM public.roles WHERE is_active = true",
                (rs, rowNum) -> rs.getString("name")));
    }

    StaffAuthTargetState loadStaffAuthTargetState(JdbcTemplate jdbcTemplate, TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException("Exactly one target organisation is required to load staff/auth target state");
        }

        Set<String> mappedLegacyIds = new HashSet<>(jdbcTemplate.query(
                """
                SELECT source_id
                FROM public.clienthub_legacy_id_mappings
                WHERE organisation_id = ?
                  AND source_system = 'ClientHubAI'
                  AND entity_name = 'users'
                """,
                (rs, rowNum) -> rs.getString("source_id"),
                target.organisationId()));

        Set<String> authEmails = new HashSet<>(jdbcTemplate.query(
                """
                SELECT normalised_email
                FROM public.auth_identities
                WHERE organisation_id = ?
                  AND identity_type = 'STAFF'
                  AND normalised_email IS NOT NULL
                """,
                (rs, rowNum) -> rs.getString("normalised_email"),
                target.organisationId()));

        Set<String> authUsernames = new HashSet<>(jdbcTemplate.query(
                """
                SELECT normalised_username
                FROM public.auth_identities
                WHERE organisation_id = ?
                  AND identity_type = 'STAFF'
                  AND normalised_username IS NOT NULL
                """,
                (rs, rowNum) -> rs.getString("normalised_username"),
                target.organisationId()));

        String usersTable = ClientHubIdentifier.qualified(target.schemaName(), "users");
        Set<String> tenantUserEmails = new HashSet<>(jdbcTemplate.query(
                "SELECT LOWER(TRIM(email)) FROM " + usersTable + " WHERE email IS NOT NULL AND TRIM(email) <> ''",
                (rs, rowNum) -> rs.getString(1)));

        return new StaffAuthTargetState(mappedLegacyIds, authEmails, authUsernames, tenantUserEmails);
    }

    ClientTargetState loadClientTargetState(JdbcTemplate jdbcTemplate, TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException("Exactly one target organisation is required to load client target state");
        }

        Set<String> mappedClientIds = loadMappedSourceIds(jdbcTemplate, target.organisationId(), "clients");
        Set<String> mappedUserIds = loadMappedSourceIds(jdbcTemplate, target.organisationId(), "users");
        String clientsTable = ClientHubIdentifier.qualified(target.schemaName(), "clients");
        Integer tenantClientRows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + clientsTable, Integer.class);

        return new ClientTargetState(
                mappedClientIds,
                mappedUserIds,
                tenantClientRows == null ? 0 : tenantClientRows);
    }

    SessionTargetState loadSessionTargetState(JdbcTemplate jdbcTemplate, TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException("Exactly one target organisation is required to load session target state");
        }
        return new SessionTargetState(
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "clients"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "users"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "services"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "sessions"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "session_notes"));
    }

    ServiceTargetState loadServiceTargetState(JdbcTemplate jdbcTemplate, TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException("Exactly one target organisation is required to load service target state");
        }
        String servicesTable = ClientHubIdentifier.qualified(target.schemaName(), "services");
        Set<String> serviceCodes = Set.copyOf(jdbcTemplate.query(
                "SELECT service_code FROM " + servicesTable + " WHERE service_code IS NOT NULL",
                (rs, rowNum) -> rs.getString("service_code")));
        return new ServiceTargetState(
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "services"),
                serviceCodes);
    }

    BillingTargetState loadBillingTargetState(JdbcTemplate jdbcTemplate, TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException("Exactly one target organisation is required to load billing target state");
        }
        return new BillingTargetState(
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "sessions"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "session_billing"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "payment_transactions"));
    }

    DocumentTargetState loadDocumentTargetState(JdbcTemplate jdbcTemplate, TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException("Exactly one target organisation is required to load document target state");
        }
        return new DocumentTargetState(
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "clients"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "users"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "documents"));
    }

    RoomIntegrationTargetState loadRoomIntegrationTargetState(JdbcTemplate jdbcTemplate, TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException("Exactly one target organisation is required to load room/integration target state");
        }
        String roomsTable = ClientHubIdentifier.qualified(target.schemaName(), "rooms");
        Set<String> roomNumbers = Set.copyOf(jdbcTemplate.query(
                "SELECT room_number FROM " + roomsTable + " WHERE room_number IS NOT NULL",
                (rs, rowNum) -> rs.getString("room_number")));
        return new RoomIntegrationTargetState(
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "rooms"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "sessions"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "session_integrations"),
                roomNumbers);
    }

    SourceTherapistScheduleInventory inspectTherapistScheduleSource(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String profilesTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "user_profiles");
                String blockedTable = ClientHubIdentifier.qualified(
                        properties.getSourceSchema(), "therapist_blocked_times");
                SourceTherapistScheduleInventory inventory = new SourceTherapistScheduleInventory(
                        count(connection, "SELECT COUNT(*) FROM " + profilesTable),
                        countWhere(connection, profilesTable, "user_id IS NULL"),
                        countWhere(connection, profilesTable,
                                "working_hours IS NOT NULL AND BTRIM(working_hours) <> ''"),
                        countWhere(connection, profilesTable, "virtual_room_id IS NOT NULL"),
                        countWhere(connection, profilesTable,
                                "available_physical_rooms IS NOT NULL AND cardinality(available_physical_rooms) > 0"),
                        countWhere(connection, profilesTable,
                                "license_number IS NOT NULL AND BTRIM(license_number) <> ''"),
                        countWhere(connection, profilesTable,
                                "specializations IS NOT NULL AND cardinality(specializations) > 0"),
                        countWhere(connection, profilesTable,
                                "emergency_contact_name IS NOT NULL AND BTRIM(emergency_contact_name) <> ''"),
                        count(connection, "SELECT COUNT(*) FROM " + blockedTable),
                        countWhere(connection, blockedTable, "therapist_id IS NULL"),
                        countWhere(connection, blockedTable, "start_time IS NULL"),
                        countWhere(connection, blockedTable, "end_time IS NULL"),
                        countWhere(connection, blockedTable, "block_type IS NULL OR BTRIM(block_type) = ''"));
                connection.rollback();
                return inventory;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceUserProfileScheduleRecord> loadUserProfileScheduleRecords(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String profilesTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "user_profiles");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, user_id,
                               license_number, license_type, license_state, license_expiry, license_status,
                               specializations, treatment_approaches, age_groups, languages,
                               certifications, education, years_of_experience,
                               working_days, working_hours, max_clients_per_day, session_duration,
                               availability_status, virtual_room_id, available_physical_rooms,
                               emergency_contact_name, emergency_contact_phone, emergency_contact_relationship,
                               previous_positions, clinical_experience, research_background, publications,
                               professional_memberships, continuing_education, supervisory_experience,
                               award_recognitions, professional_references, career_objectives
                        FROM %s
                        ORDER BY id
                        """.formatted(profilesTable))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceUserProfileScheduleRecord> profiles = new ArrayList<>();
                        while (rows.next()) {
                            profiles.add(new SourceUserProfileScheduleRecord(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "user_id"),
                                    rows.getString("license_number"),
                                    rows.getString("license_type"),
                                    rows.getString("license_state"),
                                    localDate(rows, "license_expiry"),
                                    rows.getString("license_status"),
                                    textArray(rows, "specializations"),
                                    textArray(rows, "treatment_approaches"),
                                    textArray(rows, "age_groups"),
                                    textArray(rows, "languages"),
                                    textArray(rows, "certifications"),
                                    textArray(rows, "education"),
                                    nullableInteger(rows, "years_of_experience"),
                                    textArray(rows, "working_days"),
                                    rows.getString("working_hours"),
                                    nullableInteger(rows, "max_clients_per_day"),
                                    nullableInteger(rows, "session_duration"),
                                    rows.getString("availability_status"),
                                    nullableLongString(rows, "virtual_room_id"),
                                    integerArrayAsStrings(rows, "available_physical_rooms"),
                                    rows.getString("emergency_contact_name"),
                                    rows.getString("emergency_contact_phone"),
                                    rows.getString("emergency_contact_relationship"),
                                    textArray(rows, "previous_positions"),
                                    rows.getString("clinical_experience"),
                                    rows.getString("research_background"),
                                    textArray(rows, "publications"),
                                    textArray(rows, "professional_memberships"),
                                    textArray(rows, "continuing_education"),
                                    rows.getString("supervisory_experience"),
                                    textArray(rows, "award_recognitions"),
                                    textArray(rows, "professional_references"),
                                    rows.getString("career_objectives")));
                        }
                        connection.rollback();
                        return profiles;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceTherapistBlockedTimeRecord> loadTherapistBlockedTimeRecords(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String blockedTable = ClientHubIdentifier.qualified(
                        properties.getSourceSchema(), "therapist_blocked_times");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, therapist_id, start_time, end_time, all_day, block_type, reason,
                               is_recurring, recurrence_pattern, is_active
                        FROM %s
                        ORDER BY id
                        """.formatted(blockedTable))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceTherapistBlockedTimeRecord> blockedTimes = new ArrayList<>();
                        while (rows.next()) {
                            blockedTimes.add(new SourceTherapistBlockedTimeRecord(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "therapist_id"),
                                    instant(rows, "start_time", properties),
                                    instant(rows, "end_time", properties),
                                    rows.getBoolean("all_day"),
                                    rows.getString("block_type"),
                                    rows.getString("reason"),
                                    rows.getBoolean("is_recurring"),
                                    rows.getString("recurrence_pattern"),
                                    rows.getBoolean("is_active")));
                        }
                        connection.rollback();
                        return blockedTimes;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    TherapistScheduleTargetState loadTherapistScheduleTargetState(JdbcTemplate jdbcTemplate, TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException(
                    "Exactly one target organisation is required to load therapist schedule target state");
        }
        Set<String> mappedUserIds = loadMappedSourceIds(jdbcTemplate, target.organisationId(), "users");
        Set<String> mappedProfileIds = loadMappedSourceIds(jdbcTemplate, target.organisationId(), "user_profiles");
        Set<String> mappedBlockedTimeIds = loadMappedSourceIds(
                jdbcTemplate, target.organisationId(), "therapist_blocked_times");
        Set<String> mappedRoomIds = loadMappedSourceIds(jdbcTemplate, target.organisationId(), "rooms");

        String profilesTable = ClientHubIdentifier.qualified(target.schemaName(), "user_profiles");
        Set<String> existingProfileUserLegacyIds = Set.copyOf(jdbcTemplate.query("""
                SELECT m.source_id
                FROM public.clienthub_legacy_id_mappings m
                INNER JOIN %s p ON p.user_id = m.target_id
                WHERE m.organisation_id = ?
                  AND m.source_system = 'ClientHubAI'
                  AND m.entity_name = 'users'
                """.formatted(profilesTable),
                (rs, rowNum) -> rs.getString("source_id"),
                target.organisationId()));

        return new TherapistScheduleTargetState(
                mappedUserIds,
                mappedProfileIds,
                mappedBlockedTimeIds,
                mappedRoomIds,
                existingProfileUserLegacyIds);
    }

    SourceTaskInventory inspectTaskSource(ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String tasksTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "tasks");
                String commentsTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "task_comments");
                SourceTaskInventory inventory = new SourceTaskInventory(
                        count(connection, "SELECT COUNT(*) FROM " + tasksTable),
                        countWhere(connection, tasksTable, "title IS NULL OR BTRIM(title) = ''"),
                        countWhere(connection, tasksTable, "client_id IS NULL"),
                        count(connection, "SELECT COUNT(*) FROM " + commentsTable),
                        countWhere(connection, commentsTable, "content IS NULL OR BTRIM(content) = ''"));
                connection.rollback();
                return inventory;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceTaskRecord> loadTaskRecords(ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String tasksTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "tasks");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, client_id, assigned_to_id, title, description, status::text AS status,
                               priority::text AS priority, due_date, completed_at, created_at, updated_at
                        FROM %s
                        ORDER BY id
                        """.formatted(tasksTable))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceTaskRecord> tasks = new ArrayList<>();
                        while (rows.next()) {
                            tasks.add(new SourceTaskRecord(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "client_id"),
                                    nullableLongString(rows, "assigned_to_id"),
                                    rows.getString("title"),
                                    rows.getString("description"),
                                    rows.getString("status"),
                                    rows.getString("priority"),
                                    instant(rows, "due_date", properties),
                                    instant(rows, "completed_at", properties),
                                    instant(rows, "created_at", properties),
                                    instant(rows, "updated_at", properties)));
                        }
                        connection.rollback();
                        return tasks;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceTaskCommentRecord> loadTaskCommentRecords(ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String commentsTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "task_comments");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, task_id, author_id, content, is_internal, created_at, updated_at
                        FROM %s
                        ORDER BY id
                        """.formatted(commentsTable))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceTaskCommentRecord> comments = new ArrayList<>();
                        while (rows.next()) {
                            comments.add(new SourceTaskCommentRecord(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "task_id"),
                                    nullableLongString(rows, "author_id"),
                                    rows.getString("content"),
                                    rows.getBoolean("is_internal"),
                                    instant(rows, "created_at", properties),
                                    instant(rows, "updated_at", properties)));
                        }
                        connection.rollback();
                        return comments;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    TaskTargetState loadTaskTargetState(JdbcTemplate jdbcTemplate, TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException("Exactly one target organisation is required to load task target state");
        }
        return new TaskTargetState(
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "clients"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "users"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "tasks"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "task_comments"));
    }

    SourceNotificationInventory inspectNotificationSource(ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String templatesTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "notification_templates");
                String triggersTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "notification_triggers");
                String preferencesTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "notification_preferences");
                String notificationsTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "notifications");
                String scheduledTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "scheduled_notifications");
                String notificationRecency = notificationRecencyPredicate(properties, "created_at");
                String scheduledRecency = notificationRecencyPredicate(properties, "created_at");
                SourceNotificationInventory inventory = new SourceNotificationInventory(
                        count(connection, "SELECT COUNT(*) FROM " + templatesTable),
                        countWhere(connection, templatesTable, "name IS NULL OR BTRIM(name) = ''"),
                        countWhere(connection, templatesTable, "type IS NULL OR BTRIM(type) = ''"),
                        countWhere(connection, templatesTable, "subject IS NULL OR BTRIM(subject) = ''"),
                        countWhere(connection, templatesTable, "body_template IS NULL OR BTRIM(body_template) = ''"),
                        countDuplicateGroups(connection, templatesTable, "BTRIM(name)"),
                        count(connection, "SELECT COUNT(*) FROM " + triggersTable),
                        countWhere(connection, triggersTable, "name IS NULL OR BTRIM(name) = ''"),
                        countWhere(connection, triggersTable, "event_type IS NULL OR BTRIM(event_type) = ''"),
                        countWhere(connection, triggersTable, "entity_type IS NULL OR BTRIM(entity_type) = ''"),
                        count(connection, "SELECT COUNT(*) FROM " + preferencesTable),
                        countWhere(connection, preferencesTable, "user_id IS NULL"),
                        countWhere(connection, preferencesTable, "trigger_type IS NULL OR BTRIM(trigger_type) = ''"),
                        countWhere(connection, preferencesTable,
                                "trigger_type IS NOT NULL AND LOWER(BTRIM(trigger_type)) = '__global__'"),
                        countWhere(connection, notificationsTable, notificationRecency),
                        countWhere(connection, notificationsTable,
                                notificationRecency + " AND (user_id IS NULL)"),
                        countWhere(connection, notificationsTable,
                                notificationRecency + " AND (type IS NULL OR BTRIM(type) = '')"),
                        countWhere(connection, notificationsTable,
                                notificationRecency + " AND (title IS NULL OR BTRIM(title) = '')"),
                        countWhere(connection, notificationsTable,
                                notificationRecency + " AND (message IS NULL OR BTRIM(message) = '')"),
                        countWhere(connection, scheduledTable, scheduledRecency),
                        countWhere(connection, scheduledTable,
                                scheduledRecency + " AND (trigger_id IS NULL)"),
                        countWhere(connection, scheduledTable,
                                scheduledRecency + " AND (entity_type IS NULL OR BTRIM(entity_type) = '')"),
                        countWhere(connection, scheduledTable,
                                scheduledRecency + " AND (entity_id IS NULL)"),
                        countWhere(connection, scheduledTable,
                                scheduledRecency + " AND (execute_at IS NULL)"));
                connection.rollback();
                return inventory;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceNotificationTemplateRecord> loadNotificationTemplateRecords(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String table = ClientHubIdentifier.qualified(properties.getSourceSchema(), "notification_templates");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, name, type, subject, body_template, action_url_template, action_label,
                               recipient_roles, variables, is_system, is_active, created_at, updated_at
                        FROM %s
                        ORDER BY id
                        """.formatted(table))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceNotificationTemplateRecord> templates = new ArrayList<>();
                        while (rows.next()) {
                            templates.add(new SourceNotificationTemplateRecord(
                                    String.valueOf(rows.getLong("id")),
                                    rows.getString("name"),
                                    rows.getString("type"),
                                    rows.getString("subject"),
                                    rows.getString("body_template"),
                                    rows.getString("action_url_template"),
                                    rows.getString("action_label"),
                                    rows.getString("recipient_roles"),
                                    rows.getString("variables"),
                                    rows.getBoolean("is_system"),
                                    rows.getBoolean("is_active"),
                                    instant(rows, "created_at", properties),
                                    instant(rows, "updated_at", properties)));
                        }
                        connection.rollback();
                        return templates;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceNotificationTriggerRecord> loadNotificationTriggerRecords(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String table = ClientHubIdentifier.qualified(properties.getSourceSchema(), "notification_triggers");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, name, description, event_type, entity_type, condition_rules, recipient_rules,
                               template_id, priority, delay_minutes, batch_window_minutes, max_batch_size,
                               is_scheduled, is_active, created_at, updated_at
                        FROM %s
                        ORDER BY id
                        """.formatted(table))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceNotificationTriggerRecord> triggers = new ArrayList<>();
                        while (rows.next()) {
                            triggers.add(new SourceNotificationTriggerRecord(
                                    String.valueOf(rows.getLong("id")),
                                    rows.getString("name"),
                                    rows.getString("description"),
                                    rows.getString("event_type"),
                                    rows.getString("entity_type"),
                                    rows.getString("condition_rules"),
                                    rows.getString("recipient_rules"),
                                    nullableLongString(rows, "template_id"),
                                    rows.getString("priority"),
                                    nullableInteger(rows, "delay_minutes"),
                                    nullableInteger(rows, "batch_window_minutes"),
                                    nullableInteger(rows, "max_batch_size"),
                                    rows.getBoolean("is_scheduled"),
                                    rows.getBoolean("is_active"),
                                    instant(rows, "created_at", properties),
                                    instant(rows, "updated_at", properties)));
                        }
                        connection.rollback();
                        return triggers;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceNotificationPreferenceRecord> loadNotificationPreferenceRecords(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String table = ClientHubIdentifier.qualified(properties.getSourceSchema(), "notification_preferences");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, user_id, trigger_type, delivery_methods, timing, enable_in_app, enable_email,
                               enable_sms, quiet_hours_start, quiet_hours_end, weekends_enabled,
                               created_at, updated_at
                        FROM %s
                        ORDER BY id
                        """.formatted(table))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceNotificationPreferenceRecord> preferences = new ArrayList<>();
                        while (rows.next()) {
                            preferences.add(new SourceNotificationPreferenceRecord(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "user_id"),
                                    rows.getString("trigger_type"),
                                    rows.getString("delivery_methods"),
                                    rows.getString("timing"),
                                    rows.getBoolean("enable_in_app"),
                                    rows.getBoolean("enable_email"),
                                    rows.getBoolean("enable_sms"),
                                    rows.getString("quiet_hours_start"),
                                    rows.getString("quiet_hours_end"),
                                    rows.getBoolean("weekends_enabled"),
                                    instant(rows, "created_at", properties),
                                    instant(rows, "updated_at", properties)));
                        }
                        connection.rollback();
                        return preferences;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceNotificationRecord> loadNotificationRecords(ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String table = ClientHubIdentifier.qualified(properties.getSourceSchema(), "notifications");
                String recency = notificationRecencyPredicate(properties, "created_at");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, user_id, type, title, message, data, priority, is_read, read_at,
                               action_url, action_label, grouping_key, expires_at,
                               related_entity_type, related_entity_id, created_at
                        FROM %s
                        WHERE %s
                        ORDER BY id
                        """.formatted(table, recency))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceNotificationRecord> notifications = new ArrayList<>();
                        while (rows.next()) {
                            notifications.add(new SourceNotificationRecord(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "user_id"),
                                    rows.getString("type"),
                                    rows.getString("title"),
                                    rows.getString("message"),
                                    rows.getString("data"),
                                    rows.getString("priority"),
                                    rows.getBoolean("is_read"),
                                    instant(rows, "read_at", properties),
                                    rows.getString("action_url"),
                                    rows.getString("action_label"),
                                    rows.getString("grouping_key"),
                                    instant(rows, "expires_at", properties),
                                    rows.getString("related_entity_type"),
                                    nullableLongString(rows, "related_entity_id"),
                                    instant(rows, "created_at", properties)));
                        }
                        connection.rollback();
                        return notifications;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceScheduledNotificationRecord> loadScheduledNotificationRecords(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String table = ClientHubIdentifier.qualified(properties.getSourceSchema(), "scheduled_notifications");
                String recency = notificationRecencyPredicate(properties, "created_at");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, trigger_id, session_id, entity_type, entity_id, entity_data,
                               execute_at, status, retry_count, last_error, created_at, processed_at
                        FROM %s
                        WHERE %s
                        ORDER BY id
                        """.formatted(table, recency))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceScheduledNotificationRecord> scheduled = new ArrayList<>();
                        while (rows.next()) {
                            scheduled.add(new SourceScheduledNotificationRecord(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "trigger_id"),
                                    nullableLongString(rows, "session_id"),
                                    rows.getString("entity_type"),
                                    nullableLongString(rows, "entity_id"),
                                    rows.getString("entity_data"),
                                    instant(rows, "execute_at", properties),
                                    rows.getString("status"),
                                    nullableInteger(rows, "retry_count"),
                                    rows.getString("last_error"),
                                    instant(rows, "created_at", properties),
                                    instant(rows, "processed_at", properties)));
                        }
                        connection.rollback();
                        return scheduled;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    NotificationTargetState loadNotificationTargetState(JdbcTemplate jdbcTemplate, TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException("Exactly one target organisation is required to load notification target state");
        }
        String templatesTable = ClientHubIdentifier.qualified(target.schemaName(), "notification_templates");
        String triggersTable = ClientHubIdentifier.qualified(target.schemaName(), "notification_triggers");
        Set<String> templateNames = Set.copyOf(jdbcTemplate.query(
                "SELECT name FROM " + templatesTable + " WHERE name IS NOT NULL",
                (rs, rowNum) -> rs.getString("name")));
        Set<String> triggerNames = Set.copyOf(jdbcTemplate.query(
                "SELECT name FROM " + triggersTable + " WHERE name IS NOT NULL",
                (rs, rowNum) -> rs.getString("name")));
        return new NotificationTargetState(
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "users"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "clients"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "sessions"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "tasks"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "documents"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "notification_templates"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "notification_triggers"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "notification_preferences"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "notifications"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "scheduled_notifications"),
                templateNames,
                triggerNames);
    }

    SourceClinicalExtrasInventory inspectClinicalExtrasSource(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String consentsTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "patient_consents");
                String supervisorsTable = ClientHubIdentifier.qualified(
                        properties.getSourceSchema(), "supervisor_assignments");
                String clientsTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "clients");
                String checklistTemplatesTable = ClientHubIdentifier.qualified(
                        properties.getSourceSchema(), "checklist_templates");
                String checklistItemsTable = ClientHubIdentifier.qualified(
                        properties.getSourceSchema(), "checklist_items");
                String clientChecklistsTable = ClientHubIdentifier.qualified(
                        properties.getSourceSchema(), "client_checklists");
                String clientChecklistItemsTable = ClientHubIdentifier.qualified(
                        properties.getSourceSchema(), "client_checklist_items");
                String usersTable = ClientHubIdentifier.qualified(properties.getSourceSchema(), "users");
                String portalEligible = "(portal_email IS NOT NULL AND BTRIM(portal_email) <> '') "
                        + "OR has_portal_access = true";
                String zoomEligible = "(zoom_account_id IS NOT NULL AND BTRIM(zoom_account_id) <> '') "
                        + "OR (zoom_client_id IS NOT NULL AND BTRIM(zoom_client_id) <> '')";
                SourceClinicalExtrasInventory inventory = new SourceClinicalExtrasInventory(
                        count(connection, "SELECT COUNT(*) FROM " + consentsTable),
                        countWhere(connection, consentsTable, "client_id IS NULL"),
                        countWhere(connection, consentsTable,
                                "consent_type IS NULL OR BTRIM(consent_type) = ''"),
                        count(connection, "SELECT COUNT(*) FROM " + supervisorsTable),
                        countWhere(connection, supervisorsTable, "supervisor_id IS NULL"),
                        countWhere(connection, supervisorsTable, "therapist_id IS NULL"),
                        countWhere(connection, clientsTable, portalEligible),
                        countWhere(connection, clientsTable,
                                portalEligible + " AND (portal_email IS NULL OR BTRIM(portal_email) = '') "
                                        + "AND (email IS NULL OR BTRIM(email) = '')"),
                        count(connection, "SELECT COUNT(*) FROM " + checklistTemplatesTable),
                        countWhere(connection, checklistTemplatesTable, "name IS NULL OR BTRIM(name) = ''"),
                        count(connection, "SELECT COUNT(*) FROM " + checklistItemsTable),
                        countWhere(connection, checklistItemsTable, "template_id IS NULL"),
                        countWhere(connection, checklistItemsTable, "title IS NULL OR BTRIM(title) = ''"),
                        countWhere(connection, checklistItemsTable,
                                "category IS NULL OR BTRIM(category) = ''"),
                        count(connection, "SELECT COUNT(*) FROM " + clientChecklistsTable),
                        countWhere(connection, clientChecklistsTable, "client_id IS NULL"),
                        countWhere(connection, clientChecklistsTable, "template_id IS NULL"),
                        count(connection, "SELECT COUNT(*) FROM " + clientChecklistItemsTable),
                        countWhere(connection, clientChecklistItemsTable, "client_checklist_id IS NULL"),
                        countWhere(connection, clientChecklistItemsTable, "checklist_item_id IS NULL"),
                        countWhere(connection, usersTable, zoomEligible));
                connection.rollback();
                return inventory;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourcePatientConsentRecord> loadPatientConsentRecords(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String table = ClientHubIdentifier.qualified(properties.getSourceSchema(), "patient_consents");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, client_id, consent_type, consent_version, granted, granted_at, withdrawn_at,
                               ip_address, user_agent, notes, created_at, updated_at
                        FROM %s
                        ORDER BY id
                        """.formatted(table))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourcePatientConsentRecord> consents = new ArrayList<>();
                        while (rows.next()) {
                            consents.add(new SourcePatientConsentRecord(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "client_id"),
                                    rows.getString("consent_type"),
                                    rows.getString("consent_version"),
                                    rows.getBoolean("granted"),
                                    instant(rows, "granted_at", properties),
                                    instant(rows, "withdrawn_at", properties),
                                    rows.getString("ip_address"),
                                    rows.getString("user_agent"),
                                    rows.getString("notes"),
                                    instant(rows, "created_at", properties),
                                    instant(rows, "updated_at", properties)));
                        }
                        connection.rollback();
                        return consents;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceSupervisorAssignmentRecord> loadSupervisorAssignmentRecords(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String table = ClientHubIdentifier.qualified(properties.getSourceSchema(), "supervisor_assignments");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, supervisor_id, therapist_id, assigned_date, is_active, notes,
                               required_meeting_frequency, next_meeting_date, last_meeting_date,
                               created_at, updated_at
                        FROM %s
                        ORDER BY id
                        """.formatted(table))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceSupervisorAssignmentRecord> assignments = new ArrayList<>();
                        while (rows.next()) {
                            assignments.add(new SourceSupervisorAssignmentRecord(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "supervisor_id"),
                                    nullableLongString(rows, "therapist_id"),
                                    instant(rows, "assigned_date", properties),
                                    rows.getBoolean("is_active"),
                                    rows.getString("notes"),
                                    rows.getString("required_meeting_frequency"),
                                    instant(rows, "next_meeting_date", properties),
                                    instant(rows, "last_meeting_date", properties),
                                    instant(rows, "created_at", properties),
                                    instant(rows, "updated_at", properties)));
                        }
                        connection.rollback();
                        return assignments;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceClientPortalRecord> loadClientPortalRecords(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String table = ClientHubIdentifier.qualified(properties.getSourceSchema(), "clients");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, full_name, email, has_portal_access, portal_email, portal_password,
                               last_login, activation_token, updated_at
                        FROM %s
                        WHERE (portal_email IS NOT NULL AND BTRIM(portal_email) <> '')
                           OR has_portal_access = true
                        ORDER BY id
                        """.formatted(table))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceClientPortalRecord> portals = new ArrayList<>();
                        while (rows.next()) {
                            String portalPassword = rows.getString("portal_password");
                            portals.add(new SourceClientPortalRecord(
                                    String.valueOf(rows.getLong("id")),
                                    rows.getString("full_name"),
                                    rows.getString("email"),
                                    rows.getBoolean("has_portal_access"),
                                    rows.getString("portal_email"),
                                    portalPassword,
                                    portalPassword != null && portalPassword.matches("^\\$2[aby]\\$.*"),
                                    instant(rows, "last_login", properties),
                                    rows.getString("activation_token"),
                                    instant(rows, "updated_at", properties)));
                        }
                        connection.rollback();
                        return portals;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceChecklistTemplateRecord> loadChecklistTemplateRecords(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String table = ClientHubIdentifier.qualified(properties.getSourceSchema(), "checklist_templates");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, name, description, client_type, is_active, sort_order, created_at, updated_at
                        FROM %s
                        ORDER BY id
                        """.formatted(table))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceChecklistTemplateRecord> templates = new ArrayList<>();
                        while (rows.next()) {
                            templates.add(new SourceChecklistTemplateRecord(
                                    String.valueOf(rows.getLong("id")),
                                    rows.getString("name"),
                                    rows.getString("description"),
                                    rows.getString("client_type"),
                                    rows.getBoolean("is_active"),
                                    nullableInteger(rows, "sort_order"),
                                    instant(rows, "created_at", properties),
                                    instant(rows, "updated_at", properties)));
                        }
                        connection.rollback();
                        return templates;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceChecklistItemRecord> loadChecklistItemRecords(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String table = ClientHubIdentifier.qualified(properties.getSourceSchema(), "checklist_items");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, template_id, title, description, category, is_required,
                               days_from_start, sort_order, created_at
                        FROM %s
                        ORDER BY id
                        """.formatted(table))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceChecklistItemRecord> items = new ArrayList<>();
                        while (rows.next()) {
                            items.add(new SourceChecklistItemRecord(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "template_id"),
                                    rows.getString("title"),
                                    rows.getString("description"),
                                    rows.getString("category"),
                                    rows.getBoolean("is_required"),
                                    nullableInteger(rows, "days_from_start"),
                                    nullableInteger(rows, "sort_order"),
                                    instant(rows, "created_at", properties)));
                        }
                        connection.rollback();
                        return items;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceClientChecklistRecord> loadClientChecklistRecords(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String table = ClientHubIdentifier.qualified(properties.getSourceSchema(), "client_checklists");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, client_id, template_id, is_completed, completed_at, completed_by,
                               notes, due_date, created_at
                        FROM %s
                        ORDER BY id
                        """.formatted(table))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceClientChecklistRecord> checklists = new ArrayList<>();
                        while (rows.next()) {
                            checklists.add(new SourceClientChecklistRecord(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "client_id"),
                                    nullableLongString(rows, "template_id"),
                                    rows.getBoolean("is_completed"),
                                    instant(rows, "completed_at", properties),
                                    nullableLongString(rows, "completed_by"),
                                    rows.getString("notes"),
                                    localDate(rows, "due_date"),
                                    instant(rows, "created_at", properties)));
                        }
                        connection.rollback();
                        return checklists;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceClientChecklistItemRecord> loadClientChecklistItemRecords(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String table = ClientHubIdentifier.qualified(properties.getSourceSchema(), "client_checklist_items");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, client_checklist_id, checklist_item_id, is_completed, completed_at,
                               completed_by, notes, created_at
                        FROM %s
                        ORDER BY id
                        """.formatted(table))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceClientChecklistItemRecord> items = new ArrayList<>();
                        while (rows.next()) {
                            items.add(new SourceClientChecklistItemRecord(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "client_checklist_id"),
                                    nullableLongString(rows, "checklist_item_id"),
                                    rows.getBoolean("is_completed"),
                                    instant(rows, "completed_at", properties),
                                    nullableLongString(rows, "completed_by"),
                                    rows.getString("notes"),
                                    instant(rows, "created_at", properties)));
                        }
                        connection.rollback();
                        return items;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceUserZoomIntegrationRecord> loadUserZoomIntegrationRecords(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String table = ClientHubIdentifier.qualified(properties.getSourceSchema(), "users");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, zoom_account_id, zoom_client_id, zoom_client_secret,
                               zoom_access_token, zoom_token_expiry, updated_at
                        FROM %s
                        WHERE (zoom_account_id IS NOT NULL AND BTRIM(zoom_account_id) <> '')
                           OR (zoom_client_id IS NOT NULL AND BTRIM(zoom_client_id) <> '')
                        ORDER BY id
                        """.formatted(table))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceUserZoomIntegrationRecord> integrations = new ArrayList<>();
                        while (rows.next()) {
                            integrations.add(new SourceUserZoomIntegrationRecord(
                                    String.valueOf(rows.getLong("id")),
                                    rows.getString("zoom_account_id"),
                                    rows.getString("zoom_client_id"),
                                    rows.getString("zoom_client_secret"),
                                    rows.getString("zoom_access_token"),
                                    instant(rows, "zoom_token_expiry", properties),
                                    instant(rows, "updated_at", properties)));
                        }
                        connection.rollback();
                        return integrations;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    ClinicalExtrasTargetState loadClinicalExtrasTargetState(JdbcTemplate jdbcTemplate, TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException(
                    "Exactly one target organisation is required to load clinical extras target state");
        }
        String templatesTable = ClientHubIdentifier.qualified(target.schemaName(), "checklist_templates");
        Set<String> templateNames = Set.copyOf(jdbcTemplate.query(
                "SELECT name FROM " + templatesTable + " WHERE name IS NOT NULL AND is_deleted = false",
                (rs, rowNum) -> rs.getString("name")));
        return new ClinicalExtrasTargetState(
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "clients"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "users"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "patient_consents"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "supervisor_assignments"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "client_portal"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "checklist_templates"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "checklist_items"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "client_checklists"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "client_checklist_items"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "user_integrations_zoom"),
                templateNames);
    }

    SourceAssessmentsInventory inspectAssessmentsSource(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String templatesTable = ClientHubIdentifier.qualified(
                        properties.getSourceSchema(), "assessment_templates");
                String sectionsTable = ClientHubIdentifier.qualified(
                        properties.getSourceSchema(), "assessment_sections");
                String questionsTable = ClientHubIdentifier.qualified(
                        properties.getSourceSchema(), "assessment_questions");
                String optionsTable = ClientHubIdentifier.qualified(
                        properties.getSourceSchema(), "assessment_question_options");
                String assignmentsTable = ClientHubIdentifier.qualified(
                        properties.getSourceSchema(), "assessment_assignments");
                String responsesTable = ClientHubIdentifier.qualified(
                        properties.getSourceSchema(), "assessment_responses");
                String reportsTable = ClientHubIdentifier.qualified(
                        properties.getSourceSchema(), "assessment_reports");
                SourceAssessmentsInventory inventory = new SourceAssessmentsInventory(
                        count(connection, "SELECT COUNT(*) FROM " + templatesTable),
                        countWhere(connection, templatesTable, "name IS NULL OR BTRIM(name) = ''"),
                        count(connection, "SELECT COUNT(*) FROM " + sectionsTable),
                        countWhere(connection, sectionsTable, "template_id IS NULL"),
                        countWhere(connection, sectionsTable, "title IS NULL OR BTRIM(title) = ''"),
                        count(connection, "SELECT COUNT(*) FROM " + questionsTable),
                        countWhere(connection, questionsTable, "section_id IS NULL"),
                        countWhere(connection, questionsTable,
                                "question_text IS NULL OR BTRIM(question_text) = ''"),
                        countWhere(connection, questionsTable,
                                "question_type IS NULL OR BTRIM(question_type) = ''"),
                        count(connection, "SELECT COUNT(*) FROM " + optionsTable),
                        countWhere(connection, optionsTable, "question_id IS NULL"),
                        countWhere(connection, optionsTable,
                                "option_text IS NULL OR BTRIM(option_text) = ''"),
                        count(connection, "SELECT COUNT(*) FROM " + assignmentsTable),
                        countWhere(connection, assignmentsTable, "template_id IS NULL"),
                        countWhere(connection, assignmentsTable, "client_id IS NULL"),
                        count(connection, "SELECT COUNT(*) FROM " + responsesTable),
                        countWhere(connection, responsesTable, "assignment_id IS NULL"),
                        countWhere(connection, responsesTable, "question_id IS NULL"),
                        count(connection, "SELECT COUNT(*) FROM " + reportsTable),
                        countWhere(connection, reportsTable, "assignment_id IS NULL"));
                connection.rollback();
                return inventory;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceAssessmentTemplateRecord> loadAssessmentTemplateRecords(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String table = ClientHubIdentifier.qualified(properties.getSourceSchema(), "assessment_templates");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, name, description, category, is_standardized, is_active, created_by_id,
                               version, created_at, updated_at
                        FROM %s
                        ORDER BY id
                        """.formatted(table))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceAssessmentTemplateRecord> templates = new ArrayList<>();
                        while (rows.next()) {
                            templates.add(new SourceAssessmentTemplateRecord(
                                    String.valueOf(rows.getLong("id")),
                                    rows.getString("name"),
                                    rows.getString("description"),
                                    rows.getString("category"),
                                    rows.getBoolean("is_standardized"),
                                    rows.getBoolean("is_active"),
                                    nullableLongString(rows, "created_by_id"),
                                    rows.getString("version"),
                                    instant(rows, "created_at", properties),
                                    instant(rows, "updated_at", properties)));
                        }
                        connection.rollback();
                        return templates;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceAssessmentSectionRecord> loadAssessmentSectionRecords(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String table = ClientHubIdentifier.qualified(properties.getSourceSchema(), "assessment_sections");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, template_id, title, description, access_level, is_scoring,
                               report_mapping::text AS report_mapping, ai_report_prompt, sort_order,
                               created_at, updated_at
                        FROM %s
                        ORDER BY id
                        """.formatted(table))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceAssessmentSectionRecord> sections = new ArrayList<>();
                        while (rows.next()) {
                            sections.add(new SourceAssessmentSectionRecord(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "template_id"),
                                    rows.getString("title"),
                                    rows.getString("description"),
                                    rows.getString("access_level"),
                                    rows.getBoolean("is_scoring"),
                                    rows.getString("report_mapping"),
                                    rows.getString("ai_report_prompt"),
                                    nullableInteger(rows, "sort_order"),
                                    instant(rows, "created_at", properties),
                                    instant(rows, "updated_at", properties)));
                        }
                        connection.rollback();
                        return sections;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceAssessmentQuestionRecord> loadAssessmentQuestionRecords(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String table = ClientHubIdentifier.qualified(properties.getSourceSchema(), "assessment_questions");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, section_id, question_text, question_type, is_required, sort_order,
                               rating_min, rating_max, rating_labels::text AS rating_labels,
                               contributes_to_score, created_at, updated_at
                        FROM %s
                        ORDER BY id
                        """.formatted(table))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceAssessmentQuestionRecord> questions = new ArrayList<>();
                        while (rows.next()) {
                            questions.add(new SourceAssessmentQuestionRecord(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "section_id"),
                                    rows.getString("question_text"),
                                    rows.getString("question_type"),
                                    rows.getBoolean("is_required"),
                                    nullableInteger(rows, "sort_order"),
                                    nullableInteger(rows, "rating_min"),
                                    nullableInteger(rows, "rating_max"),
                                    stringLabels(rows, "rating_labels"),
                                    rows.getBoolean("contributes_to_score"),
                                    instant(rows, "created_at", properties),
                                    instant(rows, "updated_at", properties)));
                        }
                        connection.rollback();
                        return questions;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceAssessmentQuestionOptionRecord> loadAssessmentQuestionOptionRecords(
            ClientHubMigrationProperties properties) throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String table = ClientHubIdentifier.qualified(
                        properties.getSourceSchema(), "assessment_question_options");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, question_id, option_text, option_value, sort_order
                        FROM %s
                        ORDER BY id
                        """.formatted(table))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceAssessmentQuestionOptionRecord> options = new ArrayList<>();
                        while (rows.next()) {
                            BigDecimal optionValue = rows.getBigDecimal("option_value");
                            options.add(new SourceAssessmentQuestionOptionRecord(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "question_id"),
                                    rows.getString("option_text"),
                                    optionValue,
                                    nullableInteger(rows, "sort_order")));
                        }
                        connection.rollback();
                        return options;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceAssessmentAssignmentRecord> loadAssessmentAssignmentRecords(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String table = ClientHubIdentifier.qualified(properties.getSourceSchema(), "assessment_assignments");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, template_id, client_id, assigned_by_id, status, due_date, completed_at,
                               finalized_at, client_submitted_at, therapist_completed_at, total_score, notes,
                               created_at, updated_at
                        FROM %s
                        ORDER BY id
                        """.formatted(table))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceAssessmentAssignmentRecord> assignments = new ArrayList<>();
                        while (rows.next()) {
                            assignments.add(new SourceAssessmentAssignmentRecord(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "template_id"),
                                    nullableLongString(rows, "client_id"),
                                    nullableLongString(rows, "assigned_by_id"),
                                    rows.getString("status"),
                                    localDate(rows, "due_date"),
                                    instant(rows, "completed_at", properties),
                                    instant(rows, "finalized_at", properties),
                                    instant(rows, "client_submitted_at", properties),
                                    instant(rows, "therapist_completed_at", properties),
                                    rows.getBigDecimal("total_score"),
                                    rows.getString("notes"),
                                    instant(rows, "created_at", properties),
                                    instant(rows, "updated_at", properties)));
                        }
                        connection.rollback();
                        return assignments;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceAssessmentResponseRecord> loadAssessmentResponseRecords(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String table = ClientHubIdentifier.qualified(properties.getSourceSchema(), "assessment_responses");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, assignment_id, question_id, responder_id, response_text, selected_options,
                               selected_option_id, rating_value, score_value, created_at, updated_at
                        FROM %s
                        ORDER BY id
                        """.formatted(table))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceAssessmentResponseRecord> responses = new ArrayList<>();
                        while (rows.next()) {
                            Object ratingValue = rows.getObject("rating_value");
                            List<String> selected = new ArrayList<>(integerArrayAsStrings(rows, "selected_options"));
                            String selectedOptionId = nullableLongString(rows, "selected_option_id");
                            if (selectedOptionId != null && !selected.contains(selectedOptionId)) {
                                selected.add(selectedOptionId);
                            }
                            responses.add(new SourceAssessmentResponseRecord(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "assignment_id"),
                                    nullableLongString(rows, "question_id"),
                                    nullableLongString(rows, "responder_id"),
                                    rows.getString("response_text"),
                                    List.copyOf(selected),
                                    ratingValue == null ? null : String.valueOf(ratingValue),
                                    rows.getBigDecimal("score_value"),
                                    instant(rows, "created_at", properties),
                                    instant(rows, "updated_at", properties)));
                        }
                        connection.rollback();
                        return responses;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceAssessmentReportRecord> loadAssessmentReportRecords(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String table = ClientHubIdentifier.qualified(properties.getSourceSchema(), "assessment_reports");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, assignment_id, generated_content, draft_content, final_content,
                               report_data::text AS report_data, is_draft, is_finalized,
                               generated_at, edited_at, finalized_at, exported_at,
                               created_by_id, finalized_by_id
                        FROM %s
                        ORDER BY id
                        """.formatted(table))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceAssessmentReportRecord> reports = new ArrayList<>();
                        while (rows.next()) {
                            Instant generatedAt = instant(rows, "generated_at", properties);
                            Instant editedAt = instant(rows, "edited_at", properties);
                            Instant finalizedAt = instant(rows, "finalized_at", properties);
                            Instant exportedAt = instant(rows, "exported_at", properties);
                            Instant createdAt = generatedAt != null ? generatedAt
                                    : (editedAt != null ? editedAt : finalizedAt);
                            Instant updatedAt = editedAt != null ? editedAt
                                    : (finalizedAt != null ? finalizedAt
                                    : (exportedAt != null ? exportedAt : generatedAt));
                            reports.add(new SourceAssessmentReportRecord(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "assignment_id"),
                                    rows.getString("generated_content"),
                                    rows.getString("draft_content"),
                                    rows.getString("final_content"),
                                    rows.getString("report_data"),
                                    rows.getBoolean("is_draft"),
                                    rows.getBoolean("is_finalized"),
                                    generatedAt,
                                    editedAt,
                                    finalizedAt,
                                    exportedAt,
                                    nullableLongString(rows, "created_by_id"),
                                    nullableLongString(rows, "finalized_by_id"),
                                    createdAt,
                                    updatedAt));
                        }
                        connection.rollback();
                        return reports;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    AssessmentTargetState loadAssessmentsTargetState(JdbcTemplate jdbcTemplate, TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException(
                    "Exactly one target organisation is required to load assessments target state");
        }
        String templatesTable = ClientHubIdentifier.qualified(target.schemaName(), "assessment_templates");
        Set<String> existingNameVersions = Set.copyOf(jdbcTemplate.query(
                "SELECT name, version_number FROM " + templatesTable
                        + " WHERE name IS NOT NULL AND is_deleted = false",
                (rs, rowNum) -> rs.getString("name") + "\u0000" + rs.getInt("version_number")));
        return new AssessmentTargetState(
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "clients"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "users"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "assessment_templates"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "assessment_sections"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "assessment_questions"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "assessment_question_options"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "assessment_assignments"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "assessment_responses"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "assessment_reports"),
                existingNameVersions);
    }

    SourceTranscriptsInventory inspectTranscriptsSource(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String table = ClientHubIdentifier.qualified(properties.getSourceSchema(), "session_transcripts");
                SourceTranscriptsInventory inventory = new SourceTranscriptsInventory(
                        count(connection, "SELECT COUNT(*) FROM " + table),
                        countWhere(connection, table, "session_id IS NULL"),
                        countWhere(connection, table, "client_id IS NULL"),
                        countWhere(connection, table, "therapist_id IS NULL"));
                connection.rollback();
                return inventory;
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    List<SourceTranscriptRecord> loadTranscriptRecords(ClientHubMigrationProperties properties)
            throws SQLException {
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                String table = ClientHubIdentifier.qualified(properties.getSourceSchema(), "session_transcripts");
                try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, session_id, client_id, therapist_id, content, raw_content, language,
                               translated_to_english, duration_seconds, chunk_count, word_count, status,
                               error_message, created_at, updated_at, upload_id
                        FROM %s
                        ORDER BY id
                        """.formatted(table))) {
                    try (ResultSet rows = statement.executeQuery()) {
                        List<SourceTranscriptRecord> transcripts = new ArrayList<>();
                        while (rows.next()) {
                            transcripts.add(new SourceTranscriptRecord(
                                    String.valueOf(rows.getLong("id")),
                                    nullableLongString(rows, "session_id"),
                                    nullableLongString(rows, "client_id"),
                                    nullableLongString(rows, "therapist_id"),
                                    rows.getString("content"),
                                    rows.getString("raw_content"),
                                    rows.getString("language"),
                                    rows.getObject("translated_to_english") != null
                                            && rows.getBoolean("translated_to_english"),
                                    nullableInteger(rows, "duration_seconds"),
                                    nullableInteger(rows, "chunk_count"),
                                    nullableInteger(rows, "word_count"),
                                    rows.getString("status"),
                                    rows.getString("error_message"),
                                    instant(rows, "created_at", properties),
                                    instant(rows, "updated_at", properties),
                                    rows.getString("upload_id")));
                        }
                        connection.rollback();
                        return transcripts;
                    }
                }
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    TranscriptsTargetState loadTranscriptsTargetState(JdbcTemplate jdbcTemplate, TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException(
                    "Exactly one target organisation is required to load transcripts target state");
        }
        return new TranscriptsTargetState(
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "sessions"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "clients"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "users"),
                loadMappedSourceIds(jdbcTemplate, target.organisationId(), "session_transcripts"));
    }

    DocumentBinariesTargetState loadDocumentBinariesTargetState(
            JdbcTemplate jdbcTemplate, TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException(
                    "Exactly one target organisation is required to load document binaries target state");
        }
        Map<String, Long> documentMappings = new HashMap<>();
        jdbcTemplate.query("""
                SELECT source_id, target_id
                FROM public.clienthub_legacy_id_mappings
                WHERE organisation_id = ?
                  AND source_system = 'ClientHubAI'
                  AND entity_name = 'documents'
                """,
                (RowCallbackHandler) rs -> documentMappings.put(rs.getString("source_id"), rs.getLong("target_id")),
                target.organisationId());

        String documentsTable = ClientHubIdentifier.qualified(target.schemaName(), "documents");
        Map<Long, MappedDocumentBinaryRef> byTargetId = new HashMap<>();
        if (!documentMappings.isEmpty()) {
            // original_name is encrypted at rest; leave null here and resolve via JPA on execute.
            jdbcTemplate.query(
                    "SELECT id, file_name, scan_status, client_id, mime_type FROM "
                            + documentsTable + " WHERE is_deleted = false",
                    (RowCallbackHandler) rs -> {
                        Long id = rs.getLong("id");
                        byTargetId.put(id, new MappedDocumentBinaryRef(
                                null,
                                id,
                                rs.getString("file_name"),
                                null,
                                rs.getString("scan_status"),
                                rs.getLong("client_id"),
                                rs.getString("mime_type")));
                    });
        }

        List<MappedDocumentBinaryRef> mapped = new ArrayList<>();
        for (Map.Entry<String, Long> entry : documentMappings.entrySet()) {
            MappedDocumentBinaryRef doc = byTargetId.get(entry.getValue());
            if (doc == null) {
                mapped.add(new MappedDocumentBinaryRef(
                        entry.getKey(), entry.getValue(), null, null, null, null, null));
                continue;
            }
            mapped.add(new MappedDocumentBinaryRef(
                    entry.getKey(),
                    doc.targetDocumentId(),
                    doc.fileName(),
                    null,
                    doc.scanStatus(),
                    doc.targetClientId(),
                    doc.mimeType()));
        }
        return new DocumentBinariesTargetState(List.copyOf(mapped));
    }

    private Connection openReadOnlyConnection(ClientHubMigrationProperties properties) throws SQLException {
        DriverManager.setLoginTimeout(properties.getConnectTimeoutSeconds());
        Properties connectionProperties = new Properties();
        connectionProperties.put("user", properties.getSourceUsername());
        if (hasText(properties.getSourcePassword())) {
            connectionProperties.put("password", properties.getSourcePassword());
        }
        // Prevent indefinite hangs on stalled Azure connections (seen during loadClientRefs).
        int timeoutSeconds = Math.max(30, properties.getConnectTimeoutSeconds());
        connectionProperties.put("connectTimeout", String.valueOf(timeoutSeconds));
        connectionProperties.put("socketTimeout", String.valueOf(Math.max(120, timeoutSeconds * 4)));
        Connection connection = DriverManager.getConnection(properties.getSourceUrl(), connectionProperties);
        connection.setReadOnly(true);
        return connection;
    }

    private SourceInventory inspectSource(Connection connection, String schemaName) throws SQLException {
        List<String> tableNames = loadTableNames(connection, schemaName);
        List<TableInventory> tables = new ArrayList<>(tableNames.size());
        StringBuilder fingerprintInput = new StringBuilder();

        for (String tableName : tableNames) {
            long rowCount = countRows(connection, schemaName, tableName);
            List<ColumnInventory> columns = loadColumns(connection, schemaName, tableName);
            tables.add(new TableInventory(tableName, rowCount, columns));
            appendColumnFingerprint(tableName, columns, fingerprintInput);
        }

        String fingerprint = ClientHubIdentifier.sha256Hex(fingerprintInput.toString());
        return new SourceInventory(schemaName, tables, fingerprint);
    }

    private List<String> loadTableNames(Connection connection, String schemaName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = ?
                  AND table_type = 'BASE TABLE'
                ORDER BY table_name
                """)) {
            statement.setString(1, schemaName);
            try (ResultSet rows = statement.executeQuery()) {
                List<String> tableNames = new ArrayList<>();
                while (rows.next()) {
                    tableNames.add(rows.getString("table_name"));
                }
                return tableNames;
            }
        }
    }

    private long countRows(Connection connection, String schemaName, String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + ClientHubIdentifier.qualified(schemaName, tableName);
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            rows.next();
            return rows.getLong(1);
        }
    }

    private List<ColumnInventory> loadColumns(
            Connection connection,
            String schemaName,
            String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT column_name, data_type, is_nullable, column_default
                FROM information_schema.columns
                WHERE table_schema = ?
                  AND table_name = ?
                ORDER BY ordinal_position
                """)) {
            statement.setString(1, schemaName);
            statement.setString(2, tableName);
            try (ResultSet rows = statement.executeQuery()) {
                List<ColumnInventory> columns = new ArrayList<>();
                while (rows.next()) {
                    columns.add(new ColumnInventory(
                            rows.getString("column_name"),
                            rows.getString("data_type"),
                            "YES".equalsIgnoreCase(rows.getString("is_nullable")),
                            rows.getString("column_default")));
                }
                return columns;
            }
        }
    }

    private void appendColumnFingerprint(
            String tableName,
            List<ColumnInventory> columns,
            StringBuilder fingerprintInput) {
        for (ColumnInventory column : columns) {
            fingerprintInput
                    .append(tableName).append('|')
                    .append(column.columnName()).append('|')
                    .append(column.dataType()).append('|')
                    .append(column.nullable()).append('|')
                    .append(column.columnDefault()).append('\n');
        }
    }

    private long count(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            rows.next();
            return rows.getLong(1);
        }
    }

    private Map<String, Long> groupedCounts(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            Map<String, Long> counts = new LinkedHashMap<>();
            while (rows.next()) {
                counts.put(rows.getString("key"), rows.getLong(2));
            }
            return counts;
        }
    }

    private long countDuplicateGroups(Connection connection, String usersTable, String columnOrExpression) throws SQLException {
        String sql = "SELECT COUNT(*) FROM (SELECT LOWER(" + columnOrExpression + ")"
                + " FROM " + usersTable
                + " WHERE " + columnOrExpression + " IS NOT NULL"
                + " AND BTRIM(" + columnOrExpression + ") <> ''"
                + " GROUP BY LOWER(" + columnOrExpression + ") HAVING COUNT(*) > 1) duplicate_groups";
        return count(connection, sql);
    }

    private long countWhere(Connection connection, String tableName, String predicate) throws SQLException {
        return count(connection, "SELECT COUNT(*) FROM " + tableName + " WHERE " + predicate);
    }

    /**
     * Restrict notification history import to recent rows. {@code lookbackMonths <= 0} means no cutoff.
     * Column names are internal constants only (never user input).
     */
    static String notificationRecencyPredicate(ClientHubMigrationProperties properties, String columnName) {
        int months = properties.getNotificationLookbackMonths();
        if (months <= 0) {
            return "TRUE";
        }
        LocalDateTime cutoff = LocalDateTime.now(sourceZone(properties)).minusMonths(months);
        return columnName + " >= TIMESTAMP '" + cutoff + "'";
    }

    private Set<String> loadMappedSourceIds(JdbcTemplate jdbcTemplate, Long organisationId, String entityName) {
        return Set.copyOf(jdbcTemplate.query(
                """
                SELECT source_id
                FROM public.clienthub_legacy_id_mappings
                WHERE organisation_id = ?
                  AND source_system = 'ClientHubAI'
                  AND entity_name = ?
                """,
                (rs, rowNum) -> rs.getString("source_id"),
                organisationId,
                entityName));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalise(String value) {
        return value == null || value.isBlank() ? "" : value.trim().toLowerCase();
    }

    private static LocalDate localDate(ResultSet rows, String columnName) throws SQLException {
        Date date = rows.getDate(columnName);
        if (date != null) {
            return date.toLocalDate();
        }
        // Some V1 columns (e.g. assessment_assignments.due_date) are timestamptz.
        return localDateInZone(rows, columnName, ZoneOffset.UTC);
    }

    /**
     * Convert a DATE or TIMESTAMPTZ column to a calendar date.
     * ClientHub writes billing_date as a UTC calendar day ({@code toISOString().split('T')[0]})
     * stored in timestamptz at midnight UTC — keep that UTC day so Sept 6 does not become Sept 5
     * in America/Toronto. Non-midnight timestamps convert in {@code zone}.
     */
    static LocalDate localDateInZone(ResultSet rows, String columnName, ZoneId zone) throws SQLException {
        OffsetDateTime offsetDateTime = rows.getObject(columnName, OffsetDateTime.class);
        if (offsetDateTime != null) {
            return toBillingLocalDate(offsetDateTime.toInstant(), zone);
        }
        Timestamp timestamp = rows.getTimestamp(columnName);
        if (timestamp != null) {
            try {
                return toBillingLocalDate(timestamp.toInstant(), zone);
            } catch (Exception ignored) {
                return timestamp.toLocalDateTime().toLocalDate();
            }
        }
        Date date = rows.getDate(columnName);
        return date == null ? null : date.toLocalDate();
    }

    /**
     * Midnight-UTC timestamps are date-only values from ClientHub — keep the UTC calendar day.
     * Otherwise interpret the instant in the practice/system zone.
     */
    public static LocalDate toBillingLocalDate(Instant instant, ZoneId zone) {
        if (instant == null) {
            return null;
        }
        var utc = instant.atZone(ZoneOffset.UTC);
        if (utc.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)) {
            return utc.toLocalDate();
        }
        ZoneId resolved = zone == null ? ZoneOffset.UTC : zone;
        return instant.atZone(resolved).toLocalDate();
    }

    public static LocalDate toLocalDateInZone(Instant instant, ZoneId zone) {
        return toBillingLocalDate(instant, zone);
    }

    private static Instant instant(ResultSet rows, String columnName, ClientHubMigrationProperties properties)
            throws SQLException {
        // Prefer OffsetDateTime so timestamptz columns (e.g. documents.reviewed_at) work.
        OffsetDateTime offsetDateTime = rows.getObject(columnName, OffsetDateTime.class);
        if (offsetDateTime != null) {
            return offsetDateTime.toInstant();
        }
        // ClientHub also stores UTC instants in timestamp-without-time-zone columns.
        // Never use Timestamp#toInstant() — that applies the JVM default zone.
        LocalDateTime local = rows.getObject(columnName, LocalDateTime.class);
        if (local != null) {
            return local.atZone(sourceZone(properties)).toInstant();
        }
        Timestamp timestamp = rows.getTimestamp(columnName);
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime().atZone(sourceZone(properties)).toInstant();
    }

    public static ZoneId sourceZone(ClientHubMigrationProperties properties) {
        String configured = properties.getSourceTimezone();
        if (configured != null && !configured.isBlank()) {
            return ZoneId.of(configured.trim());
        }
        return ZoneId.of("UTC");
    }

    /** Prefer organisation/practice timezone; fall back to migration source-timezone. */
    public static ZoneId billingDateZone(TargetInventory target, ClientHubMigrationProperties properties) {
        if (target != null && target.timezone() != null && !target.timezone().isBlank()) {
            return ZoneId.of(target.timezone().trim());
        }
        return sourceZone(properties);
    }

    private static Integer nullableInteger(ResultSet rows, String columnName) throws SQLException {
        int value = rows.getInt(columnName);
        return rows.wasNull() ? null : value;
    }

    private static String nullableLongString(ResultSet rows, String columnName) throws SQLException {
        Object value = rows.getObject(columnName);
        return value == null ? null : String.valueOf(((Number) value).longValue());
    }

    private static List<String> stringLabels(ResultSet rows, String columnName) throws SQLException {
        String raw = rows.getString(columnName);
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String trimmed = raw.trim();
        if ("{}".equals(trimmed) || "[]".equals(trimmed)) {
            return List.of();
        }
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            // Postgres text-array literal rendered as varchar, e.g. {Poor,Good}
            String inner = trimmed.substring(1, trimmed.length() - 1).trim();
            if (inner.isEmpty()) {
                return List.of();
            }
            List<String> out = new ArrayList<>();
            for (String part : inner.split(",")) {
                String label = part.trim();
                if (label.startsWith("\"") && label.endsWith("\"") && label.length() >= 2) {
                    label = label.substring(1, label.length() - 1);
                }
                if (!label.isBlank()) {
                    out.add(label);
                }
            }
            return List.copyOf(out);
        }
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            String inner = trimmed.substring(1, trimmed.length() - 1).trim();
            if (inner.isEmpty()) {
                return List.of();
            }
            List<String> out = new ArrayList<>();
            for (String part : inner.split(",")) {
                String label = part.trim();
                if (label.startsWith("\"") && label.endsWith("\"") && label.length() >= 2) {
                    label = label.substring(1, label.length() - 1);
                }
                if (!label.isBlank()) {
                    out.add(label);
                }
            }
            return List.copyOf(out);
        }
        return List.of(trimmed);
    }

    private static List<String> textArray(ResultSet rows, String columnName) throws SQLException {
        Array array = rows.getArray(columnName);
        if (array == null) {
            return List.of();
        }
        Object raw = array.getArray();
        if (!(raw instanceof Object[] values)) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (Object value : values) {
            if (value != null && !String.valueOf(value).isBlank()) {
                out.add(String.valueOf(value).trim());
            }
        }
        return List.copyOf(out);
    }

    private static List<String> integerArrayAsStrings(ResultSet rows, String columnName) throws SQLException {
        Array array = rows.getArray(columnName);
        if (array == null) {
            return List.of();
        }
        Object raw = array.getArray();
        if (!(raw instanceof Object[] values)) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof Number number) {
                out.add(String.valueOf(number.longValue()));
            }
        }
        return List.copyOf(out);
    }

    public record SourceInventory(String schemaName, List<TableInventory> tables, String schemaFingerprintSha256) {
        public long totalRows() {
            return tables.stream().mapToLong(TableInventory::rowCount).sum();
        }

        public Map<String, TableInventory> tablesByName() {
            Map<String, TableInventory> tablesByName = new TreeMap<>();
            for (TableInventory table : tables) {
                tablesByName.put(table.tableName(), table);
            }
            return tablesByName;
        }
    }

    public record TableInventory(String tableName, long rowCount, List<ColumnInventory> columns) {
        public boolean hasColumn(String columnName) {
            return columns.stream().anyMatch(column -> column.columnName().equals(columnName));
        }
    }

    public record ColumnInventory(String columnName, String dataType, boolean nullable, String columnDefault) {
    }

    public record TargetInventory(
            int matchingTargetOrganisations,
            int demoTenantCount,
            Long organisationId,
            String schemaName,
            String timezone) {

        public TargetInventory(
                int matchingTargetOrganisations,
                int demoTenantCount,
                Long organisationId,
                String schemaName) {
            this(matchingTargetOrganisations, demoTenantCount, organisationId, schemaName, null);
        }

        public boolean resolved() {
            return matchingTargetOrganisations == 1 && organisationId != null && schemaName != null && !schemaName.isBlank();
        }

        public ZoneId zoneIdOrUtc() {
            if (timezone == null || timezone.isBlank()) {
                return ZoneId.of("UTC");
            }
            return ZoneId.of(timezone.trim());
        }
    }

    public record SourceStaffAuthInventory(
            long userRows,
            Map<String, Long> roleCounts,
            Map<String, Long> statusCounts,
            long bcryptCompatiblePasswordRows,
            long unsupportedPasswordRows,
            long blankEmailRows,
            long blankUsernameRows,
            long duplicateEmailGroups,
            long duplicateUsernameGroups) {
    }

    public record SourceStaffUserRef(
            String legacyUserId,
            String username,
            String email,
            String normalisedUsername,
            String normalisedEmail,
            String fullName,
            String phone,
            String sourceRole,
            String sourceStatus,
            String passwordHash,
            boolean emailVerified,
            boolean bcryptCompatiblePassword) {
    }

    public record StaffAuthTargetState(
            Set<String> mappedLegacyIds,
            Set<String> authNormalisedEmails,
            Set<String> authNormalisedUsernames,
            Set<String> tenantUserEmails) {
    }

    public record SourceClientInventory(
            long clientRows,
            long blankClientIdRows,
            long blankFullNameRows,
            long duplicateClientIdGroups,
            long assignedTherapistRows,
            long emailContactRows,
            long phoneContactRows,
            long emergencyContactRows,
            long addressRows,
            long completeInsuranceRows,
            long incompleteInsuranceRows,
            long referralRows,
            long employmentRows) {
    }

    public record SourceClientRef(
            String legacyClientPk,
            String clientId,
            String fullName,
            String assignedTherapistLegacyId) {

        boolean missingClientId() {
            return clientId == null || clientId.isBlank();
        }

        boolean missingFullName() {
            return fullName == null || fullName.isBlank();
        }
    }

    public record ClientTargetState(
            Set<String> mappedClientIds,
            Set<String> mappedUserIds,
            int tenantClientRows) {
    }

    public record SourceClientRecord(
            String legacyClientPk,
            String clientId,
            String fullName,
            LocalDate dateOfBirth,
            String gender,
            String maritalStatus,
            String preferredLanguage,
            String pronouns,
            String timezone,
            LocalDate startDate,
            String serviceType,
            String serviceFrequency,
            String clientType,
            String status,
            String stage,
            Instant lastUpdateDate,
            String notes,
            String assignedTherapistLegacyId,
            String email,
            String phone,
            String emergencyContactName,
            String emergencyContactPhone,
            String emergencyContactRelationship,
            String streetAddress1,
            String streetAddress2,
            String city,
            String province,
            String postalCode,
            String country,
            String addressLegacy,
            String stateLegacy,
            String zipCodeLegacy,
            String insuranceProvider,
            String policyNumber,
            String groupNumber,
            String insurancePhone,
            BigDecimal copayAmount,
            BigDecimal deductible,
            String referrerName,
            LocalDate referralDate,
            String referenceNumber,
            String clientSource,
            String referralSource,
            String referralType,
            String referringPerson,
            String referralNotes,
            String employmentStatus,
            String educationLevel,
            Integer dependents) {
    }

    public record SourceSessionInventory(
            long sessionRows,
            long missingClientRows,
            long missingTherapistRows,
            long missingServiceRows,
            long missingDateRows,
            long missingTypeRows,
            long noteRows,
            long notesMissingSessionRows,
            long notesMissingClientRows,
            long notesMissingTherapistRows,
            long notesMissingDateRows) {
    }

    public record SourceSessionRef(
            String legacySessionPk,
            String clientLegacyId,
            String therapistLegacyId,
            String serviceLegacyId,
            boolean hasSessionDate,
            boolean hasSessionType) {
    }

    public record SourceSessionNoteRef(
            String legacyNotePk,
            String sessionLegacyId,
            String clientLegacyId,
            String therapistLegacyId,
            boolean hasDate) {
    }

    public record SourceSessionRecord(
            String legacySessionPk,
            String clientLegacyId,
            String therapistLegacyId,
            String serviceLegacyId,
            String roomLegacyId,
            Instant sessionDate,
            String sessionType,
            String status,
            Integer duration,
            String notes,
            BigDecimal calculatedRate,
            boolean insuranceApplicable,
            String billingNotes,
            boolean zoomEnabled,
            String recurrenceGroupId) {
    }

    public record SourceSessionNoteRecord(
            String legacyNotePk,
            String sessionLegacyId,
            String clientLegacyId,
            String therapistLegacyId,
            Instant noteDate,
            String sessionFocus,
            String symptoms,
            String shortTermGoals,
            String intervention,
            String progress,
            String remarks,
            String recommendations,
            Integer clientRating,
            Integer therapistRating,
            Integer progressTowardGoals,
            Integer moodBefore,
            Integer moodAfter,
            Integer riskSuicidalIdeation,
            Integer riskSelfHarm,
            Integer riskHomicidalIdeation,
            Integer riskPsychosis,
            Integer riskSubstanceUse,
            Integer riskImpulsivity,
            Integer riskAggression,
            Integer riskTraumaSymptoms,
            Integer riskNonAdherence,
            Integer riskSupportSystem,
            String generatedContent,
            String draftContent,
            String finalContent,
            boolean draft,
            boolean finalized,
            Instant finalizedAt,
            boolean aiEnabled,
            String customAiPrompt,
            String aiProcessingStatus,
            String voiceTranscription) {
    }

    public record SessionTargetState(
            Set<String> mappedClientIds,
            Set<String> mappedUserIds,
            Set<String> mappedServiceIds,
            Set<String> mappedSessionIds,
            Set<String> mappedSessionNoteIds) {
    }

    public record SourceBillingInventory(
            long billingRows,
            long billingMissingSessionRows,
            long billingBlankServiceCodeRows,
            long billingMissingRateRows,
            long billingMissingTotalRows,
            long paymentTransactionRows,
            long paymentMissingBillingRows,
            long paymentBlankSourceRows,
            long paymentMissingAmountRows) {
    }

    public record SourceBillingRef(
            String legacyBillingPk,
            String sessionLegacyId,
            String serviceCode,
            BigDecimal ratePerUnit,
            BigDecimal totalAmount) {

        boolean missingRequiredFields() {
            return sessionLegacyId == null
                    || serviceCode == null || serviceCode.isBlank()
                    || ratePerUnit == null
                    || totalAmount == null;
        }
    }

    public record SourcePaymentTransactionRef(
            String legacyPaymentTransactionPk,
            String billingLegacyId,
            String source,
            BigDecimal amount) {

        boolean missingRequiredFields() {
            return billingLegacyId == null
                    || source == null || source.isBlank()
                    || amount == null;
        }
    }

    public record SourceBillingRecord(
            String legacyBillingPk,
            String sessionLegacyId,
            String serviceCode,
            Integer units,
            BigDecimal ratePerUnit,
            BigDecimal totalAmount,
            boolean insuranceCovered,
            BigDecimal copayAmount,
            LocalDate billingDate,
            String paymentStatus,
            String discountType,
            BigDecimal discountValue,
            BigDecimal discountAmount,
            BigDecimal paymentAmount,
            BigDecimal clientPaidAmount,
            BigDecimal insurancePaidAmount,
            LocalDate paymentDate,
            String paymentReference,
            String paymentMethod,
            String paymentNotes) {
    }

    public record SourcePaymentTransactionRecord(
            String legacyPaymentTransactionPk,
            String billingLegacyId,
            String source,
            BigDecimal amount,
            String paymentMethod,
            String referenceNumber,
            String notes,
            LocalDate paymentDate,
            Instant recordedAt) {
    }

    public record BillingTargetState(
            Set<String> mappedSessionIds,
            Set<String> mappedBillingIds,
            Set<String> mappedPaymentTransactionIds) {
    }

    public record SourceDocumentInventory(
            long documentRows,
            long missingClientRows,
            long blankFileNameRows,
            long blankOriginalNameRows,
            long missingFileSizeRows,
            long blankMimeTypeRows,
            long blankCategoryRows,
            long uploadedByRows,
            long reviewedByRows,
            long sharedInPortalRows) {
    }

    public record SourceDocumentRef(
            String legacyDocumentPk,
            String clientLegacyId,
            String uploadedByLegacyId,
            String reviewedByLegacyId,
            String fileName,
            String originalName,
            Integer fileSize,
            String mimeType,
            String category) {

        boolean missingRequiredFields() {
            return clientLegacyId == null
                    || fileName == null || fileName.isBlank()
                    || originalName == null || originalName.isBlank()
                    || fileSize == null
                    || category == null || category.isBlank();
        }
    }

    public record SourceDocumentRecord(
            String legacyDocumentPk,
            String clientLegacyId,
            String uploadedByLegacyId,
            String fileName,
            String originalName,
            Integer fileSize,
            String mimeType,
            String category,
            boolean sharedInPortal,
            Integer downloadCount,
            boolean requiresTherapistReview,
            boolean requiresSupervisorReview,
            String reviewStatus,
            String reviewedByLegacyId,
            Instant reviewedAt,
            String reviewNotes,
            Instant createdAt) {
    }

    public record DocumentTargetState(
            Set<String> mappedClientIds,
            Set<String> mappedUserIds,
            Set<String> mappedDocumentIds) {
    }

    public record SourceRoomIntegrationInventory(
            long roomRows,
            long blankRoomNumberRows,
            long blankRoomNameRows,
            long duplicateRoomNumberGroups,
            long sessionsWithRoomRows,
            long zoomEnabledSessionRows,
            long zoomEnabledMissingMeetingRows) {
    }

    public record SourceRoomRecord(
            String legacyRoomPk,
            String roomNumber,
            String roomName,
            Integer capacity,
            String equipment,
            boolean active) {

        boolean missingRequiredFields() {
            return roomNumber == null || roomNumber.isBlank()
                    || roomName == null || roomName.isBlank();
        }
    }

    public record SourceSessionIntegrationRecord(
            String legacySessionPk,
            String meetingId,
            String joinUrl,
            String password) {

        String legacyIntegrationKey() {
            return legacySessionPk + ":zoom";
        }
    }

    public record RoomIntegrationTargetState(
            Set<String> mappedRoomIds,
            Set<String> mappedSessionIds,
            Set<String> mappedSessionIntegrationIds,
            Set<String> existingRoomNumbers) {
    }

    public record SourceTaskInventory(
            long taskRows,
            long blankTitleRows,
            long blankClientRows,
            long commentRows,
            long blankCommentContentRows) {
    }

    public record SourceTaskRecord(
            String legacyTaskPk,
            String clientLegacyId,
            String assignedToLegacyId,
            String title,
            String description,
            String status,
            String priority,
            Instant dueDate,
            Instant completedAt,
            Instant createdAt,
            Instant updatedAt) {

        boolean missingRequiredFields() {
            return clientLegacyId == null || clientLegacyId.isBlank()
                    || title == null || title.isBlank();
        }
    }

    public record SourceTaskCommentRecord(
            String legacyCommentPk,
            String taskLegacyId,
            String authorLegacyId,
            String content,
            boolean internal,
            Instant createdAt,
            Instant updatedAt) {

        boolean missingRequiredFields() {
            return taskLegacyId == null || taskLegacyId.isBlank()
                    || authorLegacyId == null || authorLegacyId.isBlank()
                    || content == null || content.isBlank();
        }
    }

    public record TaskTargetState(
            Set<String> mappedClientIds,
            Set<String> mappedUserIds,
            Set<String> mappedTaskIds,
            Set<String> mappedTaskCommentIds) {
    }

    public record SourceServiceInventory(
            long serviceRows,
            long blankServiceCodeRows,
            long blankServiceNameRows,
            long missingDurationRows,
            long missingBaseRateRows,
            long duplicateServiceCodeGroups) {
    }

    public record SourceServiceRecord(
            String legacyServicePk,
            String serviceCode,
            String serviceName,
            String description,
            Integer duration,
            BigDecimal baseRate,
            String category,
            boolean active,
            boolean therapistVisible,
            boolean clientPortalVisible) {

        boolean missingRequiredFields() {
            return serviceCode == null || serviceCode.isBlank()
                    || serviceName == null || serviceName.isBlank()
                    || duration == null
                    || baseRate == null;
        }
    }

    public record ServiceTargetState(Set<String> mappedServiceIds, Set<String> existingServiceCodes) {
    }

    public record SourceNotificationInventory(
            long templateRows,
            long blankTemplateNameRows,
            long blankTemplateTypeRows,
            long blankTemplateSubjectRows,
            long blankTemplateBodyRows,
            long duplicateTemplateNameGroups,
            long triggerRows,
            long blankTriggerNameRows,
            long blankTriggerEventTypeRows,
            long blankTriggerEntityTypeRows,
            long preferenceRows,
            long preferenceMissingUserRows,
            long preferenceBlankTriggerTypeRows,
            long preferenceGlobalRows,
            long notificationRows,
            long notificationMissingUserRows,
            long notificationBlankTypeRows,
            long notificationBlankTitleRows,
            long notificationBlankMessageRows,
            long scheduledRows,
            long scheduledMissingTriggerRows,
            long scheduledBlankEntityTypeRows,
            long scheduledMissingEntityIdRows,
            long scheduledMissingExecuteAtRows) {
    }

    public record SourceNotificationTemplateRecord(
            String legacyTemplatePk,
            String name,
            String type,
            String subject,
            String bodyTemplate,
            String actionUrlTemplate,
            String actionLabel,
            String recipientRoles,
            String variables,
            boolean system,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {

        boolean missingRequiredFields() {
            return name == null || name.isBlank()
                    || type == null || type.isBlank()
                    || subject == null || subject.isBlank()
                    || bodyTemplate == null || bodyTemplate.isBlank();
        }
    }

    public record SourceNotificationTriggerRecord(
            String legacyTriggerPk,
            String name,
            String description,
            String eventType,
            String entityType,
            String conditionRules,
            String recipientRules,
            String templateLegacyId,
            String priority,
            Integer delayMinutes,
            Integer batchWindowMinutes,
            Integer maxBatchSize,
            boolean scheduled,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {

        boolean missingRequiredFields() {
            return name == null || name.isBlank()
                    || eventType == null || eventType.isBlank()
                    || entityType == null || entityType.isBlank();
        }
    }

    public record SourceNotificationPreferenceRecord(
            String legacyPreferencePk,
            String userLegacyId,
            String triggerType,
            String deliveryMethods,
            String timing,
            boolean enableInApp,
            boolean enableEmail,
            boolean enableSms,
            String quietHoursStart,
            String quietHoursEnd,
            boolean weekendsEnabled,
            Instant createdAt,
            Instant updatedAt) {

        boolean missingRequiredFields() {
            return userLegacyId == null || userLegacyId.isBlank()
                    || triggerType == null || triggerType.isBlank();
        }
    }

    public record SourceNotificationRecord(
            String legacyNotificationPk,
            String userLegacyId,
            String type,
            String title,
            String message,
            String data,
            String priority,
            boolean read,
            Instant readAt,
            String actionUrl,
            String actionLabel,
            String groupingKey,
            Instant expiresAt,
            String relatedEntityType,
            String relatedEntityLegacyId,
            Instant createdAt) {

        boolean missingRequiredFields() {
            return userLegacyId == null || userLegacyId.isBlank()
                    || type == null || type.isBlank()
                    || title == null || title.isBlank()
                    || message == null || message.isBlank();
        }
    }

    public record SourceScheduledNotificationRecord(
            String legacyScheduledPk,
            String triggerLegacyId,
            String sessionLegacyId,
            String entityType,
            String entityLegacyId,
            String entityData,
            Instant executeAt,
            String status,
            Integer retryCount,
            String lastError,
            Instant createdAt,
            Instant processedAt) {

        boolean missingRequiredFields() {
            return triggerLegacyId == null || triggerLegacyId.isBlank()
                    || entityType == null || entityType.isBlank()
                    || entityLegacyId == null || entityLegacyId.isBlank()
                    || executeAt == null;
        }
    }

    public record NotificationTargetState(
            Set<String> mappedUserIds,
            Set<String> mappedClientIds,
            Set<String> mappedSessionIds,
            Set<String> mappedTaskIds,
            Set<String> mappedDocumentIds,
            Set<String> mappedTemplateIds,
            Set<String> mappedTriggerIds,
            Set<String> mappedPreferenceIds,
            Set<String> mappedNotificationIds,
            Set<String> mappedScheduledIds,
            Set<String> existingTemplateNames,
            Set<String> existingTriggerNames) {
    }

    public record SourceTherapistScheduleInventory(
            long profileRows,
            long blankUserIdRows,
            long profilesWithWorkingHours,
            long profilesWithVirtualRoom,
            long profilesWithPhysicalRooms,
            long profilesWithLicense,
            long profilesWithSpecializations,
            long profilesWithEmergencyContact,
            long blockedTimeRows,
            long blankTherapistIdRows,
            long missingStartRows,
            long missingEndRows,
            long blankBlockTypeRows) {
    }

    public record SourceUserProfileScheduleRecord(
            String legacyProfilePk,
            String userLegacyId,
            String licenseNumber,
            String licenseType,
            String licenseState,
            LocalDate licenseExpiry,
            String licenseStatus,
            List<String> specializations,
            List<String> treatmentApproaches,
            List<String> ageGroups,
            List<String> languages,
            List<String> certifications,
            List<String> education,
            Integer yearsOfExperience,
            List<String> workingDays,
            String workingHoursJson,
            Integer maxClientsPerDay,
            Integer sessionDuration,
            String availabilityStatus,
            String virtualRoomLegacyId,
            List<String> physicalRoomLegacyIds,
            String emergencyContactName,
            String emergencyContactPhone,
            String emergencyContactRelationship,
            List<String> previousPositions,
            String clinicalExperience,
            String researchBackground,
            List<String> publications,
            List<String> professionalMemberships,
            List<String> continuingEducation,
            String supervisoryExperience,
            List<String> awardRecognitions,
            List<String> professionalReferences,
            String careerObjectives) {

        boolean missingRequiredFields() {
            return userLegacyId == null || userLegacyId.isBlank();
        }
    }

    public record SourceTherapistBlockedTimeRecord(
            String legacyBlockedPk,
            String therapistLegacyId,
            Instant startTime,
            Instant endTime,
            boolean allDay,
            String blockType,
            String reason,
            boolean recurring,
            String recurrencePattern,
            boolean active) {

        boolean missingRequiredFields() {
            return therapistLegacyId == null || therapistLegacyId.isBlank()
                    || startTime == null
                    || endTime == null
                    || blockType == null || blockType.isBlank();
        }
    }

    public record TherapistScheduleTargetState(
            Set<String> mappedUserIds,
            Set<String> mappedProfileIds,
            Set<String> mappedBlockedTimeIds,
            Set<String> mappedRoomIds,
            Set<String> existingProfileUserLegacyIds) {
    }

    public record SourceClinicalExtrasInventory(
            long consentRows,
            long blankConsentClientRows,
            long blankConsentTypeRows,
            long supervisorRows,
            long blankSupervisorIdRows,
            long blankTherapistIdRows,
            long portalEligibleRows,
            long portalMissingEmailRows,
            long checklistTemplateRows,
            long blankChecklistTemplateNameRows,
            long checklistItemRows,
            long blankChecklistItemTemplateRows,
            long blankChecklistItemTitleRows,
            long blankChecklistItemCategoryRows,
            long clientChecklistRows,
            long blankClientChecklistClientRows,
            long blankClientChecklistTemplateRows,
            long clientChecklistItemRows,
            long blankClientChecklistItemChecklistRows,
            long blankClientChecklistItemItemRows,
            long zoomEligibleUserRows) {
    }

    public record SourcePatientConsentRecord(
            String legacyConsentPk,
            String clientLegacyId,
            String consentType,
            String consentVersion,
            boolean granted,
            Instant grantedAt,
            Instant withdrawnAt,
            String ipAddress,
            String userAgent,
            String notes,
            Instant createdAt,
            Instant updatedAt) {

        boolean missingRequiredFields() {
            return clientLegacyId == null || clientLegacyId.isBlank()
                    || consentType == null || consentType.isBlank()
                    || grantedAt == null;
        }
    }

    public record SourceSupervisorAssignmentRecord(
            String legacyAssignmentPk,
            String supervisorLegacyId,
            String therapistLegacyId,
            Instant assignedDate,
            boolean active,
            String notes,
            String requiredMeetingFrequency,
            Instant nextMeetingDate,
            Instant lastMeetingDate,
            Instant createdAt,
            Instant updatedAt) {

        boolean missingRequiredFields() {
            return supervisorLegacyId == null || supervisorLegacyId.isBlank()
                    || therapistLegacyId == null || therapistLegacyId.isBlank()
                    || assignedDate == null;
        }
    }

    public record SourceClientPortalRecord(
            String legacyClientPk,
            String fullName,
            String email,
            boolean hasPortalAccess,
            String portalEmail,
            String portalPassword,
            boolean bcryptCompatiblePassword,
            Instant lastLogin,
            String activationToken,
            Instant updatedAt) {

        String resolvedPortalEmail() {
            if (portalEmail != null && !portalEmail.isBlank()) {
                return portalEmail.trim();
            }
            if (email != null && !email.isBlank()) {
                return email.trim();
            }
            return null;
        }

        boolean missingRequiredFields() {
            return resolvedPortalEmail() == null;
        }
    }

    public record SourceChecklistTemplateRecord(
            String legacyTemplatePk,
            String name,
            String description,
            String clientType,
            boolean active,
            Integer sortOrder,
            Instant createdAt,
            Instant updatedAt) {

        boolean missingRequiredFields() {
            return name == null || name.isBlank();
        }
    }

    public record SourceChecklistItemRecord(
            String legacyItemPk,
            String templateLegacyId,
            String title,
            String description,
            String category,
            boolean required,
            Integer daysFromStart,
            Integer sortOrder,
            Instant createdAt) {

        boolean missingRequiredFields() {
            return templateLegacyId == null || templateLegacyId.isBlank()
                    || title == null || title.isBlank()
                    || category == null || category.isBlank();
        }
    }

    public record SourceClientChecklistRecord(
            String legacyChecklistPk,
            String clientLegacyId,
            String templateLegacyId,
            boolean completed,
            Instant completedAt,
            String completedByLegacyId,
            String notes,
            LocalDate dueDate,
            Instant createdAt) {

        boolean missingRequiredFields() {
            return clientLegacyId == null || clientLegacyId.isBlank()
                    || templateLegacyId == null || templateLegacyId.isBlank();
        }
    }

    public record SourceClientChecklistItemRecord(
            String legacyChecklistItemPk,
            String clientChecklistLegacyId,
            String checklistItemLegacyId,
            boolean completed,
            Instant completedAt,
            String completedByLegacyId,
            String notes,
            Instant createdAt) {

        boolean missingRequiredFields() {
            return clientChecklistLegacyId == null || clientChecklistLegacyId.isBlank()
                    || checklistItemLegacyId == null || checklistItemLegacyId.isBlank();
        }
    }

    public record SourceUserZoomIntegrationRecord(
            String userLegacyId,
            String zoomAccountId,
            String zoomClientId,
            String zoomClientSecret,
            String zoomAccessToken,
            Instant zoomTokenExpiry,
            Instant updatedAt) {

        boolean missingRequiredFields() {
            return userLegacyId == null || userLegacyId.isBlank()
                    || ((zoomAccountId == null || zoomAccountId.isBlank())
                    && (zoomClientId == null || zoomClientId.isBlank()));
        }
    }

    public record ClinicalExtrasTargetState(
            Set<String> mappedClientIds,
            Set<String> mappedUserIds,
            Set<String> mappedConsentIds,
            Set<String> mappedSupervisorAssignmentIds,
            Set<String> mappedClientPortalIds,
            Set<String> mappedChecklistTemplateIds,
            Set<String> mappedChecklistItemIds,
            Set<String> mappedClientChecklistIds,
            Set<String> mappedClientChecklistItemIds,
            Set<String> mappedZoomIntegrationIds,
            Set<String> existingChecklistTemplateNames) {
    }

    public record SourceAssessmentsInventory(
            long templateRows,
            long blankTemplateNameRows,
            long sectionRows,
            long blankSectionTemplateRows,
            long blankSectionTitleRows,
            long questionRows,
            long blankQuestionSectionRows,
            long blankQuestionTextRows,
            long blankQuestionTypeRows,
            long optionRows,
            long blankOptionQuestionRows,
            long blankOptionTextRows,
            long assignmentRows,
            long blankAssignmentTemplateRows,
            long blankAssignmentClientRows,
            long responseRows,
            long blankResponseAssignmentRows,
            long blankResponseQuestionRows,
            long reportRows,
            long blankReportAssignmentRows) {
    }

    public record SourceAssessmentTemplateRecord(
            String legacyTemplatePk,
            String name,
            String description,
            String category,
            boolean standardized,
            boolean active,
            String createdByLegacyId,
            String version,
            Instant createdAt,
            Instant updatedAt) {

        boolean missingRequiredFields() {
            return name == null || name.isBlank();
        }
    }

    public record SourceAssessmentSectionRecord(
            String legacySectionPk,
            String templateLegacyId,
            String title,
            String description,
            String accessLevel,
            boolean scoring,
            String reportMapping,
            String aiReportPrompt,
            Integer sortOrder,
            Instant createdAt,
            Instant updatedAt) {

        boolean missingRequiredFields() {
            return templateLegacyId == null || templateLegacyId.isBlank()
                    || title == null || title.isBlank();
        }
    }

    public record SourceAssessmentQuestionRecord(
            String legacyQuestionPk,
            String sectionLegacyId,
            String questionText,
            String questionType,
            boolean required,
            Integer sortOrder,
            Integer ratingMin,
            Integer ratingMax,
            List<String> ratingLabels,
            boolean contributesToScore,
            Instant createdAt,
            Instant updatedAt) {

        boolean missingRequiredFields() {
            return sectionLegacyId == null || sectionLegacyId.isBlank()
                    || questionText == null || questionText.isBlank()
                    || questionType == null || questionType.isBlank();
        }
    }

    public record SourceAssessmentQuestionOptionRecord(
            String legacyOptionPk,
            String questionLegacyId,
            String optionText,
            BigDecimal optionValue,
            Integer sortOrder) {

        boolean missingRequiredFields() {
            return questionLegacyId == null || questionLegacyId.isBlank()
                    || optionText == null || optionText.isBlank();
        }
    }

    public record SourceAssessmentAssignmentRecord(
            String legacyAssignmentPk,
            String templateLegacyId,
            String clientLegacyId,
            String assignedByLegacyId,
            String status,
            LocalDate dueDate,
            Instant completedAt,
            Instant finalizedAt,
            Instant clientSubmittedAt,
            Instant therapistCompletedAt,
            BigDecimal totalScore,
            String notes,
            Instant createdAt,
            Instant updatedAt) {

        boolean missingRequiredFields() {
            return templateLegacyId == null || templateLegacyId.isBlank()
                    || clientLegacyId == null || clientLegacyId.isBlank();
        }
    }

    public record SourceAssessmentResponseRecord(
            String legacyResponsePk,
            String assignmentLegacyId,
            String questionLegacyId,
            String responderLegacyId,
            String responseText,
            List<String> selectedOptionLegacyIds,
            String ratingValue,
            BigDecimal scoreValue,
            Instant createdAt,
            Instant updatedAt) {

        boolean missingRequiredFields() {
            return assignmentLegacyId == null || assignmentLegacyId.isBlank()
                    || questionLegacyId == null || questionLegacyId.isBlank();
        }
    }

    public record SourceAssessmentReportRecord(
            String legacyReportPk,
            String assignmentLegacyId,
            String generatedContent,
            String draftContent,
            String finalContent,
            String reportData,
            boolean draft,
            boolean finalized,
            Instant generatedAt,
            Instant editedAt,
            Instant finalizedAt,
            Instant exportedAt,
            String createdByLegacyId,
            String finalizedByLegacyId,
            Instant createdAt,
            Instant updatedAt) {

        boolean missingRequiredFields() {
            return assignmentLegacyId == null || assignmentLegacyId.isBlank();
        }
    }

    public record AssessmentTargetState(
            Set<String> mappedClientIds,
            Set<String> mappedUserIds,
            Set<String> mappedTemplateIds,
            Set<String> mappedSectionIds,
            Set<String> mappedQuestionIds,
            Set<String> mappedOptionIds,
            Set<String> mappedAssignmentIds,
            Set<String> mappedResponseIds,
            Set<String> mappedReportIds,
            Set<String> existingTemplateNameVersions) {
    }

    public record SourceTranscriptsInventory(
            long transcriptRows,
            long blankSessionRows,
            long blankClientRows,
            long blankTherapistRows) {
    }

    public record SourceTranscriptRecord(
            String legacyTranscriptPk,
            String sessionLegacyId,
            String clientLegacyId,
            String therapistLegacyId,
            String content,
            String rawContent,
            String language,
            boolean translatedToEnglish,
            Integer durationSeconds,
            Integer chunkCount,
            Integer wordCount,
            String status,
            String errorMessage,
            Instant createdAt,
            Instant updatedAt,
            String uploadId) {

        boolean missingRequiredFields() {
            return sessionLegacyId == null || sessionLegacyId.isBlank()
                    || clientLegacyId == null || clientLegacyId.isBlank()
                    || therapistLegacyId == null || therapistLegacyId.isBlank();
        }
    }

    public record TranscriptsTargetState(
            Set<String> mappedSessionIds,
            Set<String> mappedClientIds,
            Set<String> mappedUserIds,
            Set<String> mappedTranscriptIds) {
    }

    public record MappedDocumentBinaryRef(
            String legacyDocumentPk,
            Long targetDocumentId,
            String fileName,
            String originalName,
            String scanStatus,
            Long targetClientId,
            String mimeType) {
    }

    public record DocumentBinariesTargetState(List<MappedDocumentBinaryRef> mappedDocuments) {
    }
}

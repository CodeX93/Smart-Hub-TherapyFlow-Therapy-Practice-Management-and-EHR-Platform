package com.smart.therapy.flow.common;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.entity.Role;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.util.RoleName;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.entity.SessionTranscript;
import com.smart.therapy.flow.session.entity.SessionTranscriptChunk;
import com.smart.therapy.flow.session.enums.SessionTranscriptChunkStatus;
import com.smart.therapy.flow.session.enums.SessionTranscriptStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Factory class for creating test data objects.
 * Used across all test classes to ensure consistent test data.
 * User creation uses AuthIdentity (unified auth); use createAuthPrincipal for services that expect AuthPrincipal.
 */
public class TestDataFactory {

    // ========== Organisation & AuthIdentity ==========

    public static Organisation createTestOrganisation() {
        return Organisation.builder()
                .id(1L)
                .name("Test Organisation")
                .slug("test-org")
                .status("ACTIVE")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();
    }

    public static AuthIdentity createTestAuthIdentity(String loginIdentifier, String passwordHash) {
        String normalised = loginIdentifier != null ? loginIdentifier.toLowerCase().trim() : "";
        boolean looksLikeEmail = loginIdentifier != null && loginIdentifier.contains("@");
        return AuthIdentity.builder()
                .id(1L)
                .loginIdentifier(loginIdentifier)
                .normalisedLoginIdentifier(normalised)
                .email(looksLikeEmail ? loginIdentifier : null)
                .normalisedEmail(looksLikeEmail ? normalised : null)
                .username(looksLikeEmail ? loginIdentifier : loginIdentifier)
                .normalisedUsername(normalised)
                .passwordHash(passwordHash)
                .identityType(IdentityType.STAFF)
                .isActive(true)
                .build();
    }

    // ========== User & Authentication ==========

    public static User createTestUser() {
        AuthIdentity auth = createTestAuthIdentity("testuser@example.com", "$2a$10$encryptedPasswordHash");
        return User.builder()
                .email("testuser@example.com")
                .fullName("Test User")
                .authIdentity(auth)
                .isActive(true)
                .build();
    }

    public static User createTestTherapist() {
        AuthIdentity auth = createTestAuthIdentity("therapist@example.com", "$2a$10$encryptedPasswordHash");
        return User.builder()
                .email("therapist@example.com")
                .fullName("Jane Therapist")
                .authIdentity(auth)
                .isActive(true)
                .build();
    }

    public static User createTestAdmin() {
        AuthIdentity auth = createTestAuthIdentity("admin@example.com", "$2a$10$encryptedPasswordHash");
        return User.builder()
                .email("admin@example.com")
                .fullName("Admin User")
                .authIdentity(auth)
                .isActive(true)
                .build();
    }

    /** Use for services that expect AuthPrincipal (staff). */
    public static AuthPrincipal createAuthPrincipal(User user) {
        return createAuthPrincipal(user, "ROLE_THERAPIST");
    }

    public static AuthPrincipal createAuthPrincipal(User user, String... authorities) {
        if (user == null || user.getAuthIdentity() == null) {
            throw new IllegalArgumentException("User must have auth identity");
        }
        AuthIdentity auth = user.getAuthIdentity();
        List<SimpleGrantedAuthority> granted = Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        return AuthPrincipal.create(auth, granted);
    }

    /** Use for portal tests: AuthPrincipal with identityType CLIENT. */
    public static AuthPrincipal createAuthPrincipalForClient(long authId) {
        AuthIdentity auth = AuthIdentity.builder()
                .id(authId)
                .loginIdentifier("client@example.com")
                .normalisedLoginIdentifier("client@example.com")
                .identityType(IdentityType.CLIENT)
                .isActive(true)
                .build();
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_CLIENT"));
        return AuthPrincipal.create(auth, authorities);
    }

    // ========== Client ==========

    public static Client createTestClient() {
        return Client.builder()
                .clientId("CL-2024-0001")
                .fullName("John Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .status("active")
                .build();
    }

    public static Client createTestClient(User therapist) {
        Client client = createTestClient();
        client.setAssignedTherapist(therapist);
        return client;
    }

    public static Client createTestClientWithId(Long id) {
        Client client = createTestClient();
        client.setId(id);
        return client;
    }

    // ========== Session ==========

    public static Session createTestSession() {
        return Session.builder()
                .sessionDate(Instant.now().plus(1, ChronoUnit.DAYS))
                .duration(60)
                .sessionType(com.smart.therapy.flow.session.enums.SessionType.IN_PERSON.getValue())
                .clinicalSessionType("psychotherapy")
                .status(com.smart.therapy.flow.session.enums.SessionStatus.SCHEDULED.getValue())
                .build();
    }

    public static Session createTestSession(Client client, User therapist) {
        Session session = createTestSession();
        session.setClient(client);
        session.setTherapist(therapist);
        return session;
    }

    public static Session createTestSessionWithId(Long id) {
        Session session = createTestSession();
        session.setId(id);
        return session;
    }

    // ========== Role ==========

    public static Role createTestRole(RoleName roleName) {
        return Role.builder()
                .name(roleName.name())
                .displayName(roleName.name())
                .description("Test " + roleName.name() + " role")
                .isActive(true)
                .build();
    }

    // ========== Session transcript ==========

    public static SessionTranscript createTestSessionTranscript(Session session, Client client, User uploader) {
        return SessionTranscript.builder()
                .session(session)
                .client(client)
                .uploader(uploader)
                .uploadId("srv-testupload000000000000000001")
                .status(SessionTranscriptStatus.RECORDING)
                .language("auto")
                .translatedToEnglish(false)
                .receivedChunks(0)
                .expiresAt(Instant.now().plusSeconds(86400))
                .build();
    }

    public static SessionTranscriptChunk createTestSessionTranscriptChunk(
            SessionTranscript transcript, int chunkIndex, String chunkText) {
        return SessionTranscriptChunk.builder()
                .transcript(transcript)
                .chunkIndex(chunkIndex)
                .chunkStatus(chunkText != null && !chunkText.isBlank()
                        ? SessionTranscriptChunkStatus.RECEIVED
                        : SessionTranscriptChunkStatus.SILENT)
                .chunkText(chunkText)
                .chunkDurationSeconds(20.0)
                .receivedAt(Instant.now())
                .build();
    }
}

package com.smart.therapy.flow.integration.repository;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.BaseTenantRepositoryTest;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.enums.SessionStatus;
import com.smart.therapy.flow.session.enums.SessionType;
import com.smart.therapy.flow.session.repository.SessionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SessionRepository Integration Tests")
class SessionRepositoryIntegrationTest extends BaseTenantRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private UserRepository userRepository;

    private com.smart.therapy.flow.billing.entity.Service service;
    private User therapist;
    private Client client;
    private Session session1;
    private Session session2;

    @BeforeEach
    void setUp() {
        therapist = persistTherapist();
        service = entityManager.persistAndFlush(com.smart.therapy.flow.billing.entity.Service.builder()
                .serviceCode("fixture-consultation").serviceName("Fixture consultation").duration(60)
                .baseRate(BigDecimal.valueOf(100)).build());

        client = TestDataFactory.createTestClient(therapist);
        client = clientRepository.save(client);

        session1 = Session.builder()
                .client(client)
                .therapist(therapist)
                .service(service)
                .sessionDate(Instant.now().plusSeconds(86400))
                .duration(60)
                .status(SessionStatus.SCHEDULED.getValue())
                .sessionType(SessionType.IN_PERSON.getValue())
                .clinicalSessionType("psychotherapy")
                .build();
        entityManager.persistAndFlush(session1);

        session2 = Session.builder()
                .client(client)
                .therapist(therapist)
                .service(service)
                .sessionDate(Instant.now().plusSeconds(172800))
                .duration(90)
                .status(SessionStatus.SCHEDULED.getValue())
                .sessionType(SessionType.IN_PERSON.getValue())
                .clinicalSessionType("psychotherapy")
                .build();
        entityManager.persistAndFlush(session2);
    }

    @Test
    @DisplayName("Should find sessions by client ID")
    void shouldFindSessionsByClientId() {
        // Act
        List<Session> sessions = sessionRepository.findByClientId(client.getId());

        // Assert
        assertThat(sessions).isNotNull();
        assertThat(sessions).hasSize(2);
        assertThat(sessions).extracting(Session::getId)
                .containsExactlyInAnyOrder(session1.getId(), session2.getId());
    }

    @Test
    @DisplayName("Should find sessions by therapist ID")
    void shouldFindSessionsByTherapistId() {
        // Act
        List<Session> sessions = sessionRepository.findByTherapistId(therapist.getId());

        // Assert
        assertThat(sessions).isNotNull();
        assertThat(sessions).hasSize(2);
    }

    @Test
    @DisplayName("Should find sessions by status")
    void shouldFindSessionsByStatus() {
        // Act
        List<Session> sessions = sessionRepository.findByStatus("scheduled");

        // Assert
        assertThat(sessions).isNotNull();
        assertThat(sessions).hasSize(2);
        assertThat(sessions).allMatch(s -> "scheduled".equals(s.getStatus()));
    }

    @Test
    @DisplayName("Should save and retrieve session")
    void shouldSaveAndRetrieveSession() {
        // Arrange
        Session newSession = Session.builder()
                .client(client)
                .therapist(therapist)
                .service(service)
                .sessionDate(Instant.now().plusSeconds(259200))
                .duration(45)
                .status(SessionStatus.SCHEDULED.getValue())
                .sessionType(SessionType.IN_PERSON.getValue())
                .clinicalSessionType("psychotherapy")
                .build();

        // Act
        Session saved = sessionRepository.save(newSession);
        entityManager.flush();
        entityManager.clear();

        Session retrieved = sessionRepository.findById(saved.getId()).orElse(null);

        // Assert
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getDuration()).isEqualTo(45);
        assertThat(retrieved.getStatus()).isEqualTo("scheduled");
    }
}

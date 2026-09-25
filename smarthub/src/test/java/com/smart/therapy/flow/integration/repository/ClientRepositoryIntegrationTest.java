package com.smart.therapy.flow.integration.repository;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientContact;
import com.smart.therapy.flow.client.enums.ContactType;
import com.smart.therapy.flow.client.repository.ClientContactRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.BaseTenantRepositoryTest;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.service.BlindIndexService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClientRepository Integration Tests")
class ClientRepositoryIntegrationTest extends BaseTenantRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientContactRepository contactRepository;
    @Autowired
    private BlindIndexService blindIndexService;

    private byte[] emailIndex(String email) {
        return blindIndexService.compute(BlindIndexService.Kind.CONTACT,
                blindIndexService.normalizeEmail(email));
    }

    private void addEmail(Client client, String email) {
        var contact = ClientContact.builder()
                .client(client).contactType(ContactType.EMAIL)
                .contactValue(email).contactBlindIdx(emailIndex(email)).isPrimary(true).build();
        client.getContacts().add(contact);
        entityManager.persistAndFlush(contact);
    }

    private User therapist;
    private Client client1;
    private Client client2;

    @BeforeEach
    void setUp() {
        therapist = persistTherapist();

        client1 = TestDataFactory.createTestClient(therapist);
        client1.setClientId("CL-001");
        client1.setClientIdBlindIdx(blindIndexService.compute(BlindIndexService.Kind.CLIENT_ID, blindIndexService.normalizeMrn("CL-001")));
        entityManager.persistAndFlush(client1);

        client2 = TestDataFactory.createTestClient(therapist);
        client2.setClientId("CL-002");
        entityManager.persistAndFlush(client2);
        addEmail(client1, "client1@example.com");
        addEmail(client2, "client2@example.com");
    }

    @Test
    @DisplayName("Should find clients by therapist ID")
    void shouldFindClientsByTherapistId() {
        // Act
        List<Client> clients = clientRepository.findByAssignedTherapistId(therapist.getId());

        // Assert
        assertThat(clients).isNotNull();
        assertThat(clients).hasSize(2);
        assertThat(clients).extracting(Client::getId)
                .containsExactlyInAnyOrder(client1.getId(), client2.getId());
    }

    @Test
    @DisplayName("Should find client by normalized email blind index")
    void shouldFindClientByEmail() {
        var contacts = contactRepository.findByContactBlindIdx(emailIndex("CLIENT1@example.com"));
        assertThat(contacts).hasSize(1);
        assertThat(contacts.get(0).getClient().getId()).isEqualTo(client1.getId());
        assertThat(contacts.get(0).getContactValue()).isEqualTo("client1@example.com");
    }

    @Test
    @DisplayName("Should return empty when client not found by email")
    void shouldReturnEmptyWhenClientNotFoundByEmail() {
        // Act
        var client = contactRepository.findByContactBlindIdx(emailIndex("nonexistent@example.com"));

        // Assert
        assertThat(client).isEmpty();
    }

    @Test
    @DisplayName("Should find client by client ID blind index")
    void shouldFindClientByClientId() {
        // Act
        Optional<Client> client = clientRepository.findByClientIdBlindIdxAndIsDeletedFalse(blindIndexService.compute(BlindIndexService.Kind.CLIENT_ID, blindIndexService.normalizeMrn("CL-001")));

        // Assert
        assertThat(client).isPresent();
        assertThat(client.get().getClientId()).isEqualTo("CL-001");
    }

    @Test
    @DisplayName("Should check if email exists")
    void shouldCheckIfEmailExists() {
        // Act
        boolean exists = contactRepository.existsByClientIdAndContactTypeAndContactBlindIdx(client1.getId(), ContactType.EMAIL, emailIndex("client1@example.com"));

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when email does not exist")
    void shouldReturnFalseWhenEmailDoesNotExist() {
        // Act
        boolean exists = contactRepository.existsByClientIdAndContactTypeAndContactBlindIdx(client1.getId(), ContactType.EMAIL, emailIndex("nonexistent@example.com"));

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should save and retrieve client")
    void shouldSaveAndRetrieveClient() {
        // Arrange
        Client newClient = TestDataFactory.createTestClient();
        newClient.setClientId("CL-003");

        // Act
        Client saved = clientRepository.save(newClient);
        entityManager.flush();
        entityManager.clear();

        Optional<Client> retrieved = clientRepository.findById(saved.getId());

        // Assert
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getClientId()).isEqualTo("CL-003");
    }

    @Test
    @DisplayName("Should update client")
    void shouldUpdateClient() {
        // Arrange
        client1.setFullName("Updated Name");

        // Act
        Client updated = clientRepository.save(client1);
        entityManager.flush();
        entityManager.clear();

        Optional<Client> retrieved = clientRepository.findById(updated.getId());

        // Assert
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getFullName()).isEqualTo("Updated Name");
    }

    @Test
    @DisplayName("Should delete client")
    void shouldDeleteClient() {
        // Act
        clientRepository.delete(client1);
        entityManager.flush();
        entityManager.clear();

        Optional<Client> deleted = clientRepository.findById(client1.getId());

        // Assert
        assertThat(deleted).isEmpty();
    }
}

package com.smart.therapy.flow.unit.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubIdentifier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientHubIdentifierTest {

    @Test
    void quotesSafePostgresIdentifiers() {
        assertThat(ClientHubIdentifier.qualified("public", "sessionBilling"))
                .isEqualTo("\"public\".\"sessionBilling\"");
    }

    @Test
    void rejectsUnsafePostgresIdentifiers() {
        assertThatThrownBy(() -> ClientHubIdentifier.qualified("public", "clients; DROP TABLE clients"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsafe PostgreSQL identifier");
    }

    @Test
    void generatesDeterministicSha256Hex() {
        assertThat(ClientHubIdentifier.sha256Hex("clients|id|integer|NO\n"))
                .isEqualTo(ClientHubIdentifier.sha256Hex("clients|id|integer|NO\n"))
                .hasSize(64);
    }
}

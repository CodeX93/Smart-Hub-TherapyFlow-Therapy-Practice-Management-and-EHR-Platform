package com.smart.therapy.flow.client.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

/**
 * HMAC blind-index digests for whitespace-separated tokens of {@link Client#getFullName()}.
 * Enables first/last-name exact-match search without LIKE on encrypted PHI columns.
 */
@Entity
@Table(name = "client_name_blind_indexes", indexes = {
        @Index(name = "idx_client_name_token_blind", columnList = "token_blind_idx"),
        @Index(name = "idx_client_name_token_client", columnList = "client_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_client_name_token_ord", columnNames = {"client_id", "token_ord"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientNameBlindIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIgnore
    private Client client;

    @Column(name = "token_blind_idx", nullable = false, columnDefinition = "BYTEA")
    private byte[] tokenBlindIdx;

    @Column(name = "token_ord", nullable = false)
    private int tokenOrd;
}

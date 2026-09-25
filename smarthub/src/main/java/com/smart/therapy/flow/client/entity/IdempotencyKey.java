package com.smart.therapy.flow.client.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Entity for tracking idempotency keys to prevent duplicate client creation.
 * Used to ensure that the same request (identified by idempotency key)
 * cannot create multiple clients, even in concurrent scenarios.
 * Tenant schema: unique per schema (idempotency_key).
 */
@Entity
@Table(name = "idempotency_keys", indexes = {
    @Index(name = "idx_idempotency_key", columnList = "idempotency_key"),
    @Index(name = "idx_idempotency_client", columnList = "client_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class IdempotencyKey extends BaseEntity {

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String key;

    @Column(name = "client_id")
    private Long clientId;
}

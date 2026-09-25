package com.smart.therapy.flow.user.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Entity for tracking idempotency keys to prevent duplicate user creation.
 * Used to ensure that the same request (identified by idempotency key)
 * cannot create multiple users, even in concurrent scenarios.
 * Tenant schema: unique per schema (idempotency_key).
 */
@Entity
@Table(name = "user_idempotency_keys", indexes = {
    @Index(name = "idx_user_idempotency_key", columnList = "idempotency_key"),
    @Index(name = "idx_user_idempotency_user", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class UserIdempotencyKey extends BaseEntity {

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String key;

    @Column(name = "user_id")
    private Long userId;
}

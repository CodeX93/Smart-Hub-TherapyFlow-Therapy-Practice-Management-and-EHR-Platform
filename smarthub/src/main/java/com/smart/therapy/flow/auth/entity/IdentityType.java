package com.smart.therapy.flow.auth.entity;

/**
 * Type of authentication identity. Security layer must know type without querying users/clients.
 */
public enum IdentityType {
    STAFF,
    CLIENT,
    API,
    SYSTEM,
    SSO
}

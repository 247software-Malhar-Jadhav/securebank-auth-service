package com.securebank.auth.entity;

/**
 * Application roles. Stored as a string in the DB ({@code @Enumerated(EnumType.STRING)})
 * so adding a role never shifts ordinals and the column stays human-readable.
 *
 * <p>Spring Security convention is to prefix authorities with {@code ROLE_}; we add that
 * prefix when building the JWT / GrantedAuthority, not here, to keep the enum clean.
 */
public enum Role {
    /** Full administrative access. */
    ADMIN,
    /** Regular banking customer. */
    CUSTOMER
}

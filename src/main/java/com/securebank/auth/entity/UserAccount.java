package com.securebank.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * The persistent user/identity record. Maps to the {@code users} table created in
 * Flyway V1.
 *
 * <p>Named {@code UserAccount} (not {@code User}) to avoid clashing with Spring
 * Security's {@code org.springframework.security.core.userdetails.User}.
 *
 * <p>Design notes:
 * <ul>
 *   <li><b>Entity, never a DTO</b> — per spec, DTOs ≠ entities. This class never
 *       crosses the HTTP/gRPC boundary; mappers convert it to DTOs/proto messages.</li>
 *   <li><b>Optimistic locking</b> via {@link Version}: two concurrent failed logins
 *       can't silently lose an increment to the failed-attempt counter.</li>
 *   <li>Lockout state ({@code failedAttempts}, {@code lockedUntil}) lives on the row;
 *       see {@code AuthService} for the policy that mutates it.</li>
 * </ul>
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /** BCrypt hash of the password — the raw password is never stored. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    /** When non-null and in the future, the account is locked out. */
    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "preferred_locale", nullable = false, length = 8)
    private String preferredLocale;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * True if the account is currently locked (locked_until set and not yet elapsed).
     * Domain logic kept on the entity (a touch of rich-domain-model) so callers don't
     * repeat the time comparison.
     */
    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }
}

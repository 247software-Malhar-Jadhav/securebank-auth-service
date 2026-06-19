package com.securebank.auth.dto;

import com.securebank.auth.entity.Role;
import com.securebank.auth.entity.UserAccount;

/**
 * Public view of a user returned by /api/auth/register. Never exposes the password
 * hash or internal lockout counters — DTOs let us choose exactly what leaves the
 * service.
 */
public record UserResponse(
        Long id,
        String username,
        String email,
        Role role,
        String preferredLocale
) {
    /** Mapper from entity -> DTO (keeps controllers thin). */
    public static UserResponse from(UserAccount u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail(),
                u.getRole(), u.getPreferredLocale());
    }
}

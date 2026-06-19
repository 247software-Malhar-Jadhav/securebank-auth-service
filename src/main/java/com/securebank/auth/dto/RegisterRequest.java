package com.securebank.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/auth/register.
 *
 * <p>A Java {@code record} = immutable DTO. Bean Validation annotations enforce input
 * rules at the edge; failures become RFC-7807 problem responses (see the exception
 * handler). DTOs are deliberately separate from the {@code UserAccount} entity.
 *
 * @param username       3-64 chars, unique
 * @param email          valid email, unique
 * @param password       raw password (BCrypt-hashed before storage); min 8 chars
 * @param preferredLocale one of en | hi | mr (defaults handled in service if blank)
 */
public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 64) String username,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @Pattern(regexp = "en|hi|mr", message = "{validation.locale.invalid}") String preferredLocale
) {
}

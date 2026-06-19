package com.securebank.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /api/auth/login. Immutable DTO.
 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}

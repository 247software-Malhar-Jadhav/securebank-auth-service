package com.securebank.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for POST /api/auth/refresh. Client exchanges a still-valid refresh
 * token for a fresh access (and refresh) token pair.
 */
public record RefreshRequest(
        @NotBlank String refreshToken
) {
}

package com.securebank.auth.dto;

/**
 * Response for login / refresh. Carries the two-token pair plus a little metadata so
 * clients don't have to decode the JWT to learn its lifetime.
 *
 * @param accessToken  short-lived JWT used on every API call
 * @param refreshToken longer-lived JWT used only against /api/auth/refresh
 * @param tokenType    always "Bearer"
 * @param expiresIn    access-token lifetime in seconds
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
    public static TokenResponse bearer(String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }
}

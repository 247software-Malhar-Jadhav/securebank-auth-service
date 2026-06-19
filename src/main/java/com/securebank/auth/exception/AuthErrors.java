package com.securebank.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Factory of the concrete {@link AuthException}s this service raises. Centralising them
 * keeps message keys consistent with the i18n bundles and documents the failure modes
 * in one place. Pattern: <b>static factory methods</b>.
 */
public final class AuthErrors {

    private AuthErrors() {
    }

    /** Username or email already taken at registration time. */
    public static AuthException duplicateUser() {
        return new AuthException(HttpStatus.CONFLICT, "error.user.duplicate");
    }

    /** Wrong username or password. Deliberately vague to avoid user enumeration. */
    public static AuthException badCredentials() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "error.auth.bad_credentials");
    }

    /** Account temporarily locked after too many failed attempts. */
    public static AuthException accountLocked() {
        return new AuthException(HttpStatus.LOCKED, "error.auth.account_locked");
    }

    /** Account exists but is disabled by an admin. */
    public static AuthException accountDisabled() {
        return new AuthException(HttpStatus.FORBIDDEN, "error.auth.account_disabled");
    }

    /** Refresh token missing/expired/forged or wrong token type. */
    public static AuthException invalidRefreshToken() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "error.auth.invalid_refresh");
    }

    /** Referenced user id not found (e.g. token valid but user deleted). */
    public static AuthException userNotFound() {
        return new AuthException(HttpStatus.NOT_FOUND, "error.user.not_found");
    }
}

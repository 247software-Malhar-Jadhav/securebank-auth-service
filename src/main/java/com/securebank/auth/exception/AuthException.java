package com.securebank.auth.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base type for auth-domain failures that map cleanly onto HTTP problem responses.
 *
 * <p>Each carries: an HTTP {@link HttpStatus}, a stable {@code messageKey} that the
 * exception handler resolves through {@code MessageSource} (so the human-readable
 * message is localised to en/hi/mr), and optional message arguments.
 *
 * <p>Pattern: a small exception hierarchy keeps the controller/service code free of
 * HTTP details — they throw meaning, the handler decides presentation.
 */
@Getter
public class AuthException extends RuntimeException {

    private final HttpStatus status;
    private final String messageKey;
    private final transient Object[] args;

    public AuthException(HttpStatus status, String messageKey, Object... args) {
        super(messageKey);
        this.status = status;
        this.messageKey = messageKey;
        this.args = args;
    }
}

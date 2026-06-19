package com.securebank.auth.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

/**
 * Translates exceptions into RFC-7807 "problem+json" responses.
 *
 * <p>Spring's {@link ProblemDetail} is the built-in RFC-7807 carrier; we populate it
 * and let Spring serialise it as {@code application/problem+json}.
 *
 * <p>All human-readable {@code detail} strings are resolved through
 * {@link MessageSource} using the request's locale (resolved from the
 * {@code Accept-Language} header), giving en/hi/mr localisation for free.
 *
 * <p>Pattern: <b>centralised exception handling</b> via {@code @RestControllerAdvice}
 * keeps controllers free of try/catch and guarantees a uniform error shape.
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private static final String PROBLEM_BASE = "https://securebank.local/problems/";

    private final MessageSource messageSource;

    /** Domain auth failures carry their own status + localised message key. */
    @ExceptionHandler(AuthException.class)
    public ProblemDetail handleAuth(AuthException ex, HttpServletRequest req) {
        String detail = messageSource.getMessage(
                ex.getMessageKey(), ex.getArgs(), ex.getMessageKey(),
                LocaleContextHolder.getLocale());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.getStatus(), detail);
        pd.setTitle(ex.getStatus().getReasonPhrase());
        pd.setType(URI.create(PROBLEM_BASE + ex.getMessageKey().replace('.', '-')));
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    /** Bean-validation failures (e.g. blank username) -> 400 with field errors. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex,
                                          HttpServletRequest req) {
        String fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        String detail = messageSource.getMessage(
                "error.validation", null, "Validation failed",
                LocaleContextHolder.getLocale());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                detail + " (" + fields + ")");
        pd.setTitle("Bad Request");
        pd.setType(URI.create(PROBLEM_BASE + "validation"));
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    /** Anything unexpected -> 500 without leaking internals. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {}", req.getRequestURI(), ex);
        String detail = messageSource.getMessage(
                "error.internal", null, "Internal server error",
                LocaleContextHolder.getLocale());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, detail);
        pd.setTitle("Internal Server Error");
        pd.setType(URI.create(PROBLEM_BASE + "internal"));
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }
}

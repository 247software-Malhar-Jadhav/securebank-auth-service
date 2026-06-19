package com.securebank.auth.service;

import com.securebank.auth.dto.LoginRequest;
import com.securebank.auth.dto.RefreshRequest;
import com.securebank.auth.dto.RegisterRequest;
import com.securebank.auth.dto.TokenResponse;
import com.securebank.auth.dto.UserResponse;
import com.securebank.auth.entity.Role;
import com.securebank.auth.entity.UserAccount;
import com.securebank.auth.exception.AuthErrors;
import com.securebank.auth.repository.UserAccountRepository;
import com.securebank.auth.security.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Core identity use-cases: register, login (with lockout), refresh.
 *
 * <p>Pattern: <b>application service</b> — orchestrates the repository, password
 * encoder and JWT service, and owns the transaction boundary. Controllers stay thin
 * and free of business rules.
 *
 * <p><b>Account-lockout policy</b> (spec): after {@value #MAX_FAILED_ATTEMPTS}
 * consecutive failed logins the account is locked for {@value #LOCK_MINUTES} minutes.
 * A successful login resets the counter. Lockout state is persisted on the user row and
 * guarded by JPA optimistic locking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    /** Lock after this many consecutive failures. */
    static final int MAX_FAILED_ATTEMPTS = 5;
    /** How long the lock lasts. */
    static final long LOCK_MINUTES = 15;
    /** Default UI locale when the client doesn't specify one. */
    private static final String DEFAULT_LOCALE = "en";

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Create a new CUSTOMER account. Self-service registration never yields ADMIN.
     */
    @Transactional
    public UserResponse register(RegisterRequest req) {
        // Fail fast on duplicates before hashing (BCrypt is intentionally slow).
        if (users.existsByUsername(req.username()) || users.existsByEmail(req.email())) {
            throw AuthErrors.duplicateUser();
        }
        UserAccount user = UserAccount.builder()
                .username(req.username())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password())) // BCrypt hash
                .role(Role.CUSTOMER)
                .enabled(true)
                .failedAttempts(0)
                .preferredLocale(StringUtils.hasText(req.preferredLocale())
                        ? req.preferredLocale() : DEFAULT_LOCALE)
                .build();
        user = users.save(user);
        log.info("Registered new user '{}' (id={})", user.getUsername(), user.getId());
        return UserResponse.from(user);
    }

    /**
     * Authenticate and return an access+refresh token pair, applying the lockout policy.
     */
    @Transactional
    public TokenResponse login(LoginRequest req) {
        // Vague "bad credentials" on unknown user prevents username enumeration.
        UserAccount user = users.findByUsername(req.username())
                .orElseThrow(AuthErrors::badCredentials);

        if (user.isLocked()) {
            throw AuthErrors.accountLocked();
        }
        if (!user.isEnabled()) {
            throw AuthErrors.accountDisabled();
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            registerFailedAttempt(user);
            throw AuthErrors.badCredentials();
        }

        // Success: clear any accrued failure state.
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        users.save(user);

        return issuePair(user);
    }

    /**
     * Exchange a valid refresh token for a fresh token pair (token rotation).
     */
    @Transactional(readOnly = true)
    public TokenResponse refresh(RefreshRequest req) {
        Claims claims = jwtService.verify(req.refreshToken())
                .orElseThrow(AuthErrors::invalidRefreshToken);

        // Must actually be a refresh token, not an access token replayed here.
        if (!JwtService.TYPE_REFRESH.equals(claims.get(JwtService.CLAIM_TYPE, String.class))) {
            throw AuthErrors.invalidRefreshToken();
        }

        Long userId = Long.valueOf(claims.getSubject());
        UserAccount user = users.findById(userId)
                .orElseThrow(AuthErrors::invalidRefreshToken);
        if (!user.isEnabled() || user.isLocked()) {
            throw AuthErrors.invalidRefreshToken();
        }
        return issuePair(user);
    }

    // ---- helpers -----------------------------------------------------------

    private TokenResponse issuePair(UserAccount user) {
        String access = jwtService.issueAccessToken(user);
        String refresh = jwtService.issueRefreshToken(user);
        return TokenResponse.bearer(access, refresh, jwtService.accessTtlSeconds());
    }

    private void registerFailedAttempt(UserAccount user) {
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plus(LOCK_MINUTES, ChronoUnit.MINUTES));
            log.warn("Account '{}' locked after {} failed attempts", user.getUsername(), attempts);
        }
        users.save(user);
    }
}

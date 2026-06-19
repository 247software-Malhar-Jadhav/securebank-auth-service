package com.securebank.auth.security;

import com.securebank.auth.config.JwtProperties;
import com.securebank.auth.entity.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Central JWT mint/verify component built on jjwt (io.jsonwebtoken).
 *
 * <p><b>This is the single place that knows the signing key.</b> That is the whole
 * point of the architecture: the REST side mints tokens here, and the gRPC
 * {@code ValidateToken} RPC verifies them here. No other service ever sees the secret —
 * they call auth-service over gRPC instead. (Trusted internal authority pattern.)
 *
 * <p>Tokens are signed with HMAC-SHA256 ("HS256"). The {@code iss} claim is fixed to
 * "securebank"; we also embed {@code roles} and {@code locale} so the gateway can
 * forward identity downstream without a DB hit.
 */
@Component
@Slf4j
public class JwtService {

    /** Custom claim names kept as constants so mint & verify can't drift apart. */
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_LOCALE = "locale";
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_TYPE = "type"; // "access" | "refresh"

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final JwtProperties props;
    private final SecretKey signingKey;

    public JwtService(JwtProperties props) {
        this.props = props;
        // Derive an HMAC key from the configured secret. Keys.hmacShaKeyFor enforces a
        // minimum key length, surfacing a weak-secret misconfiguration at startup.
        this.signingKey = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    /** Mint a short-lived access token carrying roles + preferred locale. */
    public String issueAccessToken(UserAccount user) {
        return buildToken(user, TYPE_ACCESS, props.accessTtlSeconds());
    }

    /** Mint a longer-lived refresh token (minimal claims — it only buys new access tokens). */
    public String issueRefreshToken(UserAccount user) {
        return buildToken(user, TYPE_REFRESH, props.refreshTtlSeconds());
    }

    private String buildToken(UserAccount user, String type, long ttlSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(props.issuer())                       // iss = "securebank"
                .subject(String.valueOf(user.getId()))        // sub = user id
                .claim(CLAIM_USERNAME, user.getUsername())
                .claim(CLAIM_ROLES, List.of("ROLE_" + user.getRole().name()))
                .claim(CLAIM_LOCALE, user.getPreferredLocale())
                .claim(CLAIM_TYPE, type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parse + verify a token's signature, issuer and expiry. Returns the claims when
     * valid, or empty when the token is forged/expired/malformed. Never throws to the
     * caller — verification failure is an expected outcome, not an error.
     */
    public Optional<Claims> verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(props.issuer())  // reject tokens not minted by us
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            // Expected for expired/forged tokens — log at debug, return empty.
            log.debug("JWT verification failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> rolesOf(Claims claims) {
        Object roles = claims.get(CLAIM_ROLES);
        return roles instanceof List<?> list ? (List<String>) list : List.of();
    }

    public long accessTtlSeconds() {
        return props.accessTtlSeconds();
    }
}

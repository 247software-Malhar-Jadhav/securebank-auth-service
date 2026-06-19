package com.securebank.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed JWT configuration, bound from {@code securebank.jwt.*} in
 * application.yml. Pattern: <b>externalised configuration</b> — secrets/timeouts live
 * outside code so they differ per environment without recompiling.
 *
 * <p>The signing {@code secret} comes from the {@code SECUREBANK_JWT_SECRET} env var
 * (wired in application.yml). It must be long enough for HMAC-SHA256 (>= 32 bytes).
 *
 * @param issuer            the "iss" claim, fixed to "securebank"
 * @param secret            HMAC signing key (Base64 or raw; see JwtService)
 * @param accessTtlSeconds  access-token lifetime
 * @param refreshTtlSeconds refresh-token lifetime
 */
@ConfigurationProperties(prefix = "securebank.jwt")
public record JwtProperties(
        String issuer,
        String secret,
        long accessTtlSeconds,
        long refreshTtlSeconds
) {
}

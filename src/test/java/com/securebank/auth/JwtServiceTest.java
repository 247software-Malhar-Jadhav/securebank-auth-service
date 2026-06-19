package com.securebank.auth;

import com.securebank.auth.config.JwtProperties;
import com.securebank.auth.entity.Role;
import com.securebank.auth.entity.UserAccount;
import com.securebank.auth.security.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests for {@link JwtService} — no Spring context, no database, so they run
 * fast in CI. They prove the mint/verify round-trip and that tampering is rejected,
 * which is exactly what the gRPC ValidateToken path relies on.
 */
class JwtServiceTest {

    private final JwtService jwt = new JwtService(new JwtProperties(
            "securebank",
            "test-secret-test-secret-test-secret-0123456789", // >= 32 bytes for HS256
            900, 604800));

    private UserAccount sampleUser() {
        return UserAccount.builder()
                .id(42L).username("jsmith").email("jsmith@securebank.local")
                .role(Role.CUSTOMER).enabled(true).preferredLocale("hi")
                .passwordHash("x").build();
    }

    @Test
    void accessToken_roundTrips_withClaims() {
        String token = jwt.issueAccessToken(sampleUser());

        Optional<Claims> verified = jwt.verify(token);
        assertTrue(verified.isPresent(), "freshly issued token must verify");
        Claims c = verified.get();
        assertEquals("42", c.getSubject());
        assertEquals("jsmith", c.get(JwtService.CLAIM_USERNAME, String.class));
        assertEquals("hi", c.get(JwtService.CLAIM_LOCALE, String.class));
        assertEquals("securebank", c.getIssuer());
        assertTrue(jwt.rolesOf(c).contains("ROLE_CUSTOMER"));
        assertEquals(JwtService.TYPE_ACCESS, c.get(JwtService.CLAIM_TYPE, String.class));
    }

    @Test
    void refreshToken_isTaggedAsRefresh() {
        String token = jwt.issueRefreshToken(sampleUser());
        Claims c = jwt.verify(token).orElseThrow();
        assertEquals(JwtService.TYPE_REFRESH, c.get(JwtService.CLAIM_TYPE, String.class));
    }

    @Test
    void tamperedToken_isRejected() {
        String token = jwt.issueAccessToken(sampleUser());
        // Tamper the payload segment (header.PAYLOAD.signature). Flipping a char in the
        // payload changes the signed content, so the original signature no longer
        // matches and verification must fail. (Flipping the trailing signature char is
        // unreliable because base64url padding bits can decode to the same bytes.)
        String[] parts = token.split("\\.");
        char c = parts[1].charAt(0);
        parts[1] = (c == 'A' ? 'B' : 'A') + parts[1].substring(1);
        String tampered = parts[0] + "." + parts[1] + "." + parts[2];
        assertFalse(jwt.verify(tampered).isPresent(), "tampered token must not verify");
    }

    @Test
    void tokenFromDifferentKey_isRejected() {
        JwtService other = new JwtService(new JwtProperties(
                "securebank", "another-secret-another-secret-another-0123", 900, 604800));
        String foreign = other.issueAccessToken(sampleUser());
        assertFalse(jwt.verify(foreign).isPresent(), "token signed by another key must fail");
    }
}

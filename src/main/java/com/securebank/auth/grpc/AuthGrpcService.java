package com.securebank.auth.grpc;

import com.securebank.auth.entity.UserAccount;
import com.securebank.auth.repository.UserAccountRepository;
import com.securebank.auth.security.JwtService;
import com.securebank.contracts.auth.v1.AuthServiceGrpc;
import com.securebank.contracts.auth.v1.TokenClaims;
import com.securebank.contracts.auth.v1.TokenRequest;
import com.securebank.contracts.auth.v1.UserIdRequest;
import com.securebank.contracts.auth.v1.UserInfo;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Optional;

/**
 * gRPC server-side implementation of the platform-wide {@code AuthService} contract
 * (generated from the vendored {@code auth.proto}).
 *
 * <p><b>Why this exists — the "trusted internal authority" pattern.</b>
 * auth-service is the ONLY component that holds the JWT signing key. Instead of sharing
 * that secret with the gateway and every downstream service (which would multiply the
 * blast radius of a leak and force every service to re-implement JWT parsing), those
 * components call {@code ValidateToken} over gRPC. auth-service verifies the signature,
 * issuer and expiry locally and returns a small, already-decoded {@link TokenClaims}
 * object. The gateway then forwards {@code X-User-Id}/{@code X-Roles} downstream on the
 * trusted internal network. One place to verify, one place to rotate keys.
 *
 * <p>{@code @GrpcService} (net.devh starter) registers this bean on the gRPC server
 * configured at port 9091 in application.yml.
 *
 * <p>Pattern: <b>Adapter</b> — translates between the gRPC/proto world and the domain
 * (JwtService + repository). It contains no business rules of its own.
 */
@GrpcService
@RequiredArgsConstructor
@Slf4j
public class AuthGrpcService extends AuthServiceGrpc.AuthServiceImplBase {

    private final JwtService jwtService;
    private final UserAccountRepository users;

    /**
     * Verify a JWT this service issued and return its decoded claims.
     *
     * <p>Note: an invalid/expired/forged token is NOT a gRPC error — it is a normal,
     * expected result. We respond with {@code valid=false} and empty fields so the
     * caller can cleanly reject the request. Only genuinely exceptional conditions
     * would map to a gRPC {@link Status}.
     */
    @Override
    public void validateToken(TokenRequest request, StreamObserver<TokenClaims> responseObserver) {
        Optional<Claims> verified = jwtService.verify(request.getAccessToken());

        TokenClaims.Builder reply = TokenClaims.newBuilder();
        if (verified.isEmpty()) {
            reply.setValid(false); // forged / expired / wrong issuer
        } else {
            Claims c = verified.get();
            reply.setValid(true)
                    .setUserId(c.getSubject())
                    .setUsername(c.get(JwtService.CLAIM_USERNAME, String.class))
                    .addAllRoles(jwtService.rolesOf(c));
            String locale = c.get(JwtService.CLAIM_LOCALE, String.class);
            if (locale != null) {
                reply.setPreferredLocale(locale);
            }
        }
        responseObserver.onNext(reply.build());
        responseObserver.onCompleted();
    }

    /**
     * Look up basic user info by id. Used by services that only persist a user id and
     * occasionally need the username/email/roles (e.g. to render or notify).
     *
     * <p>A missing user maps to gRPC {@code NOT_FOUND} — here the absence really is an
     * error for the caller (unlike an invalid token).
     */
    @Override
    public void getUser(UserIdRequest request, StreamObserver<UserInfo> responseObserver) {
        final Long id;
        try {
            id = Long.valueOf(request.getUserId());
        } catch (NumberFormatException ex) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("user_id must be numeric").asRuntimeException());
            return;
        }

        Optional<UserAccount> found = users.findById(id);
        if (found.isEmpty()) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("user not found: " + id).asRuntimeException());
            return;
        }

        UserAccount u = found.get();
        UserInfo info = UserInfo.newBuilder()
                .setUserId(String.valueOf(u.getId()))
                .setUsername(u.getUsername())
                .setEmail(u.getEmail())
                .addRoles("ROLE_" + u.getRole().name())
                .setEnabled(u.isEnabled())
                .build();
        responseObserver.onNext(info);
        responseObserver.onCompleted();
    }
}

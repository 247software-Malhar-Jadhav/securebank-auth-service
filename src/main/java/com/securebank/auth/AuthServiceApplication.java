package com.securebank.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point for the SecureBank auth-service.
 *
 * <p>This service is the platform's identity authority. It speaks two protocols:
 * <ul>
 *   <li><b>REST</b> (port 8081) for browser-facing flows via the gateway:
 *       register / login / refresh.</li>
 *   <li><b>gRPC</b> (port 9091) for other services + the gateway to verify tokens
 *       and look up users without re-implementing JWT logic.</li>
 * </ul>
 *
 * <p>Virtual threads are enabled in application.yml
 * ({@code spring.threads.virtual.enabled=true}) so each request runs on a lightweight
 * JVM thread — a good fit for the mostly-IO-bound auth workload.
 */
@SpringBootApplication
@ConfigurationPropertiesScan // binds @ConfigurationProperties records (e.g. JwtProperties)
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}

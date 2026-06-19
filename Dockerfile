# ============================================================================
# Multi-stage build for auth-service.
#  Stage 1 (build): full Maven + JDK 21 -> compiles, generates gRPC stubs, packages.
#  Stage 2 (run):   slim JRE only -> small, attack-surface-reduced runtime image.
# ============================================================================

# ---- Stage 1: build --------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the POM first and warm the dependency cache. This layer is reused on rebuilds
# whenever pom.xml is unchanged, so source-only edits don't re-download the world.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# Now the sources (incl. vendored protos under src/main/proto).
COPY src ./src

# Build the fat jar. Tests run in CI; skip here to keep image builds fast/deterministic.
RUN mvn -q -B -DskipTests package

# ---- Stage 2: runtime ------------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as a non-root user (defence in depth).
RUN useradd -r -u 1001 appuser
USER appuser

COPY --from=build /app/target/securebank-auth-service-*.jar app.jar

# 8081 = REST, 9091 = gRPC (per MICROSERVICES_SPEC.md).
EXPOSE 8081 9091

# Default to the 'docker' profile; SECUREBANK_JWT_SECRET must be supplied at runtime.
ENV SPRING_PROFILES_ACTIVE=docker
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# syntax=docker/dockerfile:1

# ── Build stage ──────────────────────────────────────────────────────────────
# BUILDPLATFORM = the machine running the build (your laptop / CI runner)
# TARGETPLATFORM = the machine that will run the image (EC2 amd64 or arm64)
ARG BUILDPLATFORM=linux/amd64
ARG TARGETPLATFORM=linux/amd64

FROM --platform=${BUILDPLATFORM} eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Cache Maven dependencies as a separate layer.
# This layer is only invalidated when pom.xml changes, not on every source edit.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source and build the fat JAR.
COPY src src
RUN ./mvnw clean package -DskipTests -B

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM --platform=${TARGETPLATFORM} eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install wget for the health check, then create a non-root user.
RUN apk add --no-cache wget \
    && addgroup -S ingoboka \
    && adduser -S ingoboka -G ingoboka --home /app

# Copy the fat JAR from the build stage.
COPY --from=build /app/target/*.jar app.jar

# Fix ownership so the non-root user can read the JAR.
RUN chown ingoboka:ingoboka app.jar

USER ingoboka

# Internal port the Spring Boot application listens on.
EXPOSE 8080

# ── Health check ──────────────────────────────────────────────────────────────
# Uses the liveness probe endpoint (enabled in application-docker.properties).
# start-period gives the JVM + Flyway migrations time to complete before
# the first check. Adjust if your migration set grows significantly.
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1

# ── Entrypoint ────────────────────────────────────────────────────────────────
# JVM flags explained:
#   -XX:+UseContainerSupport        — read CPU/memory limits from cgroup (default in JDK 11+, explicit here for clarity)
#   -XX:MaxRAMPercentage=75.0       — use at most 75% of the container's memory limit for the JVM heap
#   -XX:+ExitOnOutOfMemoryError     — crash fast on OOM instead of degrading silently
#   -Djava.security.egd=...urandom  — faster entropy source (avoids /dev/random blocking in containers)
#   server.shutdown=graceful        — finish in-flight requests before stopping (SIGTERM handling)
#   lifecycle.timeout=30s           — wait up to 30 seconds for in-flight requests to complete
#
# SPRING_PROFILES_ACTIVE is intentionally NOT set here.
# It is passed as an environment variable at runtime so the same image
# can be used for Docker Compose (docker profile) and production (prod profile).
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dserver.shutdown=graceful", \
  "-Dspring.lifecycle.timeout-per-shutdown-phase=30s", \
  "-jar", "app.jar"]

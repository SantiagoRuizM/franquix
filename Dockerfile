# syntax=docker/dockerfile:1

# ---- build: compile and package the Spring Boot jar --------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# Gradle wrapper and build scripts first, so the dependency layer is cached between builds
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null

COPY src ./src
# Tests run in CI (they need Docker for Testcontainers); the image build only packages
RUN ./gradlew --no-daemon bootJar -x test \
    && java -Djarmode=tools -jar build/libs/franquix-*.jar extract --layers --launcher --destination build/extracted

# ---- runtime: small JRE image, non-root user, layered jar --------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S franquix && adduser -S -G franquix -h /app franquix
WORKDIR /app

# Spring Boot layers ordered from least to most frequently changing
COPY --from=build --chown=franquix:franquix /workspace/build/extracted/dependencies/ ./
COPY --from=build --chown=franquix:franquix /workspace/build/extracted/spring-boot-loader/ ./
COPY --from=build --chown=franquix:franquix /workspace/build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=franquix:franquix /workspace/build/extracted/application/ ./

USER franquix

ENV SPRING_PROFILES_ACTIVE=prod \
    PORT=8080 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=50 -XX:+ExitOnOutOfMemoryError -XX:+UseSerialGC -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- "http://127.0.0.1:${PORT}/actuator/health" | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]

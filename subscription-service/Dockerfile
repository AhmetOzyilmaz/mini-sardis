# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy dependency manifests first — Docker caches this layer until build.gradle changes
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon --quiet

# Copy source and build fat JAR (skip tests — run them separately in CI)
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user — no write access to system paths
RUN addgroup -S sardis && adduser -S sardis -G sardis
USER sardis

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

# UseContainerSupport: honour cgroup memory limits set by Docker
# MaxRAMPercentage: use up to 75% of container RAM for heap
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]

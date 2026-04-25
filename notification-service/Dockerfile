# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Root Gradle wrapper + multi-module config
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle

# sardis-common source — required by all services
COPY sardis-common ./sardis-common

# All service build scripts — Gradle configures every subproject in settings.gradle
COPY subscription-service/build.gradle subscription-service/build.gradle
COPY payment-service/build.gradle payment-service/build.gradle
COPY notification-service/build.gradle notification-service/build.gradle

RUN chmod +x gradlew && ./gradlew :notification-service:dependencies --no-daemon --quiet

# Service source and build fat JAR (skip tests — run them separately in CI)
COPY notification-service/src ./notification-service/src
RUN ./gradlew :notification-service:bootJar --no-daemon -x test

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S sardis && adduser -S sardis -G sardis
USER sardis

COPY --from=build /app/notification-service/build/libs/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]

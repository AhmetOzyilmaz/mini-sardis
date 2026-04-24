FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app
COPY build/libs/notification-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]

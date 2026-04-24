FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app
COPY build/libs/payment-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]

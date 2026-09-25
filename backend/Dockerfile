# Stage 1: Build the Spring Boot application with Maven and Java 17.
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
# Download dependencies first so Docker can reuse this layer between source edits.
RUN mvn dependency:go-offline -B
COPY src ./src
# Tests are executed in local verification; the image build creates the runtime jar.
RUN mvn clean package -DskipTests -B

# Stage 2: Run the packaged jar on a small JRE-only image.
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Run as a non-root user for safer container defaults.
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

EXPOSE 8080
# Docker Compose waits for this healthcheck before starting the frontend proxy.
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]

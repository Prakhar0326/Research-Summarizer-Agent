# Multi-stage build with Gradle
FROM gradle:8.7-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy build configuration and source
COPY build.gradle .
COPY settings.gradle .
COPY gradle ./gradle
COPY gradlew .
COPY gradlew.bat .
COPY src ./src

# Build the application
RUN ./gradlew clean bootJar -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /build/build/libs/research-summarizer-agent-*.jar app.jar

# Set environment variables
ENV OPENAI_API_KEY=""
ENV SERVER_PORT=8080

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/api/research/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

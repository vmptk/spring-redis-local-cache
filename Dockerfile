# Build stage
FROM eclipse-temurin:24-jdk AS builder

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Copy source code
COPY src src

# Make gradlew executable and build the application
RUN chmod +x ./gradlew
RUN ./gradlew bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:24-jre

WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose the port
EXPOSE 8080

# Set JVM options for containerized environment
ENV JAVA_OPTS="-Xmx512m -Xms256m -server"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
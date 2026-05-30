# ===========================================
# SkyCrew Dockerfile — Multi-Stage Build
# ===========================================
# Stage 1: Build with Maven
# Stage 2: Run with minimal JRE
# ===========================================

# --- Stage 1: Build ---
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy POM first (layer caching)
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src src
RUN mvn clean package -DskipTests -B

# --- Stage 2: Runtime ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Security: run as non-root user
RUN addgroup -S skycrew && adduser -S skycrew -G skycrew

# Copy JAR from build stage
COPY --from=build /app/target/skycrew-*.jar app.jar

# Set ownership
RUN chown -R skycrew:skycrew /app
USER skycrew

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

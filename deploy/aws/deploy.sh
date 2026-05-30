#!/bin/bash
# ===========================================
# SkyCrew — AWS EC2 Deployment Script
# ===========================================
# Prerequisites:
#   - Docker installed on EC2 instance
#   - Docker Compose installed
#   - RDS PostgreSQL instance running
# ===========================================

set -e

echo "========================================="
echo "  SkyCrew Deployment Script"
echo "========================================="

# ---- Configuration (set via environment or modify here) ----
APP_NAME="skycrew"
APP_PORT=8080
IMAGE_NAME="${DOCKER_IMAGE:-ghcr.io/your-username/skycrew:latest}"

# RDS Configuration
DB_HOST="${RDS_HOSTNAME:-your-rds-endpoint.region.rds.amazonaws.com}"
DB_PORT="${RDS_PORT:-5432}"
DB_NAME="${RDS_DB_NAME:-skycrew_db}"
DB_USER="${RDS_USERNAME:-skycrew}"
DB_PASS="${RDS_PASSWORD:-change_me_in_production}"

# JWT
JWT_SECRET="${JWT_SECRET:-c2t5Y3Jldy1wcm9kLXNlY3JldC1rZXktZm9yLWp3dC1hdXRoZW50aWNhdGlvbg==}"

# ---- Step 1: Pull latest image ----
echo "[1/4] Pulling latest Docker image..."
docker pull "$IMAGE_NAME"

# ---- Step 2: Stop existing container ----
echo "[2/4] Stopping existing container (if running)..."
docker stop "$APP_NAME" 2>/dev/null || true
docker rm "$APP_NAME" 2>/dev/null || true

# ---- Step 3: Run new container ----
echo "[3/4] Starting new container..."
docker run -d \
  --name "$APP_NAME" \
  --restart unless-stopped \
  -p "$APP_PORT:8080" \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}" \
  -e SPRING_DATASOURCE_USERNAME="$DB_USER" \
  -e SPRING_DATASOURCE_PASSWORD="$DB_PASS" \
  -e SPRING_FLYWAY_ENABLED=true \
  -e SKYCREW_JWT_SECRET="$JWT_SECRET" \
  -e SKYCREW_JWT_EXPIRATION_MS=86400000 \
  -e SKYCREW_RATE_LIMIT_REQUESTS_PER_MINUTE=50 \
  -e SKYCREW_RULES_MIN_REST_HOURS=12 \
  "$IMAGE_NAME"

# ---- Step 4: Health check ----
echo "[4/4] Waiting for application to start..."
for i in {1..30}; do
  if curl -sf "http://localhost:${APP_PORT}/actuator/health" > /dev/null 2>&1; then
    echo "✅ SkyCrew is running at http://localhost:${APP_PORT}"
    echo "📖 Swagger UI: http://localhost:${APP_PORT}/swagger-ui.html"
    exit 0
  fi
  echo "  Waiting... ($i/30)"
  sleep 2
done

echo "❌ Application failed to start. Check logs:"
echo "   docker logs $APP_NAME"
exit 1

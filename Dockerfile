# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM gradle:8-jdk17-jammy AS builder
WORKDIR /app
COPY . .
RUN ./gradlew build -x test --no-daemon

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-bookworm

# Instala Chromium e ChromeDriver (Debian bookworm — sem snap, funciona em Docker)
RUN apt-get update && apt-get install -y \
    chromium \
    chromium-driver \
    fonts-liberation \
    libglib2.0-0 \
    libnss3 \
    libfontconfig1 \
    --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*

ENV CHROME_BIN=/usr/bin/chromium
ENV CHROMEDRIVER_PATH=/usr/bin/chromedriver
ENV CHROME_HEADLESS=true

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

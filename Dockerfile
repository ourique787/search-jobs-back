# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM gradle:8-jdk17-jammy AS builder
WORKDIR /app
COPY . .
RUN ./gradlew build -x test --no-daemon

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM debian:bookworm-slim

# Instala Java 17 + Chromium (Debian bookworm — sem snap, funciona em Docker)
RUN apt-get update && apt-get install -y \
    openjdk-17-jre-headless \
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
ENTRYPOINT ["java", "-Xmx180m", "-XX:+UseContainerSupport", "-XX:+UseG1GC", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

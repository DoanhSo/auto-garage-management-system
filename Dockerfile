# Stage 1: Build package from source
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Minimal runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S garage && adduser -S garage -G garage \
    && mkdir -p /app/data/attachments && chown -R garage:garage /app/data
COPY --from=builder /build/target/AutoGarage-0.0.1-SNAPSHOT.jar app.jar
COPY docker/backend-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh && mkdir -p /app/data/attachments && chown -R garage:garage /app/data
USER garage
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=18 CMD wget -q -O /dev/null http://127.0.0.1:8080/api/v1/public/ui-policy || exit 1
ENTRYPOINT ["/app/docker-entrypoint.sh"]

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S garage && adduser -S garage -G garage
COPY target/AutoGarage-0.0.1-SNAPSHOT.jar app.jar
COPY docker/backend-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh
USER garage
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=18 CMD wget -q -O /dev/null http://127.0.0.1:8080/api/v1/public/ui-policy || exit 1
ENTRYPOINT ["/app/docker-entrypoint.sh"]

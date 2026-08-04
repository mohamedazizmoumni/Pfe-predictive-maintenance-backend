# Build context is the repo root (see docker-compose.ml.yml) because this is
# a multi-module Maven reactor - api-module alone can't be built in isolation.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy POMs first so dependency resolution is cached across source-only changes.
COPY pom.xml .
COPY common-module/pom.xml common-module/pom.xml
COPY core-module/pom.xml core-module/pom.xml
COPY data-module/pom.xml data-module/pom.xml
COPY alert-module/pom.xml alert-module/pom.xml
COPY inventory-module/pom.xml inventory-module/pom.xml
COPY task-module/pom.xml task-module/pom.xml
COPY nlp-module/pom.xml nlp-module/pom.xml
COPY ml-module/pom.xml ml-module/pom.xml
COPY api-module/pom.xml api-module/pom.xml
RUN mvn -q -pl api-module -am dependency:go-offline

# Now bring in sources and build the real jar.
COPY common-module/src common-module/src
COPY core-module/src core-module/src
COPY data-module/src data-module/src
COPY alert-module/src alert-module/src
COPY inventory-module/src inventory-module/src
COPY task-module/src task-module/src
COPY nlp-module/src nlp-module/src
COPY ml-module/src ml-module/src
COPY api-module/src api-module/src
RUN mvn -q -pl api-module -am -DskipTests clean package

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
RUN useradd --system --create-home appuser
WORKDIR /app
COPY --from=build /workspace/api-module/target/api-module-*.jar app.jar
RUN mkdir -p /app/uploads /app/logs && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -sf http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

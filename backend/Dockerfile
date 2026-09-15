# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S linkpilot && adduser -S linkpilot -G linkpilot \
    && mkdir -p /app/data && chown -R linkpilot:linkpilot /app
COPY --from=build /build/target/*.jar app.jar
USER linkpilot
# SQLite file lives here - mount a volume at this path to persist it across container restarts
VOLUME /app/data
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

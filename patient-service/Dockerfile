# ─── Build stage ───────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q

COPY src/ src/
RUN ./mvnw package -DskipTests -q

# ─── Runtime stage ─────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S medapp && adduser -S medapp -G medapp

COPY --from=builder /app/target/*.jar app.jar

RUN chown medapp:medapp app.jar

USER medapp

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]

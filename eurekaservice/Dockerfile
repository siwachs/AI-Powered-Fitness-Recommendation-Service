FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline -DskipTests

COPY src ./src
RUN ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && addgroup --system spring \
 && adduser --system spring --ingroup spring

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN chown spring:spring app.jar

USER spring

ENV SERVER_PORT=8761
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8761/ || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
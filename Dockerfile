# Build Stage
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml /app/
RUN mvn dependency:resolve

# Copy source code and build the application
COPY src /app/src/
RUN mvn clean package -Dmaven.test.skip=true

# Production Stage
FROM eclipse-temurin:17-jre AS production

LABEL maintainer="flood-aid-team"

WORKDIR /app

# Copy the compiled JAR file
COPY --from=build /app/target/*.jar /app/onspot-app.jar

EXPOSE 8080

# Use SerialGC for low-memory environments (Render free tier = 512MB)
# PORT is provided by Render at runtime
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+UseSerialGC", "-Xss256k", "-jar", "/app/onspot-app.jar"]
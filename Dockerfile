# Build Stage
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml /app/
RUN mvn dependency:resolve

# Copy source code and build the application
COPY src /app/src/
RUN mvn clean package -Dmaven.test.skip=true

# Production Stage
FROM eclipse-temurin:21-jre AS production

LABEL maintainer="nonokub.671@gmail.com"

WORKDIR /app

# Copy the compiled JAR file (assuming only one JAR is generated)
COPY --from=build /app/target/*.jar /app/onspot-app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/onspot-app.jar"]
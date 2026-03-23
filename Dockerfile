FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY pom.xml mvnw ./
COPY .mvn .mvn

RUN ./mvnw dependency:go-offline -B

COPY src src

RUN ./mvnw package -DskipTests
EXPOSE 8080


CMD ["java", "-jar", "target/bf-navigator-service-1.0.0.jar"]
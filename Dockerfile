# Этап 1: Сборка приложения с Maven
FROM maven:3.9-eclipse-temurin-24-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Этап 2: Запуск приложения
FROM eclipse-temurin:24-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/smpp-sender-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
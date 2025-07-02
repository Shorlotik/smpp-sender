# Этап 1: Сборка приложения с Maven, OpenJDK 17 (Temurin)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package

# Этап 2: Запуск приложения с JRE 17
FROM eclipse-temurin:17
WORKDIR /app
COPY --from=build /app/target/smpp-sender-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
# Используем официальный образ OpenJDK 24
FROM eclipse-temurin:24-jdk-alpine

# Рабочая директория
WORKDIR /app

# Копируем скомпилированный jar в контейнер
COPY target/smpp-sender-0.0.1-SNAPSHOT.jar app.jar

# Открываем порт 8080
EXPOSE 8080

# Запуск приложения
ENTRYPOINT ["java", "-jar", "app.jar"]

# BUILD
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# RUN
FROM eclipse-temurin:17-jdk-focal
WORKDIR /app
# Derlenen jar dosyasını buraya al
COPY --from=build /app/target/*.jar app.jar

#  8080 portu
EXPOSE 8080

# Baslatma komutu
ENTRYPOINT ["java", "-jar", "app.jar"]
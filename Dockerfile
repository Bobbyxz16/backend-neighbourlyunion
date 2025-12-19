# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiar el JAR
COPY --from=build /app/target/*.jar app.jar

# Exponer puerto
EXPOSE 8080

# Usuario no-root para seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Comando de inicio
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
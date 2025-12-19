# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Crear directorios necesarios
RUN mkdir -p /app/uploads /app/logs

# Copiar el JAR
COPY --from=build /app/target/*.jar app.jar

# Exponer puerto
EXPOSE 8080

# Usuario no-root para seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Variables de entorno por defecto
ENV SPRING_PROFILES_ACTIVE=docker,dev
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
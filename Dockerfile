# Etapa de build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copia pom.xml primero para cache de dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia el código fuente
COPY src ./src

# Construye la aplicación
RUN mvn clean package -DskipTests

# Etapa de runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copia el JAR
COPY --from=builder /app/target/*.jar app.jar

# Verifica que el JAR existe
RUN ls -la *.jar

# Expone el puerto
EXPOSE 8080

# Comando SIMPLE - SIN scripts complicados
ENTRYPOINT ["java", "-jar", "app.jar"]
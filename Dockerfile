FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Instala curl para debugging
RUN apk add --no-cache curl

COPY --from=builder /app/target/*.jar app.jar

# Crear script de inicio con logging
RUN echo '#!/bin/sh' > /app/start.sh && \
    echo 'echo "=== Starting Spring Boot Application ==="' >> /app/start.sh && \
    echo 'echo "Java version: $(java -version 2>&1 | head -1)"' >> /app/start.sh && \
    echo 'echo "PORT env variable: $PORT"' >> /app/start.sh && \
    echo 'echo "Waiting for app to start..."' >> /app/start.sh && \
    echo 'java -jar /app/app.jar &' >> /app/start.sh && \
    echo 'sleep 10' >> /app/start.sh && \
    echo 'echo "Checking if app is running..."' >> /app/start.sh && \
    echo 'curl -f http://localhost:$PORT/health || echo "Health check failed"' >> /app/start.sh && \
    echo 'wait' >> /app/start.sh

RUN chmod +x /app/start.sh

EXPOSE 8080

# Usa el script en lugar de CMD directo
CMD ["/app/start.sh"]
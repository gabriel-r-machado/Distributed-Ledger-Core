# ===== Multi-stage build para otimização =====
# Estágio 1: Build
FROM amazoncorretto:21-alpine AS builder
WORKDIR /app

# Copiar arquivos do projeto
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

# Build da aplicação
RUN chmod +x mvnw && \
    ./mvnw clean package -DskipTests

# Estágio 2: Runtime (imagem final menor)
FROM amazoncorretto:21-alpine
WORKDIR /app

# Instalar curl para healthcheck
RUN apk add --no-cache curl

# Criar usuário não-root para segurança
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiar JAR do estágio de build
COPY --from=builder /app/target/*.jar app.jar

# Expor porta da aplicação (padrão AWS)
EXPOSE 8080

# Healthcheck: Valida se a aplicação está respondendo
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM Tuning para containers (AWS Free Tier: t2.micro = 1GB RAM)
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Executar aplicação
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

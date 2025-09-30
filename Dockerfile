
# Etapa de Build
FROM maven:3.9.9-amazoncorretto-21-debian-bookworm AS build
WORKDIR /build

# Copia os arquivos do projeto para dentro do container
COPY ../.. .

# Executa o Maven para gerar o JAR
RUN mvn clean package -DskipTests

# Etapa de Run
FROM amazoncorretto:21.0.5
WORKDIR /app

# Copia o JAR gerado para o container
COPY --from=build /build/target/*.jar ./notafiscalapi.jar

# Expõe as portas que a aplicação vai usar
EXPOSE 8080
EXPOSE 9090

# Variáveis de ambiente (não recomendado deixar credenciais expostas)
ENV DB_URL=""
ENV DB_USERNAME=""
ENV DB_PASSWORD=""
ENV TZ="America/Sao_Paulo"

# Corrige o erro no ENTRYPOINT
ENTRYPOINT ["java", "-jar", "notafiscalapi.jar"]

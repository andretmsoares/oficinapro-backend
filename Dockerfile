FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew

COPY src src

RUN ./gradlew bootJar --no-daemon


# Imagem final: JRE, sem ferramentas de build e sem root.
# Para builds reproduzíveis, fixe a base por digest (eclipse-temurin:21-jre@sha256:...).
FROM eclipse-temurin:21-jre

# Usuário sem privilégios: um RCE na aplicação não vira root no container.
RUN groupadd --system --gid 10001 app \
 && useradd --system --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app

WORKDIR /app

ENV TZ=America/Sao_Paulo

COPY --from=build --chown=app:app /app/build/libs/*.jar app.jar

USER app

EXPOSE 8080

# Sem curl na imagem: o healthcheck abre um socket com o bash e procura "UP" na resposta.
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8080 && printf 'GET /actuator/health HTTP/1.0\r\nHost: localhost\r\n\r\n' >&3 && grep -q '\"status\":\"UP\"' <&3"]

# MaxRAMPercentage: respeita o limite de memória do container. ExitOnOutOfMemoryError: se a JVM
# esgotar a memória, ela cai e o orquestrador reinicia, em vez de ficar meio-viva e travada.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]

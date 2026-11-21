# ---- Etapa 1: compilar con Maven ----
FROM maven:3.9-eclipse-temurin-17 AS compilacion
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# ---- Etapa 2: imagen liviana solo con Java para ejecutar ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=compilacion /app/target/sistema-notarial.jar app.jar
ENV PORT=8080
EXPOSE 8080
CMD ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]

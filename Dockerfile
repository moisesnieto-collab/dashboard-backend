# ETAPA 1: Construcción (Build)
# Usamos Maven con Java 21 (Temurin) para coincidir con tu proyecto
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
# Agregamos encoding UTF-8 para evitar el error de tildes
RUN mvn clean package -DskipTests -Dproject.build.sourceEncoding=UTF-8

# ETAPA 2: Ejecución (Run)
# Usamos Java 21 ligero para correr la app
FROM eclipse-temurin:21-jdk-alpine
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]

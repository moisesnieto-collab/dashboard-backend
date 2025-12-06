# ETAPA 1: Construcción (Build)
# Usamos Maven con una versión moderna de Java (Temurin)
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# ETAPA 2: Ejecución (Run)
# Cambiamos openjdk por eclipse-temurin que sí existe y es ligera (Alpine)
FROM eclipse-temurin:17-jdk-alpine
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]

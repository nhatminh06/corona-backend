# Stage 1: build the JAR inside the container (Java 11 + Maven)
FROM maven:3.8-openjdk-11 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: runtime (your original base image)
FROM adoptopenjdk/openjdk11:jdk-11.0.2.9-slim
ENV PORT 8080
COPY --from=build /build/target/*.jar /opt/app.jar
WORKDIR /opt
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar

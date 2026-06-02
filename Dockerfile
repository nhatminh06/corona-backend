FROM harbor.lab:8080/library/maven:3.8-openjdk-11 AS build
WORKDIR /build
COPY settings.xml /root/.m2/settings.xml
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests --settings /root/.m2/settings.xml

FROM harbor.lab:8080/library/openjdk11:jdk-11.0.2.9-slim
ENV PORT 8080
COPY --from=build /build/target/*.jar /opt/app.jar
WORKDIR /opt
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar

FROM eclipse-temurin:22-jdk-alpine
LABEL authors="Father"
VOLUME /tmp
ARG JAR_FILE=target/loan-worker-1.0-SNAPSHOT.jar

COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
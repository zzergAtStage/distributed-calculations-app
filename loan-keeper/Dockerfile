FROM eclipse-temurin:22-jdk-alpine
LABEL authors="Father"
ARG JAR_FILE=target/loan-keeper-1.0-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
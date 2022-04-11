FROM maven:3.8.3-openjdk-17-slim

RUN addgroup spring && useradd -m employee-docker-user
USER employee-docker-user:spring

WORKDIR /home/employee-docker-user

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar","/home/employee-docker-user/app.jar"]

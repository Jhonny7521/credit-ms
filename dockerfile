
FROM openjdk:17-alpine
WORKDIR /app
COPY target/credit-ms-0.0.1-SNAPSHOT.jar credit-ms-0.0.1-SNAPSHOT.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "credit-ms-0.0.1-SNAPSHOT.jar"]

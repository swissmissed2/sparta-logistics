FROM eclipse-temurin:17-jre
WORKDIR /app
EXPOSE 8888
COPY build/libs/config-server-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

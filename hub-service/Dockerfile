FROM eclipse-temurin:17
WORKDIR /app
EXPOSE 19092
COPY build/libs/hub-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

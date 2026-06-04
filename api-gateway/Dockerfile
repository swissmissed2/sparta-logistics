FROM eclipse-temurin:17
WORKDIR /app
EXPOSE 8080
COPY build/libs/api-gateway-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

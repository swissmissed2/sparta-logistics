FROM eclipse-temurin:17-jre
WORKDIR /app
EXPOSE 19096
COPY build/libs/delivery-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

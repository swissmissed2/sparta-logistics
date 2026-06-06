FROM eclipse-temurin:17-jre
WORKDIR /app
EXPOSE 19094
COPY build/libs/product-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

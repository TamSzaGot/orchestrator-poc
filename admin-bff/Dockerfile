FROM eclipse-temurin:21-jre

WORKDIR /app
COPY application/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

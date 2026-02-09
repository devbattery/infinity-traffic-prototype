FROM eclipse-temurin:17-jre

WORKDIR /app

COPY build/libs/traffic-frontend-0.1.0-SNAPSHOT.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]

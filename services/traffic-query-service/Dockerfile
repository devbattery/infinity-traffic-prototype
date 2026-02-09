FROM eclipse-temurin:17-jre

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8083

# 쿼리 서비스 JVM 옵션을 보수적으로 설정해 컨테이너 메모리를 보호한다.
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", "-jar", "/app.jar"]

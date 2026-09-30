# Образ одного сервиса; jar подкладывает deploy.sh
FROM eclipse-temurin:22-jre

WORKDIR /app
COPY app.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

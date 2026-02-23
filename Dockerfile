FROM eclipse-temurin:11-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
COPY img.png img.png
CMD ["java", "-jar", "app.jar"]
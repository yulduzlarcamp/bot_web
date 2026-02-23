FROM openjdk:11-jre-slim

WORKDIR /app

COPY target/AsqarovBullBot-1.0-SNAPSHOT-jar-with-dependencies.jar app.jar
COPY img.png img.png

CMD ["java", "-jar", "app.jar"]
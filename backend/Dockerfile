
FROM eclipse-temurin:21-jre-noble

WORKDIR /app

COPY target/library-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]


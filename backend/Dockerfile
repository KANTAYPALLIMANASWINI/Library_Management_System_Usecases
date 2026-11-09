
FROM eclipse-temurin:21-jre-noble

RUN apt-get update \
    && apt-get install -y --only-upgrade apt libapt-pkg6.0t64 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY target/library-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src/main ./src/main
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/backend-0.0.1-SNAPSHOT.jar app.jar

USER app
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]

FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -pl ai-service -am package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/ai-service/target/ai-service-1.0.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

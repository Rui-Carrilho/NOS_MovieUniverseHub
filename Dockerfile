FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src src
COPY dados dados
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system hub && useradd --system --gid hub hub
COPY --from=build /build/target/movie-universe-hub-0.0.1-SNAPSHOT.jar /app/app.jar
COPY dados /app/dados
USER hub
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

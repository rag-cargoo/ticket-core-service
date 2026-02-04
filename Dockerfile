# Stage 1: Build
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app
COPY . .
RUN ./gradlew bootJar --no-daemon

# Stage 2: Run
FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
WORKDIR /app
COPY --from=builder /app/build/libs/app.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","app.jar"]

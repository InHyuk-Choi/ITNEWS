# 1단계: Gradle 빌드
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -q || true
COPY src src
RUN ./gradlew bootJar --no-daemon -q

# 2단계: 실행
FROM eclipse-temurin:25-jre
RUN apt-get update -q && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

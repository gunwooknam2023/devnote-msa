FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace
COPY . .
RUN chmod +x ./gradlew && ./gradlew :news-youtube-service:clean :news-youtube-service:bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /workspace/news-youtube-service/build/libs/*.jar /app/app.jar

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS=""

VOLUME ["/app/logs"]
EXPOSE 8090
ENTRYPOINT ["/bin/sh","-c","java $JAVA_OPTS -jar /app/app.jar"]

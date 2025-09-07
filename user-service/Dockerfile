FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace
COPY . .
RUN chmod +x ./gradlew && ./gradlew :user-service:clean :user-service:bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN mkdir -p /app/uploads/inquiries /app/uploads/profile

COPY --from=builder /workspace/user-service/build/libs/*.jar /app/app.jar

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS=""

VOLUME ["/app/logs", "/app/uploads"]
EXPOSE 8110
ENTRYPOINT ["/bin/sh","-c","java $JAVA_OPTS -jar /app/app.jar"]

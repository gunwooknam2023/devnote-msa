FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 업로드 디렉터리(컨테이너 내부)
RUN mkdir -p /app/uploads/inquiries /app/uploads/profile

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} /app/app.jar

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS=""

VOLUME ["/app/logs", "/app/uploads"]
EXPOSE 8110
ENTRYPOINT ["/bin/sh","-c","java $JAVA_OPTS -jar /app/app.jar"]

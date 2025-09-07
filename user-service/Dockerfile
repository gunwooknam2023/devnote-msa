FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
ARG JAR_FILE
RUN mkdir -p /app/uploads/inquiries /app/uploads/profile
COPY ${JAR_FILE} /app/app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS=""
VOLUME ["/app/logs", "/app/uploads"]
EXPOSE 8110
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
ARG JAR_FILE
COPY ${JAR_FILE} /app/app.jar
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS=""
VOLUME ["/app/logs"]
EXPOSE 8082
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]

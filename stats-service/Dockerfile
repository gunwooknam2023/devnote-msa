# ─────────────────────────────────────────────
# 1) Builder 스테이지: Gradle Wrapper로 프로젝트 전체 빌드
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace

# Gradle 관련 파일들을 먼저 복사하여 Docker 빌드 캐시를 활용합니다.
COPY ../gradlew .
COPY ../gradle ./gradle
COPY ../build.gradle .
COPY ../settings.gradle .

# 각 서비스의 소스코드를 복사합니다.
# stats-service 서비스의 모든 것을 복사합니다.
COPY ./stats-service ./stats-service

# 다른 서비스의 build.gradle 파일도 복사합니다. (의존성 해결)
COPY ./api-gateway/build.gradle ./api-gateway/build.gradle
COPY ./eureka-server/build.gradle ./eureka-server/build.gradle
COPY ./news-youtube-service/build.gradle ./news-youtube-service/build.gradle
COPY ./processor-service/build.gradle ./processor-service/build.gradle
COPY ./user-service/build.gradle ./user-service/build.gradle


# Gradle Wrapper 실행 권한 부여
RUN chmod +x ./gradlew

# stats-service 모듈만 빌드하여 효율성을 높입니다.
RUN ./gradlew :stats-service:bootJar --no-daemon

# ─────────────────────────────────────────────
# 2) Runtime 스테이지: 경량 JRE에서 JAR 실행
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 빌드된 stats-service의 JAR 파일 경로를 정확히 지정하여 복사합니다.
COPY --from=builder /workspace/stats-service/build/libs/*.jar app.jar

# 로그 및 업로드 파일 저장을 위한 볼륨
VOLUME ["/app/logs", "/app/uploads"]

# stats-service의 컨테이너 내부 포트
EXPOSE 8120

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
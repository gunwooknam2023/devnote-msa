# ─────────────────────────────────────────────
# 1) Builder 스테이지: Gradle Wrapper로 프로젝트 전체 빌드
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace

# [수정] 빌드 컨텍스트의 루트(.)에 있는 파일들을 복사
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .

# 각 서비스의 소스코드와 build.gradle을 컨텍스트 루트 기준으로 복사
COPY api-gateway ./api-gateway
COPY eureka-server ./eureka-server
COPY news-youtube-service ./news-youtube-service
COPY processor-service ./processor-service
COPY stats-service ./stats-service
COPY user-service ./user-service

# Gradle Wrapper 실행 권한 부여
RUN chmod +x ./gradlew

# [중요] ARG를 통해 Jenkinsfile에서 빌드할 서비스 이름을 받아옴
ARG SERVICE_NAME
# 해당 서비스 모듈만 지정하여 빌드
RUN ./gradlew :${SERVICE_NAME}:bootJar --no-daemon

# ─────────────────────────────────────────────
# 2) Runtime 스테이지: 경량 JRE에서 JAR 실행
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# ARG를 다시 선언해야 다음 스테이지에서 사용 가능
ARG SERVICE_NAME
# 빌드된 특정 서비스의 JAR 파일 경로를 정확히 지정하여 복사
COPY --from=builder /workspace/${SERVICE_NAME}/build/libs/*.jar app.jar

VOLUME [ "/app/logs", "/app/uploads" ]

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

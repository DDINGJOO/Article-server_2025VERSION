FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# gradle wrapper과 설정, 소스 복사
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
COPY src ./src

# 권한 부여 및 빌드 (테스트 생략)
RUN chmod +x ./gradlew && ./gradlew clean bootJar

# 빌드 결과 확인 (디버깅용)
RUN ls -la /app/build/libs

FROM eclipse-temurin:17-jre
WORKDIR /app

# 빌드 스테이지의 JAR 복사 (가장 넓은 패턴 사용)
COPY --from=build /app/build/libs/*.jar /app/app.jar

# 복사 확인 (디버깅용)
RUN ls -la /app

# JVM 성능 최적화 설정 (2025-11-27)
# - 60만 건 데이터 처리를 위한 메모리 설정
# - G1GC 사용으로 대용량 힙에서도 낮은 지연시간 유지
ENV JAVA_OPTS="-Xms4g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+ParallelRefProcEnabled \
  -XX:MaxMetaspaceSize=256m \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/app/logs \
  -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

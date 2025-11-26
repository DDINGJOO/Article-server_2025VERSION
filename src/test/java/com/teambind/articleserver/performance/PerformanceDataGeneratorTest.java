package com.teambind.articleserver.performance;

import com.teambind.articleserver.performance.data.PerformanceDataGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 성능 테스트 데이터 생성 테스트
 *
 * <p>실행 방법: 1. Docker Compose로 DB 시작: docker compose up -d article-mariadb 2. 테스트 실행: ./gradlew
 * test --tests PerformanceDataGeneratorTest
 */
@SpringBootTest
@ActiveProfiles("performance-test")
@Slf4j
public class PerformanceDataGeneratorTest {

  @Autowired private PerformanceDataGenerator dataGenerator;

  @Value("${performance.test.article-count:600000}")
  private int articleCount;

  @Test
  public void generatePerformanceTestData() {
    log.info("=== 성능 테스트 데이터 생성 시작 ===");
    log.info("목표 게시글 수: {}", articleCount);

    long startTime = System.currentTimeMillis();

    // 데이터 생성
    dataGenerator.generateTestData(articleCount);

    // 검증
    dataGenerator.verifyDataGeneration();

    long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
    log.info("=== 데이터 생성 완료: {}초 소요 ===", elapsedSeconds);
  }

  @Test
  public void cleanupPerformanceTestData() {
    log.info("=== 성능 테스트 데이터 정리 ===");
    dataGenerator.cleanupTestData();
    dataGenerator.verifyDataGeneration();
  }
}

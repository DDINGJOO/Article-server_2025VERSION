package com.teambind.articleserver.performance.framework;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 성능 테스트 기본 클래스
 *
 * <p>Docker Compose가 이미 실행 중인 환경에서 테스트를 수행합니다. MariaDB, Redis, Kafka 등의 인프라에 직접 연결합니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("performance-test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class PerformanceTestBase {

  @BeforeAll
  static void setupEnvironment() {
    System.out.println("========================================");
    System.out.println("Performance Test Environment");
    System.out.println("MariaDB: localhost:13306");
    System.out.println("Redis: localhost:16379");
    System.out.println("Kafka: localhost:19092");
    System.out.println("========================================");
    System.out.println("Note: Docker Compose should be running");
    System.out.println("Run: docker-compose up -d");
    System.out.println("========================================");
  }

  /** 테스트 실행 시간 측정 */
  protected long measureExecutionTime(Runnable task) {
    long startTime = System.currentTimeMillis();
    task.run();
    return System.currentTimeMillis() - startTime;
  }

  /** 메모리 사용량 측정 */
  protected long measureMemoryUsage() {
    Runtime runtime = Runtime.getRuntime();
    runtime.gc(); // 가비지 컬렉션 실행
    return (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024; // MB 단위
  }

  /** 성능 메트릭 로깅 */
  protected void logPerformanceMetrics(String testName, long executionTime, long memoryUsage) {
    System.out.println("\n========================================");
    System.out.println("Performance Test: " + testName);
    System.out.println("Execution Time: " + executionTime + " ms");
    System.out.println("Memory Usage: " + memoryUsage + " MB");
    System.out.println("========================================\n");
  }
}

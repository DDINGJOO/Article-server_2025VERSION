package com.teambind.articleserver;

import static org.assertj.core.api.Assertions.assertThat;

import com.teambind.articleserver.adapter.out.persistence.entity.board.Board;
import com.teambind.articleserver.adapter.out.persistence.repository.ArticleRepository;
import com.teambind.articleserver.adapter.out.persistence.repository.BoardRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** 간단한 통합 테스트 */
@SpringBootTest
@ActiveProfiles("performance-test")
@Slf4j
public class SimpleIntegrationTest {

  @Autowired(required = false)
  private ArticleRepository articleRepository;

  @Autowired(required = false)
  private BoardRepository boardRepository;

  @Autowired(required = false)
  private JdbcTemplate jdbcTemplate;

  @Test
  @Transactional
  void testDatabaseConnection() {
    log.info("========================================");
    log.info("Testing database connection");
    log.info("========================================");

    try {
      // Database 연결 테스트
      if (jdbcTemplate != null) {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(result).isEqualTo(1);
        log.info("✅ Database connection successful");
      } else {
        log.info("⚠️ JdbcTemplate not available");
      }

      // Repository 테스트
      if (boardRepository != null && articleRepository != null) {
        try {
          // 기존 Board 조회 (새로 생성하지 않고 기존 Board 활용)
          long boardCount = boardRepository.count();
          log.info("✅ Current board count: {}", boardCount);

          // Article 수 확인
          long articleCount = articleRepository.count();
          log.info("✅ Current article count: {}", articleCount);

          // 첫 번째 Board 조회 테스트
          if (boardCount > 0) {
            Board firstBoard = boardRepository.findAll().get(0);
            assertThat(firstBoard).isNotNull();
            log.info(
                "✅ Successfully retrieved board: ID={}, Name={}",
                firstBoard.getId(),
                firstBoard.getName());
          }
        } catch (Exception e) {
          log.error("Repository test error", e);
          throw e;
        }
      } else {
        log.info("⚠️ Repositories not available");
      }

      log.info("========================================");
      log.info("Integration test completed successfully");
      log.info("========================================");

    } catch (Exception e) {
      log.error("❌ Integration test failed", e);
      throw e;
    }
  }

  @Test
  void testPerformanceMetrics() {
    log.info("========================================");
    log.info("Testing performance metrics");
    log.info("========================================");

    if (articleRepository == null) {
      log.info("⚠️ ArticleRepository not available, skipping test");
      return;
    }

    try {
      // 간단한 성능 측정
      long startTime = System.currentTimeMillis();

      for (int i = 0; i < 100; i++) {
        articleRepository.count();
      }

      long endTime = System.currentTimeMillis();
      long totalTime = endTime - startTime;
      double avgTime = totalTime / 100.0;

      log.info("✅ 100 count queries executed");
      log.info("   Total time: {} ms", totalTime);
      log.info("   Average time: {:.2f} ms", avgTime);
      log.info("   Throughput: {:.2f} queries/sec", 100000.0 / totalTime);

      assertThat(avgTime).isLessThan(50); // 평균 50ms 미만

      log.info("========================================");
      log.info("Performance test completed successfully");
      log.info("========================================");

    } catch (Exception e) {
      log.error("❌ Performance test failed", e);
      throw e;
    }
  }
}

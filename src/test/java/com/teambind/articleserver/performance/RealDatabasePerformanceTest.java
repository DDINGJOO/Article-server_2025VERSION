package com.teambind.articleserver.performance;

import static org.assertj.core.api.Assertions.assertThat;

import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import com.teambind.articleserver.adapter.out.persistence.repository.ArticleRepository;
import com.teambind.articleserver.performance.framework.PerformanceTestBase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Real Database Performance Test without JPA Cache influence 실제 데이터베이스 성능을 측정하기 위해 JPA 캐시 영향을 제거한
 * 테스트
 */
@SpringBootTest
@ActiveProfiles("performance-test")
@Slf4j
public class RealDatabasePerformanceTest extends PerformanceTestBase {

  private final Random random = new Random();
  @Autowired private ArticleRepository articleRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @PersistenceContext private EntityManager entityManager;
  @Autowired private TransactionTemplate transactionTemplate;

  @Test
  @Transactional
  void testRealDatabaseReadPerformance() {
    log.info("========================================");
    log.info("Testing REAL database read performance (NO CACHE)");
    log.info("========================================");

    // 1. 먼저 DB에 있는 랜덤 article ID들을 JDBC로 직접 조회
    List<String> articleIds =
        jdbcTemplate.queryForList(
            "SELECT article_id FROM articles ORDER BY RAND() LIMIT 200", String.class);

    if (articleIds.isEmpty()) {
      log.warn("No articles found in database");
      return;
    }

    log.info("Found {} random article IDs for testing", articleIds.size());

    // 2. Warm-up (캐시 워밍업이 아닌 커넥션 풀 워밍업)
    for (int i = 0; i < 10; i++) {
      jdbcTemplate.queryForObject("SELECT COUNT(*) FROM articles", Long.class);
    }

    // 3. 실제 성능 측정 - 각 조회마다 영속성 컨텍스트 초기화
    List<Long> responseTimes = new ArrayList<>();

    for (int i = 0; i < Math.min(100, articleIds.size()); i++) {
      String articleId = articleIds.get(i);

      // 영속성 컨텍스트 초기화 (캐시 비우기)
      entityManager.clear();

      long startTime = System.nanoTime();
      Optional<Article> article = articleRepository.findById(articleId);
      long endTime = System.nanoTime();

      if (article.isPresent()) {
        responseTimes.add((endTime - startTime) / 1_000_000); // ms
      }

      // 다시 영속성 컨텍스트 초기화
      entityManager.clear();
    }

    calculateAndLogMetrics(responseTimes, "Real DB Performance (No Cache)");
  }

  @Test
  void testJdbcDirectPerformance() {
    log.info("========================================");
    log.info("Testing JDBC direct query performance");
    log.info("========================================");

    // JDBC로 직접 쿼리하여 JPA 오버헤드 제거
    List<String> articleIds =
        jdbcTemplate.queryForList(
            "SELECT article_id FROM articles ORDER BY RAND() LIMIT 100", String.class);

    List<Long> responseTimes = new ArrayList<>();

    for (String articleId : articleIds) {
      long startTime = System.nanoTime();

      Map<String, Object> article =
          jdbcTemplate.queryForMap("SELECT * FROM articles WHERE article_id = ?", articleId);

      long endTime = System.nanoTime();

      assertThat(article).isNotNull();
      responseTimes.add((endTime - startTime) / 1_000_000);
    }

    calculateAndLogMetrics(responseTimes, "JDBC Direct Query");
  }

  @Test
  void testCacheVsNoCacheComparison() {
    log.info("========================================");
    log.info("Comparing CACHED vs NON-CACHED performance");
    log.info("========================================");

    // 1. 테스트할 article ID 준비
    List<String> testIds =
        jdbcTemplate.queryForList("SELECT article_id FROM articles LIMIT 50", String.class);

    // 2. 캐시된 읽기 성능 측정
    List<Long> cachedTimes = new ArrayList<>();

    // 먼저 한번 로드 (캐시에 올리기)
    for (String id : testIds) {
      articleRepository.findById(id);
    }

    // 캐시된 상태에서 다시 읽기
    for (String id : testIds) {
      long start = System.nanoTime();
      articleRepository.findById(id);
      long end = System.nanoTime();
      cachedTimes.add((end - start) / 1_000_000);
    }

    // 3. 캐시 없는 읽기 성능 측정
    List<Long> nonCachedTimes = new ArrayList<>();

    for (String id : testIds) {
      entityManager.clear(); // 캐시 비우기

      long start = System.nanoTime();
      articleRepository.findById(id);
      long end = System.nanoTime();
      nonCachedTimes.add((end - start) / 1_000_000);
    }

    // 4. 결과 비교
    log.info("========================================");
    log.info("PERFORMANCE COMPARISON");
    log.info("========================================");

    calculateAndLogMetrics(cachedTimes, "WITH JPA CACHE");
    calculateAndLogMetrics(nonCachedTimes, "WITHOUT CACHE (Real DB)");

    double cachedAvg = cachedTimes.stream().mapToLong(Long::longValue).average().orElse(0);
    double nonCachedAvg = nonCachedTimes.stream().mapToLong(Long::longValue).average().orElse(0);

    log.info(
        "Cache Speed Improvement: {}x faster", String.format("%.2f", nonCachedAvg / cachedAvg));
    log.info("========================================");
  }

  @Test
  void testNewTransactionPerQuery() {
    log.info("========================================");
    log.info("Testing with new transaction per query");
    log.info("========================================");

    List<String> articleIds =
        jdbcTemplate.queryForList(
            "SELECT article_id FROM articles ORDER BY RAND() LIMIT 100", String.class);

    List<Long> responseTimes = new ArrayList<>();

    for (String articleId : articleIds) {
      // 각 쿼리마다 새로운 트랜잭션으로 실행
      Long responseTime =
          transactionTemplate.execute(
              status -> {
                long start = System.nanoTime();
                Optional<Article> article = articleRepository.findById(articleId);
                long end = System.nanoTime();

                assertThat(article).isPresent();
                return (end - start) / 1_000_000;
              });

      if (responseTime != null) {
        responseTimes.add(responseTime);
      }
    }

    calculateAndLogMetrics(responseTimes, "New Transaction Per Query");
  }

  @Test
  void testConcurrentNoCachePerformance() throws InterruptedException {
    log.info("========================================");
    log.info("Testing concurrent reads WITHOUT cache");
    log.info("========================================");

    List<String> articleIds =
        jdbcTemplate.queryForList(
            "SELECT article_id FROM articles ORDER BY RAND() LIMIT 1000", String.class);

    if (articleIds.size() < 100) {
      log.warn("Not enough articles for concurrent test");
      return;
    }

    int threads = 20;
    int requestsPerThread = 50;
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    CountDownLatch latch = new CountDownLatch(threads);
    List<Long> allResponseTimes = new CopyOnWriteArrayList<>();
    AtomicInteger successCount = new AtomicInteger(0);

    long testStart = System.currentTimeMillis();

    for (int i = 0; i < threads; i++) {
      executor.submit(
          () -> {
            try {
              for (int j = 0; j < requestsPerThread; j++) {
                String articleId = articleIds.get(random.nextInt(articleIds.size()));

                // 새 트랜잭션에서 실행 (캐시 격리)
                Long responseTime =
                    transactionTemplate.execute(
                        status -> {
                          long start = System.nanoTime();
                          Optional<Article> article = articleRepository.findById(articleId);
                          long end = System.nanoTime();

                          if (article.isPresent()) {
                            return (end - start) / 1_000_000;
                          }
                          return null;
                        });

                if (responseTime != null) {
                  allResponseTimes.add(responseTime);
                  successCount.incrementAndGet();
                }
              }
            } finally {
              latch.countDown();
            }
          });
    }

    latch.await(60, TimeUnit.SECONDS);
    executor.shutdown();

    long testEnd = System.currentTimeMillis();
    long testDuration = testEnd - testStart;

    log.info("========================================");
    log.info("Concurrent Test Results (NO CACHE):");
    log.info("  Threads: {}", threads);
    log.info("  Requests per thread: {}", requestsPerThread);
    log.info("  Total requests: {}", threads * requestsPerThread);
    log.info("  Successful: {}", successCount.get());
    log.info("  Test duration: {} ms", testDuration);
    log.info(
        "  Throughput: {} req/s",
        String.format("%.2f", (successCount.get() * 1000.0) / testDuration));

    calculateAndLogMetrics(new ArrayList<>(allResponseTimes), "Concurrent No-Cache");
  }

  private void calculateAndLogMetrics(List<Long> responseTimes, String testName) {
    if (responseTimes.isEmpty()) {
      log.warn("No response times recorded for {}", testName);
      return;
    }

    Collections.sort(responseTimes);
    int size = responseTimes.size();

    long min = responseTimes.get(0);
    long max = responseTimes.get(size - 1);
    long p50 = responseTimes.get(size / 2);
    long p95 = responseTimes.get(Math.min(size - 1, size * 95 / 100));
    long p99 = responseTimes.get(Math.min(size - 1, size * 99 / 100));
    double avg = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);

    log.info("========================================");
    log.info("{} Metrics:", testName);
    log.info("  Sample size: {}", size);
    log.info("  Min: {} ms", min);
    log.info("  Max: {} ms", max);
    log.info("  Average: {} ms", String.format("%.2f", avg));
    log.info("  P50: {} ms", p50);
    log.info("  P95: {} ms", p95);
    log.info("  P99: {} ms", p99);
    log.info("========================================");
  }
}

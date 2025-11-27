package com.teambind.articleserver.performance;

import com.teambind.articleserver.adapter.in.web.dto.condition.ArticleSearchCriteria;
import com.teambind.articleserver.adapter.in.web.dto.request.ArticleCursorPageRequest;
import com.teambind.articleserver.adapter.in.web.dto.response.ArticleCursorPageResponse;
import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import com.teambind.articleserver.adapter.out.persistence.entity.enums.Status;
import com.teambind.articleserver.adapter.out.persistence.repository.ArticleRepository;
import com.teambind.articleserver.adapter.out.persistence.repository.ArticleRepositoryCustomImpl;
import com.teambind.articleserver.service.crud.impl.ArticleReadService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

/** 실제 프로덕션 컨트롤러/서비스 성능 테스트 60만건 데이터와 다양한 동시성 시나리오 지원 */
@SpringBootTest
@ActiveProfiles("performance-test")
@Slf4j
public class ProductionPerformanceTest {

  @Autowired private ArticleReadService articleReadService;
  @Autowired private ArticleRepository articleRepository;
  @Autowired private ArticleRepositoryCustomImpl articleRepositoryCustom;
  @Autowired private JdbcTemplate jdbcTemplate;
  @PersistenceContext private EntityManager entityManager;

  // 환경변수로 설정 가능한 테스트 파라미터
  @Value("${test.data.size:100000}")
  private int DATA_SIZE;

  @Value("${test.concurrent.users:100}")
  private int CONCURRENT_USERS;

  @Value("${test.warmup.size:100}")
  private int WARMUP_SIZE;

  @Value("${test.measure.iterations:1000}")
  private int MEASURE_ITERATIONS;

  private List<String> testArticleIds;
  private Map<String, PerformanceMetrics> metricsMap = new HashMap<>();

  @BeforeEach
  public void setUp() {
    log.info("========================================");
    log.info("Production Performance Test Setup");
    log.info("Data Size: {} articles", DATA_SIZE);
    log.info("Concurrent Users: {}", CONCURRENT_USERS);
    log.info("========================================\n");

    loadTestArticleIds();
    performWarmup();
  }

  private void loadTestArticleIds() {
    log.info("Loading {} article IDs for testing...", DATA_SIZE);
    String sql = "SELECT article_id FROM articles WHERE status = 'ACTIVE' LIMIT ?";
    testArticleIds = jdbcTemplate.queryForList(sql, String.class, DATA_SIZE);
    log.info("Loaded {} article IDs", testArticleIds.size());
  }

  private void performWarmup() {
    log.info("Warming up with {} requests...", WARMUP_SIZE);
    for (int i = 0; i < WARMUP_SIZE; i++) {
      try {
        articleReadService.fetchArticleById(testArticleIds.get(i % testArticleIds.size()));
      } catch (Exception e) {
        // Ignore warmup errors
      }
    }
    entityManager.clear();
    log.info("Warmup completed\n");
  }

  @Test
  @Transactional(readOnly = true)
  public void testRealProductionQueries() throws Exception {
    log.info("Starting Real Production Performance Test");
    log.info("=".repeat(80));

    // 1. 단건 조회 성능 (fetchArticleById)
    testSingleArticleFetch();

    // 2. 검색 성능 (searchByCursor)
    testSearchPerformance();

    // 3. 동시성 테스트 (다양한 시나리오)
    testConcurrencyScenarios();

    // 4. N+1 쿼리 문제 검증
    testNPlusOneProblem();

    // 5. 최종 리포트 생성
    generateFinalReport();
  }

  /** 1. 단건 조회 성능 테스트 - 실제 사용되는 fetchArticleById */
  private void testSingleArticleFetch() {
    log.info("\nTest 1: Single Article Fetch (fetchArticleById)");
    log.info("-".repeat(50));

    List<Long> responseTimes = new ArrayList<>();
    StopWatch stopWatch = new StopWatch();
    int errors = 0;

    for (int i = 0; i < MEASURE_ITERATIONS; i++) {
      String articleId = testArticleIds.get(i % testArticleIds.size());

      stopWatch.start();
      try {
        Article article = articleReadService.fetchArticleById(articleId);
        // 실제로 연관 데이터 접근 (N+1 문제 체크)
        int imageCount = article.getImages().size();
        String boardName = article.getBoard().getName();
      } catch (Exception e) {
        errors++;
      }
      stopWatch.stop();

      responseTimes.add(stopWatch.getLastTaskTimeMillis());
    }

    PerformanceMetrics metrics = calculateMetrics("Single Article Fetch", responseTimes, errors);
    metricsMap.put("SINGLE_FETCH", metrics);
    printMetrics(metrics);
  }

  /** 2. 검색 성능 테스트 - 실제 사용되는 searchByCursor */
  private void testSearchPerformance() {
    log.info("\nTest 2: Search Performance (searchByCursor)");
    log.info("-".repeat(50));

    List<Long> responseTimes = new ArrayList<>();
    StopWatch stopWatch = new StopWatch();

    // 다양한 검색 조건 시나리오
    String[] searchTerms = {"테스트", "성능", "Article", "개선", "Spring"};

    for (String term : searchTerms) {
      for (int i = 0; i < 100; i++) {
        ArticleSearchCriteria criteria =
            ArticleSearchCriteria.builder().title(term).status(Status.ACTIVE).build();

        ArticleCursorPageRequest pageRequest =
            ArticleCursorPageRequest.builder()
                .size(20)
                .cursorId(i > 0 ? testArticleIds.get(i - 1) : null)
                .build();

        stopWatch.start();
        ArticleCursorPageResponse response =
            articleReadService.searchArticles(criteria, pageRequest);
        stopWatch.stop();

        responseTimes.add(stopWatch.getLastTaskTimeMillis());
      }
    }

    PerformanceMetrics metrics = calculateMetrics("Search Performance", responseTimes, 0);
    metricsMap.put("SEARCH", metrics);
    printMetrics(metrics);
  }

  /** 3. 동시성 테스트 - 다양한 시나리오 */
  private void testConcurrencyScenarios() throws InterruptedException {
    log.info("\nTest 3: Concurrency Scenarios");
    log.info("-".repeat(50));

    // 시나리오 1: 100% 읽기
    testConcurrentReads(CONCURRENT_USERS, "100% Read");

    // 시나리오 2: 80% 읽기, 20% 검색
    testMixedLoad(CONCURRENT_USERS, "80/20 Mix");

    // 시나리오 3: 피크 시간 (2배 사용자)
    testConcurrentReads(CONCURRENT_USERS * 2, "Peak Load");
  }

  private void testConcurrentReads(int users, String scenarioName) throws InterruptedException {
    log.info("\n  Scenario: {} ({} users)", scenarioName, users);

    ExecutorService executor = Executors.newFixedThreadPool(users);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(users);
    List<Long> responseTimes = new CopyOnWriteArrayList<>();

    for (int i = 0; i < users; i++) {
      final int userId = i;
      executor.submit(
          () -> {
            try {
              startLatch.await(); // 모든 스레드가 동시에 시작

              StopWatch sw = new StopWatch();
              sw.start();

              String articleId = testArticleIds.get(userId % testArticleIds.size());
              articleReadService.fetchArticleById(articleId);

              sw.stop();
              responseTimes.add(sw.getTotalTimeMillis());
            } catch (Exception e) {
              log.debug("User {} failed: {}", userId, e.getMessage());
            } finally {
              endLatch.countDown();
            }
          });
    }

    long startTime = System.currentTimeMillis();
    startLatch.countDown(); // 모든 스레드 시작
    endLatch.await(60, TimeUnit.SECONDS);
    long totalTime = System.currentTimeMillis() - startTime;

    executor.shutdown();

    PerformanceMetrics metrics =
        calculateMetrics(scenarioName, responseTimes, users - responseTimes.size());
    metrics.totalTime = totalTime;
    metrics.tps = (double) responseTimes.size() / (totalTime / 1000.0);

    metricsMap.put(scenarioName.toUpperCase().replace(" ", "_"), metrics);
    printConcurrencyMetrics(metrics);
  }

  private void testMixedLoad(int users, String scenarioName) throws InterruptedException {
    log.info("\n  Scenario: {} ({} users)", scenarioName, users);

    ExecutorService executor = Executors.newFixedThreadPool(users);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(users);
    List<Long> responseTimes = new CopyOnWriteArrayList<>();

    for (int i = 0; i < users; i++) {
      final int userId = i;
      final boolean isSearch = userId % 5 == 0; // 20% search

      executor.submit(
          () -> {
            try {
              startLatch.await();

              StopWatch sw = new StopWatch();
              sw.start();

              if (isSearch) {
                // 검색 요청
                ArticleSearchCriteria criteria =
                    ArticleSearchCriteria.builder().title("test").status(Status.ACTIVE).build();
                ArticleCursorPageRequest pageRequest =
                    ArticleCursorPageRequest.builder().size(10).build();
                articleReadService.searchArticles(criteria, pageRequest);
              } else {
                // 단건 조회
                String articleId = testArticleIds.get(userId % testArticleIds.size());
                articleReadService.fetchArticleById(articleId);
              }

              sw.stop();
              responseTimes.add(sw.getTotalTimeMillis());
            } catch (Exception e) {
              log.debug("User {} failed: {}", userId, e.getMessage());
            } finally {
              endLatch.countDown();
            }
          });
    }

    long startTime = System.currentTimeMillis();
    startLatch.countDown();
    endLatch.await(60, TimeUnit.SECONDS);
    long totalTime = System.currentTimeMillis() - startTime;

    executor.shutdown();

    PerformanceMetrics metrics =
        calculateMetrics(scenarioName, responseTimes, users - responseTimes.size());
    metrics.totalTime = totalTime;
    metrics.tps = (double) responseTimes.size() / (totalTime / 1000.0);

    metricsMap.put(scenarioName.toUpperCase().replace(" ", "_"), metrics);
    printConcurrencyMetrics(metrics);
  }

  /** 4. N+1 쿼리 문제 검증 */
  private void testNPlusOneProblem() {
    log.info("\nTest 4: N+1 Query Problem Detection");
    log.info("-".repeat(50));

    // 쿼리 카운트 측정
    String countQuery = "SHOW STATUS LIKE 'Queries'";
    Long beforeQueries = jdbcTemplate.queryForObject(countQuery, (rs, rowNum) -> rs.getLong(2));

    // 10개 Article 조회 후 연관 데이터 접근
    List<Article> articles = articleRepository.findAll().stream().limit(10).toList();

    int totalImages = 0;
    for (Article article : articles) {
      totalImages += article.getImages().size(); // N+1 트리거
    }

    Long afterQueries = jdbcTemplate.queryForObject(countQuery, (rs, rowNum) -> rs.getLong(2));
    long queryCount = afterQueries - beforeQueries;

    log.info("Fetched {} articles with {} total images", articles.size(), totalImages);
    log.info("Query Count: {} (Expected: 1-2, Actual: {})", queryCount, queryCount);
    log.info("N+1 Problem: {}", queryCount > articles.size() + 1 ? "DETECTED" : "NOT FOUND");

    PerformanceMetrics metrics = new PerformanceMetrics();
    metrics.name = "N+1 Detection";
    metrics.queryCount = queryCount;
    metrics.itemCount = articles.size();
    metricsMap.put("N_PLUS_ONE", metrics);
  }

  /** 5. 최종 성능 리포트 생성 */
  private void generateFinalReport() {
    log.info("\n" + "=".repeat(80));
    log.info("FINAL PERFORMANCE REPORT");
    log.info("=".repeat(80));

    log.info("\nSummary:");
    log.info("  Data Size: {} articles", DATA_SIZE);
    log.info("  Concurrent Users: {}", CONCURRENT_USERS);
    log.info("  Test Iterations: {}", MEASURE_ITERATIONS);

    log.info("\nPerformance Metrics:");
    log.info("┌─────────────────────┬────────┬────────┬────────┬────────┬─────────┐");
    log.info("│ Test Scenario       │ P50(ms)│ P95(ms)│ P99(ms)│ Max(ms)│ Grade   │");
    log.info("├─────────────────────┼────────┼────────┼────────┼────────┼─────────┤");

    for (Map.Entry<String, PerformanceMetrics> entry : metricsMap.entrySet()) {
      PerformanceMetrics m = entry.getValue();
      if (m.p50 != null) {
        log.info(
            String.format(
                "│ %-19s │ %6.1f │ %6.1f │ %6.1f │ %6.1f │ %-7s │",
                m.name, m.p50, m.p95, m.p99, m.max, m.grade));
      }
    }
    log.info("└─────────────────────┴────────┴────────┴────────┴────────┴─────────┘");

    // TPS 정보
    log.info("\nThroughput (TPS):");
    metricsMap.values().stream()
        .filter(m -> m.tps != null)
        .forEach(m -> log.info(String.format("  %s: %.1f req/sec", m.name, m.tps)));

    // N+1 문제
    PerformanceMetrics n1Metrics = metricsMap.get("N_PLUS_ONE");
    if (n1Metrics != null) {
      log.info("\nN+1 Query Analysis:");
      log.info("  Query Count: {} for {} items", n1Metrics.queryCount, n1Metrics.itemCount);
      log.info(
          "  Status: {}",
          n1Metrics.queryCount > n1Metrics.itemCount + 1
              ? "PROBLEM DETECTED - Need optimization!"
              : "OK - No N+1 problem");
    }

    // 개선 권장사항
    log.info("\nRecommendations:");
    if (metricsMap.get("SINGLE_FETCH").p95 > 50) {
      log.info("  - Single fetch P95 > 50ms: Consider using EntityGraph or DTO projection");
    }
    if (metricsMap.get("SEARCH").p95 > 100) {
      log.info("  - Search P95 > 100ms: Consider adding full-text index or caching");
    }
    if (n1Metrics != null && n1Metrics.queryCount > n1Metrics.itemCount + 1) {
      log.info("  - N+1 queries detected: Use fetch join or batch size optimization");
    }

    // 파일로 저장
    saveReportToFile();
  }

  private void saveReportToFile() {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    String filename =
        String.format("performance-testing/results/production_test_%d_%s.md", DATA_SIZE, timestamp);

    try (FileWriter writer = new FileWriter(filename)) {
      writer.write("# Production Performance Test Report\n");
      writer.write(String.format("**Date**: %s\n", timestamp));
      writer.write(String.format("**Data Size**: %d articles\n", DATA_SIZE));
      writer.write(String.format("**Concurrent Users**: %d\n\n", CONCURRENT_USERS));

      writer.write("## Performance Metrics\n\n");
      writer.write("| Test Scenario | P50 | P95 | P99 | Max | Errors | Grade |\n");
      writer.write("|---------------|-----|-----|-----|-----|--------|-------|\n");

      for (PerformanceMetrics m : metricsMap.values()) {
        if (m.p50 != null) {
          writer.write(
              String.format(
                  "| %s | %.1fms | %.1fms | %.1fms | %.1fms | %d | %s |\n",
                  m.name, m.p50, m.p95, m.p99, m.max, m.errors, m.grade));
        }
      }

      writer.write("\n## Recommendations\n");
      writer.write("- Implement EntityGraph for single article fetch\n");
      writer.write("- Add full-text index for search optimization\n");
      writer.write("- Use DTO projection for list views\n");

      log.info("\nReport saved to: {}", filename);
    } catch (IOException e) {
      log.error("Failed to save report: {}", e.getMessage());
    }
  }

  private PerformanceMetrics calculateMetrics(String name, List<Long> times, int errors) {
    PerformanceMetrics metrics = new PerformanceMetrics();
    metrics.name = name;
    metrics.errors = errors;

    if (!times.isEmpty()) {
      Collections.sort(times);
      metrics.p50 = times.get(times.size() / 2).doubleValue();
      metrics.p95 = times.get((int) (times.size() * 0.95)).doubleValue();
      metrics.p99 = times.get((int) (times.size() * 0.99)).doubleValue();
      metrics.max = times.get(times.size() - 1).doubleValue();
      metrics.avg = times.stream().mapToLong(Long::longValue).average().orElse(0);
      metrics.grade = determineGrade(metrics.p95);
    }

    return metrics;
  }

  private String determineGrade(Double p95) {
    if (p95 == null) return "N/A";
    if (p95 < 10) return "S";
    if (p95 < 20) return "A";
    if (p95 < 50) return "B";
    if (p95 < 100) return "C";
    return "D";
  }

  private void printMetrics(PerformanceMetrics m) {
    log.info(
        String.format(
            "  P50: %.1fms | P95: %.1fms | P99: %.1fms | Max: %.1fms", m.p50, m.p95, m.p99, m.max));
    log.info(String.format("  Performance Grade: %s | Errors: %d", m.grade, m.errors));
  }

  private void printConcurrencyMetrics(PerformanceMetrics m) {
    log.info(String.format("    Response: P95=%.1fms, Max=%.1fms", m.p95, m.max));
    log.info(String.format("    Throughput: %.1f req/sec", m.tps));
    log.info(
        String.format(
            "    Success Rate: %.1f%%", (1 - (double) m.errors / CONCURRENT_USERS) * 100));
  }

  @Data
  private static class PerformanceMetrics {
    String name;
    Double p50;
    Double p95;
    Double p99;
    Double max;
    Double avg;
    Double tps;
    Long totalTime;
    Long queryCount;
    Integer itemCount;
    Integer errors = 0;
    String grade;
  }
}

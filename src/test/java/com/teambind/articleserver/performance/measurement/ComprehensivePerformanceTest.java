package com.teambind.articleserver.performance.measurement;

import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import com.teambind.articleserver.adapter.out.persistence.repository.ArticleRepository;
import com.teambind.articleserver.service.crud.impl.ArticleReadService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** 포괄적인 성능 테스트 - 커서 기반 페이징 (초반/중반/후반) - 필터 조합 - 텍스트 검색 - 배치 조회 */
@SpringBootTest
@ActiveProfiles("performance-test")
@Slf4j
public class ComprehensivePerformanceTest {

  private static final int EPOCHS = 10;
  private static final int PAGE_SIZE = 20;
  private static final int WARMUP_ITERATIONS = 100;

  @Autowired private ArticleReadService articleReadService;
  @Autowired private ArticleRepository articleRepository;
  @PersistenceContext private EntityManager entityManager;

  private Statistics statistics;
  private Random random = new Random();

  // 성능 메트릭 저장
  private Map<String, List<Long>> testMetrics = new ConcurrentHashMap<>();

  @BeforeEach
  public void setUp() {
    SessionFactory sessionFactory =
        entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
    statistics = sessionFactory.getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();

    // Warmup
    performWarmup();
  }

  private void performWarmup() {
    log.info("Performing warmup...");
    for (int i = 0; i < WARMUP_ITERATIONS; i++) {
      try {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        articleRepository.findAll(pageable);
      } catch (Exception e) {
        // Ignore warmup errors
      }
    }
    statistics.clear();
    log.info("Warmup completed");
  }

  @Test
  @Transactional(readOnly = true)
  public void measureComprehensivePerformance() throws Exception {
    log.info("=== Starting Comprehensive Performance Test ===");
    log.info("Epochs: {}", EPOCHS);

    // Initialize metric collections
    initializeMetrics();

    for (int epoch = 1; epoch <= EPOCHS; epoch++) {
      log.info("\n=== Epoch {}/{} ===", epoch, EPOCHS);

      // 1. 커서 페이징 테스트 (위치별)
      testCursorPagination();

      // 2. 필터 조합 테스트
      testFilterCombinations();

      // 3. 텍스트 검색 테스트
      testTextSearch();

      // 4. 배치 조회 테스트
      testBatchQueries();

      // Clear caches between epochs
      clearCaches();
    }

    // 최종 리포트 생성
    generateFinalReport();
  }

  private void initializeMetrics() {
    testMetrics.put("cursor_early", new ArrayList<>());
    testMetrics.put("cursor_middle", new ArrayList<>());
    testMetrics.put("cursor_late", new ArrayList<>());
    testMetrics.put("filter_single", new ArrayList<>());
    testMetrics.put("filter_double", new ArrayList<>());
    testMetrics.put("filter_triple", new ArrayList<>());
    testMetrics.put("text_title", new ArrayList<>());
    testMetrics.put("text_content", new ArrayList<>());
    testMetrics.put("text_combined", new ArrayList<>());
    testMetrics.put("batch_small", new ArrayList<>());
    testMetrics.put("batch_medium", new ArrayList<>());
    testMetrics.put("batch_large", new ArrayList<>());
  }

  /** 1. 커서 기반 페이징 테스트 */
  private void testCursorPagination() {
    log.info("\n--- Testing Cursor Pagination ---");

    // 초반 커서 (최신 데이터)
    testCursorAtPosition("cursor_early", "Early Cursor (Recent Data)", 0);

    // 중간 커서
    testCursorAtPosition("cursor_middle", "Middle Cursor", 10000);

    // 후반 커서 (오래된 데이터)
    testCursorAtPosition("cursor_late", "Late Cursor (Old Data)", 50000);
  }

  private void testCursorAtPosition(String metricKey, String testName, int offset) {
    log.info("Testing {}", testName);
    statistics.clear();

    long startTime = System.nanoTime();
    try {
      // 커서 위치로 이동
      Pageable pageable =
          PageRequest.of(offset / PAGE_SIZE, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
      var page = articleRepository.findAll(pageable);

      if (!page.isEmpty()) {
        // 해당 위치에서 다음 10페이지 조회
        String lastCursor = page.getContent().get(page.getContent().size() - 1).getId();
        for (int i = 0; i < 10; i++) {
          Query query =
              entityManager.createQuery(
                  "SELECT a FROM Article a WHERE a.createdAt < "
                      + "(SELECT a2.createdAt FROM Article a2 WHERE a2.id = :cursor) "
                      + "ORDER BY a.createdAt DESC");
          query.setParameter("cursor", lastCursor);
          query.setMaxResults(PAGE_SIZE);

          var results = query.getResultList();
          if (!results.isEmpty()) {
            lastCursor = ((Article) results.get(results.size() - 1)).getId();
          }
        }
      }

      long responseTime = System.nanoTime() - startTime;
      testMetrics.get(metricKey).add(responseTime);

      log.info(
          "  {} - Response time: {:.2f}ms, Queries: {}",
          testName,
          responseTime / 1_000_000.0,
          statistics.getQueryExecutionCount());
    } catch (Exception e) {
      log.error("Error in cursor test: {}", e.getMessage());
    }
  }

  /** 2. 필터 조합 테스트 */
  private void testFilterCombinations() {
    log.info("\n--- Testing Filter Combinations ---");

    // 단일 필터
    testSingleFilter();

    // 이중 필터
    testDoubleFilter();

    // 삼중 필터
    testTripleFilter();
  }

  private void testSingleFilter() {
    log.info("Testing Single Filter (board_id)");
    statistics.clear();

    long startTime = System.nanoTime();
    try {
      Query query =
          entityManager.createQuery(
              "SELECT a FROM Article a WHERE a.boardId = :boardId " + "ORDER BY a.createdAt DESC");
      query.setParameter("boardId", 1L);
      query.setMaxResults(100);
      query.getResultList();

      long responseTime = System.nanoTime() - startTime;
      testMetrics.get("filter_single").add(responseTime);

      log.info("  Single Filter - Response time: {:.2f}ms", responseTime / 1_000_000.0);
    } catch (Exception e) {
      log.error("Error in single filter test: {}", e.getMessage());
    }
  }

  private void testDoubleFilter() {
    log.info("Testing Double Filter (board_id + article_type)");
    statistics.clear();

    long startTime = System.nanoTime();
    try {
      Query query =
          entityManager.createQuery(
              "SELECT a FROM Article a WHERE a.boardId = :boardId "
                  + "AND a.articleType = :type ORDER BY a.createdAt DESC");
      query.setParameter("boardId", 1L);
      query.setParameter("type", "REGULAR");
      query.setMaxResults(100);
      query.getResultList();

      long responseTime = System.nanoTime() - startTime;
      testMetrics.get("filter_double").add(responseTime);

      log.info("  Double Filter - Response time: {:.2f}ms", responseTime / 1_000_000.0);
    } catch (Exception e) {
      log.error("Error in double filter test: {}", e.getMessage());
    }
  }

  private void testTripleFilter() {
    log.info("Testing Triple Filter (board_id + article_type + date_range)");
    statistics.clear();

    long startTime = System.nanoTime();
    try {
      LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
      Query query =
          entityManager.createQuery(
              "SELECT a FROM Article a WHERE a.boardId = :boardId "
                  + "AND a.articleType = :type AND a.createdAt > :dateFrom "
                  + "ORDER BY a.createdAt DESC");
      query.setParameter("boardId", 1L);
      query.setParameter("type", "REGULAR");
      query.setParameter("dateFrom", thirtyDaysAgo);
      query.setMaxResults(100);
      query.getResultList();

      long responseTime = System.nanoTime() - startTime;
      testMetrics.get("filter_triple").add(responseTime);

      log.info("  Triple Filter - Response time: {:.2f}ms", responseTime / 1_000_000.0);
    } catch (Exception e) {
      log.error("Error in triple filter test: {}", e.getMessage());
    }
  }

  /** 3. 텍스트 검색 테스트 */
  private void testTextSearch() {
    log.info("\n--- Testing Text Search ---");

    // 제목 검색
    testTitleSearch();

    // 내용 검색
    testContentSearch();

    // 제목+내용 통합 검색
    testCombinedSearch();
  }

  private void testTitleSearch() {
    log.info("Testing Title Search");
    statistics.clear();

    long startTime = System.nanoTime();
    try {
      Query query =
          entityManager.createQuery(
              "SELECT a FROM Article a WHERE a.title LIKE :keyword " + "ORDER BY a.createdAt DESC");
      query.setParameter("keyword", "%Spring%");
      query.setMaxResults(100);
      query.getResultList();

      long responseTime = System.nanoTime() - startTime;
      testMetrics.get("text_title").add(responseTime);

      log.info("  Title Search - Response time: {:.2f}ms", responseTime / 1_000_000.0);
    } catch (Exception e) {
      log.error("Error in title search test: {}", e.getMessage());
    }
  }

  private void testContentSearch() {
    log.info("Testing Content Search");
    statistics.clear();

    long startTime = System.nanoTime();
    try {
      Query query =
          entityManager.createQuery(
              "SELECT a FROM Article a WHERE a.contents LIKE :keyword "
                  + "ORDER BY a.createdAt DESC");
      query.setParameter("keyword", "%performance%");
      query.setMaxResults(100);
      query.getResultList();

      long responseTime = System.nanoTime() - startTime;
      testMetrics.get("text_content").add(responseTime);

      log.info("  Content Search - Response time: {:.2f}ms", responseTime / 1_000_000.0);
    } catch (Exception e) {
      log.error("Error in content search test: {}", e.getMessage());
    }
  }

  private void testCombinedSearch() {
    log.info("Testing Combined Title+Content Search");
    statistics.clear();

    long startTime = System.nanoTime();
    try {
      Query query =
          entityManager.createQuery(
              "SELECT a FROM Article a WHERE (a.title LIKE :keyword OR a.contents LIKE :keyword) "
                  + "ORDER BY a.createdAt DESC");
      query.setParameter("keyword", "%test%");
      query.setMaxResults(100);
      query.getResultList();

      long responseTime = System.nanoTime() - startTime;
      testMetrics.get("text_combined").add(responseTime);

      log.info("  Combined Search - Response time: {:.2f}ms", responseTime / 1_000_000.0);
    } catch (Exception e) {
      log.error("Error in combined search test: {}", e.getMessage());
    }
  }

  /** 4. 배치 조회 테스트 */
  private void testBatchQueries() {
    log.info("\n--- Testing Batch Queries ---");

    // Small batch (100)
    testBatchSize("batch_small", "Small Batch", 100);

    // Medium batch (500)
    testBatchSize("batch_medium", "Medium Batch", 500);

    // Large batch (1000)
    testBatchSize("batch_large", "Large Batch", 1000);
  }

  private void testBatchSize(String metricKey, String testName, int batchSize) {
    log.info("Testing {} (size={})", testName, batchSize);
    statistics.clear();

    long startTime = System.nanoTime();
    try {
      Query query =
          entityManager.createQuery(
              "SELECT a FROM Article a "
                  + "LEFT JOIN FETCH a.images "
                  + "LEFT JOIN FETCH a.keywords "
                  + "ORDER BY a.createdAt DESC");
      query.setMaxResults(batchSize);
      query.getResultList();

      long responseTime = System.nanoTime() - startTime;
      testMetrics.get(metricKey).add(responseTime);

      log.info(
          "  {} - Response time: {:.2f}ms, Queries: {}",
          testName,
          responseTime / 1_000_000.0,
          statistics.getQueryExecutionCount());
    } catch (Exception e) {
      log.error("Error in batch test: {}", e.getMessage());
    }
  }

  private void clearCaches() {
    entityManager.clear();
    statistics.clear();
  }

  private void generateFinalReport() {
    log.info("\n" + "=".repeat(80));
    log.info("COMPREHENSIVE PERFORMANCE REPORT");
    log.info("=".repeat(80));

    // 각 테스트 카테고리별 결과
    generateCategoryReport(
        "CURSOR PAGINATION", Arrays.asList("cursor_early", "cursor_middle", "cursor_late"));

    generateCategoryReport(
        "FILTER COMBINATIONS", Arrays.asList("filter_single", "filter_double", "filter_triple"));

    generateCategoryReport(
        "TEXT SEARCH", Arrays.asList("text_title", "text_content", "text_combined"));

    generateCategoryReport(
        "BATCH QUERIES", Arrays.asList("batch_small", "batch_medium", "batch_large"));

    // 성능 권장사항
    generateRecommendations();
  }

  private void generateCategoryReport(String categoryName, List<String> metrics) {
    log.info("\n### {} ###", categoryName);

    for (String metricKey : metrics) {
      List<Long> times = testMetrics.get(metricKey);
      if (times == null || times.isEmpty()) continue;

      Collections.sort(times);
      long p50 = times.get((int) (times.size() * 0.5));
      long p95 = times.get((int) (times.size() * 0.95));
      long p99 = times.get((int) (times.size() * 0.99));

      String displayName = metricKey.replace("_", " ").toUpperCase();
      log.info(
          "{}: P50={:.2f}ms, P95={:.2f}ms, P99={:.2f}ms",
          displayName,
          p50 / 1_000_000.0,
          p95 / 1_000_000.0,
          p99 / 1_000_000.0);
    }
  }

  private void generateRecommendations() {
    log.info("\n### PERFORMANCE RECOMMENDATIONS ###");

    // 커서 페이징 분석
    double earlyP95 = getP95("cursor_early");
    double lateP95 = getP95("cursor_late");
    if (lateP95 > earlyP95 * 1.5) {
      log.warn("⚠ Cursor pagination shows significant degradation for older data");
      log.warn("  Consider: Partitioning, archiving old data, or optimizing indexes");
    }

    // 필터 조합 분석
    double singleP95 = getP95("filter_single");
    double tripleP95 = getP95("filter_triple");
    if (tripleP95 > singleP95 * 3) {
      log.warn("⚠ Complex filters show poor scaling");
      log.warn("  Consider: Composite indexes for common filter combinations");
    }

    // 텍스트 검색 분석
    double textP95 = getP95("text_content");
    if (textP95 > 1000) {
      log.warn("⚠ Text search is slow (>1s)");
      log.warn("  Consider: Full-text search engine (Elasticsearch) or database FTS");
    }

    // 배치 크기 분석
    double smallBatchP95 = getP95("batch_small");
    double largeBatchP95 = getP95("batch_large");
    if (largeBatchP95 > smallBatchP95 * 10) {
      log.warn("⚠ Large batch queries show poor performance");
      log.warn("  Consider: Implementing pagination or streaming for large results");
    }

    log.info("\n" + "=".repeat(80));
  }

  private double getP95(String metricKey) {
    List<Long> times = testMetrics.get(metricKey);
    if (times == null || times.isEmpty()) return 0;
    Collections.sort(times);
    return times.get((int) (times.size() * 0.95)) / 1_000_000.0;
  }

  @Data
  public static class PerformanceResult {
    private String testName;
    private double p50Latency;
    private double p95Latency;
    private double p99Latency;
    private long queryCount;
    private String recommendation;
  }
}

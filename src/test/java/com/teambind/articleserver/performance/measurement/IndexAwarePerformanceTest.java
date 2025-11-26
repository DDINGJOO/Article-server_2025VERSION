package com.teambind.articleserver.performance.measurement;

import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import com.teambind.articleserver.adapter.out.persistence.repository.ArticleRepository;
import com.teambind.articleserver.service.crud.impl.ArticleReadService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** 인덱스 위치를 고려한 성능 테스트 B-tree 인덱스의 특성상 데이터 위치에 따라 성능이 달라질 수 있음을 반영 */
@SpringBootTest
@ActiveProfiles("performance-test")
@Slf4j
public class IndexAwarePerformanceTest {

  // 테스트 설정
  private static final int TOTAL_OPERATIONS = 10000;
  private static final int WARMUP_OPERATIONS = 1000;
  private static final int EPOCHS = 5;
  @Autowired private ArticleReadService articleReadService;
  @Autowired private ArticleRepository articleRepository;
  @PersistenceContext private EntityManager entityManager;
  private Statistics statistics;
  private Random random = new Random();
  // 데이터 분류 (인덱스 위치별)
  private List<String> earlyIds = new ArrayList<>(); // 초기 데이터 (인덱스 앞부분)
  private List<String> middleIds = new ArrayList<>(); // 중간 데이터 (인덱스 중간)
  private List<String> recentIds = new ArrayList<>(); // 최신 데이터 (인덱스 뒷부분)
  // 각 세그먼트별 response times 저장
  private Map<String, List<Long>> segmentResponseTimes = new HashMap<>();

  @BeforeEach
  public void setUp() {
    SessionFactory sessionFactory =
        entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
    statistics = sessionFactory.getStatistics();
    statistics.setStatisticsEnabled(true);
    statistics.clear();

    loadDataDistribution();
  }

  /** 데이터를 시간/인덱스 위치별로 분류 */
  private void loadDataDistribution() {
    log.info("Loading data distribution for index-aware testing...");

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime thirtyDaysAgo = now.minusDays(30);
    LocalDateTime oneEightyDaysAgo = now.minusDays(180);

    // 최근 데이터 (인덱스 뒷부분 - 30일 이내)
    recentIds =
        articleRepository
            .findAll(PageRequest.of(0, 5000, Sort.by(Sort.Direction.DESC, "createdAt")))
            .stream()
            .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().isAfter(thirtyDaysAgo))
            .map(Article::getId)
            .collect(Collectors.toList());

    // 중간 데이터 (30일 ~ 180일)
    middleIds =
        articleRepository
            .findAll(PageRequest.of(0, 5000, Sort.by(Sort.Direction.DESC, "createdAt")))
            .stream()
            .filter(
                a ->
                    a.getCreatedAt() != null
                        && a.getCreatedAt().isBefore(thirtyDaysAgo)
                        && a.getCreatedAt().isAfter(oneEightyDaysAgo))
            .map(Article::getId)
            .limit(3000)
            .collect(Collectors.toList());

    // 초기 데이터 (인덱스 앞부분 - 180일 이상)
    earlyIds =
        articleRepository
            .findAll(PageRequest.of(0, 5000, Sort.by(Sort.Direction.ASC, "createdAt")))
            .stream()
            .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().isBefore(oneEightyDaysAgo))
            .map(Article::getId)
            .limit(2000)
            .collect(Collectors.toList());

    log.info("Data distribution loaded:");
    log.info("  - Recent data (last 30 days): {} articles", recentIds.size());
    log.info("  - Middle data (30-180 days): {} articles", middleIds.size());
    log.info("  - Early data (180+ days): {} articles", earlyIds.size());

    if (recentIds.isEmpty() || middleIds.isEmpty() || earlyIds.isEmpty()) {
      log.warn("WARNING: Some data categories are empty. Test may not be representative.");
    }
  }

  @Test
  @Transactional(readOnly = true)
  public void measureIndexAwarePerformance() throws Exception {
    log.info("=== Starting Index-Aware Performance Test ===");
    log.info("Total operations: {}, Epochs: {}", TOTAL_OPERATIONS, EPOCHS);

    // Warmup
    performWarmup();

    // Initialize response time collections
    segmentResponseTimes.put("early", new ArrayList<>());
    segmentResponseTimes.put("middle", new ArrayList<>());
    segmentResponseTimes.put("recent", new ArrayList<>());
    segmentResponseTimes.put("realistic", new ArrayList<>());

    Map<String, Long> totalQueries = new HashMap<>();
    Map<String, Integer> totalOperations = new HashMap<>();

    for (int epoch = 1; epoch <= EPOCHS; epoch++) {
      log.info("\n=== Epoch {}/{} ===", epoch, EPOCHS);

      // 1. 초기 데이터 테스트 (인덱스 앞부분)
      testDataSegment(
          "early", "Early Data (180+ days old)", earlyIds, totalQueries, totalOperations);

      // 2. 중간 데이터 테스트 (인덱스 중간)
      testDataSegment(
          "middle", "Middle Data (30-180 days old)", middleIds, totalQueries, totalOperations);

      // 3. 최신 데이터 테스트 (인덱스 뒷부분)
      testDataSegment(
          "recent", "Recent Data (last 30 days)", recentIds, totalQueries, totalOperations);

      // 4. 실제 사용 패턴 테스트 (70% 최신, 20% 중간, 10% 오래된 데이터)
      testRealisticPattern(totalQueries, totalOperations);

      // 캐시 클리어 (다음 에포크를 위해)
      clearCaches();
    }

    // 결과 분석 및 보고
    generateComparativeReport(totalQueries, totalOperations);
  }

  private void performWarmup() {
    log.info("Performing warmup with {} operations...", WARMUP_OPERATIONS);

    for (int i = 0; i < WARMUP_OPERATIONS; i++) {
      String articleId = getRandomId();
      if (articleId != null) {
        try {
          articleReadService.fetchArticleById(articleId);
        } catch (Exception e) {
          // Ignore warmup errors
        }
      }
    }

    statistics.clear();
    log.info("Warmup completed");
  }

  private void testDataSegment(
      String segmentKey,
      String segmentName,
      List<String> segmentIds,
      Map<String, Long> totalQueries,
      Map<String, Integer> totalOperations) {
    log.info("\nTesting {}: {} articles available", segmentName, segmentIds.size());

    if (segmentIds.isEmpty()) {
      log.warn("No data available for segment: {}", segmentName);
      return;
    }

    statistics.clear();
    int operations = Math.min(TOTAL_OPERATIONS / 4, segmentIds.size() * 10);

    for (int i = 0; i < operations; i++) {
      String articleId = segmentIds.get(random.nextInt(segmentIds.size()));

      long startTime = System.nanoTime();
      try {
        articleReadService.fetchArticleById(articleId);
        long responseTime = System.nanoTime() - startTime;
        segmentResponseTimes.get(segmentKey).add(responseTime);
      } catch (Exception e) {
        log.debug("Error reading article {}: {}", articleId, e.getMessage());
      }
    }

    // Update total metrics
    totalQueries.merge(segmentKey, statistics.getQueryExecutionCount(), Long::sum);
    totalOperations.merge(segmentKey, operations, Integer::sum);

    // Print immediate results
    printSegmentResults(
        segmentName,
        segmentResponseTimes.get(segmentKey),
        statistics.getQueryExecutionCount(),
        operations);
  }

  private void testRealisticPattern(
      Map<String, Long> totalQueries, Map<String, Integer> totalOperations) {
    log.info("\nTesting Realistic Usage Pattern (70% recent, 20% middle, 10% old)");

    statistics.clear();
    int operations = TOTAL_OPERATIONS / 4;

    for (int i = 0; i < operations; i++) {
      String articleId = getRealisticPatternId();

      if (articleId != null) {
        long startTime = System.nanoTime();
        try {
          articleReadService.fetchArticleById(articleId);
          long responseTime = System.nanoTime() - startTime;
          segmentResponseTimes.get("realistic").add(responseTime);
        } catch (Exception e) {
          log.debug("Error reading article {}: {}", articleId, e.getMessage());
        }
      }
    }

    totalQueries.merge("realistic", statistics.getQueryExecutionCount(), Long::sum);
    totalOperations.merge("realistic", operations, Integer::sum);

    printSegmentResults(
        "Realistic Pattern",
        segmentResponseTimes.get("realistic"),
        statistics.getQueryExecutionCount(),
        operations);
  }

  private String getRealisticPatternId() {
    double rand = random.nextDouble();

    if (rand < 0.7 && !recentIds.isEmpty()) {
      // 70% - 최신 데이터
      return recentIds.get(random.nextInt(recentIds.size()));
    } else if (rand < 0.9 && !middleIds.isEmpty()) {
      // 20% - 중간 데이터
      return middleIds.get(random.nextInt(middleIds.size()));
    } else if (!earlyIds.isEmpty()) {
      // 10% - 오래된 데이터
      return earlyIds.get(random.nextInt(earlyIds.size()));
    }

    return getRandomId(); // Fallback
  }

  private String getRandomId() {
    List<String> allIds = new ArrayList<>();
    allIds.addAll(recentIds);
    allIds.addAll(middleIds);
    allIds.addAll(earlyIds);

    if (allIds.isEmpty()) {
      return null;
    }

    return allIds.get(random.nextInt(allIds.size()));
  }

  private void clearCaches() {
    entityManager.clear();
    statistics.clear();
  }

  private void printSegmentResults(
      String segmentName, List<Long> responseTimes, long queryCount, int operations) {
    if (responseTimes.isEmpty()) {
      log.warn("{}: No successful operations", segmentName);
      return;
    }

    List<Long> sortedTimes = new ArrayList<>(responseTimes);
    Collections.sort(sortedTimes);

    // Only use the most recent results for this epoch
    int epochSize = Math.min(operations, sortedTimes.size());
    List<Long> epochTimes =
        sortedTimes.subList(Math.max(0, sortedTimes.size() - epochSize), sortedTimes.size());

    if (epochTimes.isEmpty()) {
      return;
    }

    long p50 = epochTimes.get((int) (epochTimes.size() * 0.5));
    long p95 = epochTimes.get((int) (epochTimes.size() * 0.95));
    long p99 = epochTimes.get((int) (epochTimes.size() * 0.99));

    log.info("{} Results:", segmentName);
    log.info("  - Operations: {}", operations);
    log.info("  - Query Count: {}", queryCount);
    log.info("  - P50 Latency: {}ms", String.format("%.2f", p50 / 1_000_000.0));
    log.info("  - P95 Latency: {}ms", String.format("%.2f", p95 / 1_000_000.0));
    log.info("  - P99 Latency: {}ms", String.format("%.2f", p99 / 1_000_000.0));
    log.info(
        "  - Queries per Operation: {}", String.format("%.2f", (double) queryCount / operations));
  }

  private void generateComparativeReport(
      Map<String, Long> totalQueries, Map<String, Integer> totalOperations) {
    log.info("\n" + "=".repeat(60));
    log.info("COMPARATIVE PERFORMANCE REPORT");
    log.info("=".repeat(60));

    for (String category : Arrays.asList("early", "middle", "recent", "realistic")) {
      List<Long> allResponseTimes = segmentResponseTimes.get(category);

      if (allResponseTimes == null || allResponseTimes.isEmpty()) {
        continue;
      }

      Collections.sort(allResponseTimes);

      long p50 = allResponseTimes.get((int) (allResponseTimes.size() * 0.5));
      long p95 = allResponseTimes.get((int) (allResponseTimes.size() * 0.95));
      long p99 = allResponseTimes.get((int) (allResponseTimes.size() * 0.99));

      String displayName = category.substring(0, 1).toUpperCase() + category.substring(1);
      log.info("\n{} Data Performance (Averaged over {} epochs):", displayName, EPOCHS);
      log.info("  Total Operations: {}", totalOperations.getOrDefault(category, 0));
      log.info("  Total Queries: {}", totalQueries.getOrDefault(category, 0L));

      int ops = totalOperations.getOrDefault(category, 1);
      long queries = totalQueries.getOrDefault(category, 0L);
      log.info("  Avg Queries/Operation: {}", String.format("%.2f", (double) queries / ops));
      log.info("  P50 Latency: {}ms", String.format("%.2f", p50 / 1_000_000.0));
      log.info("  P95 Latency: {}ms", String.format("%.2f", p95 / 1_000_000.0));
      log.info("  P99 Latency: {}ms", String.format("%.2f", p99 / 1_000_000.0));
    }

    // 성능 차이 분석
    analyzePerformanceDifferences();
  }

  private void analyzePerformanceDifferences() {
    log.info("\n" + "=".repeat(60));
    log.info("PERFORMANCE DIFFERENCE ANALYSIS");
    log.info("=".repeat(60));

    Map<String, Double> p95Values = new HashMap<>();

    for (String category : Arrays.asList("early", "middle", "recent", "realistic")) {
      List<Long> times = segmentResponseTimes.get(category);
      if (times != null && !times.isEmpty()) {
        Collections.sort(times);
        long p95 = times.get((int) (times.size() * 0.95));
        p95Values.put(category, p95 / 1_000_000.0);
      }
    }

    if (p95Values.containsKey("early") && p95Values.containsKey("recent")) {
      double earlyP95 = p95Values.get("early");
      double recentP95 = p95Values.get("recent");
      double difference = ((recentP95 - earlyP95) / earlyP95) * 100;

      log.info("\nIndex Position Impact:");
      log.info("  Early Data P95: {}ms", String.format("%.2f", earlyP95));
      log.info("  Recent Data P95: {}ms", String.format("%.2f", recentP95));
      log.info("  Performance Difference: {}%", String.format("%.1f", difference));

      if (Math.abs(difference) > 20) {
        log.warn("  ️  Significant performance difference detected!");
        log.warn("  Consider index optimization or partitioning strategies.");
      }
    }

    if (p95Values.containsKey("realistic")) {
      log.info("\nRealistic Usage Pattern:");
      log.info("  P95 Latency: {}ms", String.format("%.2f", p95Values.get("realistic")));
      log.info("  This represents expected production performance");
    }

    log.info("\n" + "=".repeat(60));
  }
}

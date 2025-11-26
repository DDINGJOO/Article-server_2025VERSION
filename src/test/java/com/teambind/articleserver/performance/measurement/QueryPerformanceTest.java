package com.teambind.articleserver.performance.measurement;

import com.teambind.articleserver.adapter.in.web.dto.condition.ArticleSearchCriteria;
import com.teambind.articleserver.adapter.in.web.dto.request.ArticleCursorPageRequest;
import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import com.teambind.articleserver.adapter.out.persistence.entity.board.Board;
import com.teambind.articleserver.adapter.out.persistence.entity.enums.Status;
import com.teambind.articleserver.adapter.out.persistence.entity.keyword.Keyword;
import com.teambind.articleserver.adapter.out.persistence.repository.ArticleRepository;
import com.teambind.articleserver.adapter.out.persistence.repository.BoardRepository;
import com.teambind.articleserver.adapter.out.persistence.repository.KeywordRepository;
import com.teambind.articleserver.performance.metrics.PerformanceMetrics;
import com.teambind.articleserver.performance.metrics.QueryMetricsCollector;
import com.teambind.articleserver.service.crud.impl.ArticleReadService;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 게시글 조회 성능 측정 테스트
 *
 * <p>여러 Epoch를 통해 안정적인 평균 성능 측정
 */
@SpringBootTest
@ActiveProfiles("performance-test")
@Slf4j
public class QueryPerformanceTest {

  @Autowired private ArticleReadService articleReadService;

  @Autowired private ArticleRepository articleRepository;

  @Autowired private BoardRepository boardRepository;

  @Autowired private KeywordRepository keywordRepository;

  @Autowired private SessionFactory sessionFactory;

  @Autowired private QueryMetricsCollector metricsCollector;

  // 테스트 설정
  @Value("${performance.test.epochs:5}")
  private int epochs; // 반복 횟수

  @Value("${performance.test.warmup-iterations:50}")
  private int warmupIterations;

  @Value("${performance.test.test-iterations:200}")
  private int testIterations;

  private List<String> articleIds;
  private List<Board> boards;
  private List<Keyword> keywords;
  private Random random = new Random();

  @BeforeEach
  public void setup() {
    log.info("테스트 데이터 준비 중...");

    // 테스트용 Article ID 샘플링 (전체 중 1000개)
    articleIds = articleRepository.findAll().stream().limit(1000).map(Article::getId).toList();

    boards = boardRepository.findAll();
    keywords = keywordRepository.findAll();

    // Hibernate Statistics 초기화
    Statistics stats = sessionFactory.getStatistics();
    stats.setStatisticsEnabled(true);
    stats.clear();

    log.info(
        "준비 완료 - Articles: {}, Boards: {}, Keywords: {}",
        articleIds.size(),
        boards.size(),
        keywords.size());
  }

  /** 메인 성능 테스트 - 모든 시나리오를 여러 Epoch로 실행 */
  @Test
  public void measureCurrentPerformance() {
    log.info("╔══════════════════════════════════════════╗");
    log.info("║     현재 프로젝트 성능 측정 시작         ║");
    log.info("╚══════════════════════════════════════════╝");
    log.info("설정: Epochs={}, Warmup={}, Iterations={}", epochs, warmupIterations, testIterations);

    // 1. 단건 조회 성능
    runMultipleEpochs("단건 조회", this::testSingleArticleRead);

    // 2. 페이지네이션 성능
    runMultipleEpochs("페이지네이션 (size=20)", () -> testPagination(20));
    runMultipleEpochs("페이지네이션 (size=50)", () -> testPagination(50));

    // 3. 복잡한 검색 성능
    runMultipleEpochs("복잡한 검색", this::testComplexSearch);

    // 4. 동시 사용자 성능
    runMultipleEpochs("동시 사용자 (50명)", () -> testConcurrentUsers(50));
    runMultipleEpochs("동시 사용자 (100명)", () -> testConcurrentUsers(100));

    // 최종 요약
    printFinalSummary();
  }

  /** 여러 Epoch를 실행하고 평균 계산 */
  private void runMultipleEpochs(String scenarioName, Runnable testScenario) {
    log.info("\n=== {} 테스트 시작 ===", scenarioName);

    List<PerformanceMetrics> epochResults = new ArrayList<>();

    // Warm-up
    log.info("Warm-up 실행 중 ({} iterations)...", warmupIterations);
    for (int i = 0; i < warmupIterations; i++) {
      try {
        performSingleRead(); // 간단한 warm-up
      } catch (Exception e) {
        // warm-up 에러는 무시
      }
    }

    // 각 Epoch 실행
    for (int epoch = 1; epoch <= epochs; epoch++) {
      log.info("Epoch {}/{} 실행 중...", epoch, epochs);
      metricsCollector.reset();

      // 테스트 시나리오 실행
      long startTime = System.currentTimeMillis();
      testScenario.run();
      long elapsedMs = System.currentTimeMillis() - startTime;

      // 메트릭 수집
      PerformanceMetrics metrics = metricsCollector.getMetrics();
      epochResults.add(metrics);

      log.info(
          "Epoch {} 완료 - P50: {}ms, P95: {}ms, P99: {}ms, 쿼리: {}",
          epoch,
          metrics.getP50ResponseTime(),
          metrics.getP95ResponseTime(),
          metrics.getP99ResponseTime(),
          metrics.getAverageQueryCount());
    }

    // 평균 계산 및 출력
    printAverageResults(scenarioName, epochResults);
  }

  /** 시나리오 1: 단건 조회 */
  private void testSingleArticleRead() {
    Statistics stats = sessionFactory.getStatistics();

    for (int i = 0; i < testIterations; i++) {
      String articleId = articleIds.get(random.nextInt(articleIds.size()));

      long startTime = System.nanoTime();
      long queryCountBefore = stats.getQueryExecutionCount();

      try {
        // 실제 조회
        Article article = articleReadService.fetchArticleById(articleId);

        long queryCountAfter = stats.getQueryExecutionCount();
        long elapsedNanos = System.nanoTime() - startTime;

        // 메트릭 기록
        metricsCollector.recordResponseTime(elapsedNanos);
        metricsCollector.recordQueryCount(queryCountAfter - queryCountBefore);
      } catch (Exception e) {
        // Article not found - skip this iteration
        log.debug("Article 조회 실패: {}", e.getMessage());
      }
    }
  }

  /** 시나리오 2: 페이지네이션 */
  private void testPagination(int pageSize) {
    Statistics stats = sessionFactory.getStatistics();

    for (int i = 0; i < testIterations; i++) {
      ArticleCursorPageRequest pageRequest =
          ArticleCursorPageRequest.builder().size(pageSize).build();

      ArticleSearchCriteria criteria =
          ArticleSearchCriteria.builder().status(Status.ACTIVE).build();

      long startTime = System.nanoTime();
      long queryCountBefore = stats.getQueryExecutionCount();

      // 페이지 조회
      var result = articleReadService.searchArticles(criteria, pageRequest);

      long queryCountAfter = stats.getQueryExecutionCount();
      long elapsedNanos = System.nanoTime() - startTime;

      metricsCollector.recordResponseTime(elapsedNanos);
      metricsCollector.recordQueryCount(queryCountAfter - queryCountBefore);
    }
  }

  /** 시나리오 3: 복잡한 검색 */
  private void testComplexSearch() {
    Statistics stats = sessionFactory.getStatistics();

    for (int i = 0; i < testIterations; i++) {
      // 랜덤 검색 조건
      Board randomBoard = boards.get(random.nextInt(boards.size()));
      List<Keyword> randomKeywords = selectRandomKeywords(3);

      ArticleSearchCriteria criteria =
          ArticleSearchCriteria.builder()
              .status(Status.ACTIVE)
              .board(randomBoard)
              .keywords(randomKeywords)
              .title("테스트")
              .build();

      ArticleCursorPageRequest pageRequest = ArticleCursorPageRequest.builder().size(20).build();

      long startTime = System.nanoTime();
      long queryCountBefore = stats.getQueryExecutionCount();

      var result = articleReadService.searchArticles(criteria, pageRequest);

      long queryCountAfter = stats.getQueryExecutionCount();
      long elapsedNanos = System.nanoTime() - startTime;

      metricsCollector.recordResponseTime(elapsedNanos);
      metricsCollector.recordQueryCount(queryCountAfter - queryCountBefore);
    }
  }

  /** 시나리오 4: 동시 사용자 */
  private void testConcurrentUsers(int userCount) {
    int requestsPerUser = testIterations / userCount;
    ExecutorService executor = Executors.newFixedThreadPool(userCount);
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (int user = 0; user < userCount; user++) {
      CompletableFuture<Void> future =
          CompletableFuture.runAsync(
              () -> {
                for (int req = 0; req < requestsPerUser; req++) {
                  try {
                    performRandomQuery();
                  } catch (Exception e) {
                    metricsCollector.recordError();
                  }
                }
              },
              executor);
      futures.add(future);
    }

    // 모든 요청 완료 대기
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    executor.shutdown();
  }

  // === Helper Methods ===

  private void performSingleRead() {
    try {
      String articleId = articleIds.get(random.nextInt(articleIds.size()));
      articleReadService.fetchArticleById(articleId);
    } catch (Exception e) {
      // Ignore errors during warm-up
    }
  }

  private void performRandomQuery() {
    Statistics stats = sessionFactory.getStatistics();
    int queryType = random.nextInt(3);

    long startTime = System.nanoTime();
    long queryCountBefore = stats.getQueryExecutionCount();

    switch (queryType) {
      case 0 -> {
        // 단건 조회
        String articleId = articleIds.get(random.nextInt(articleIds.size()));
        articleReadService.fetchArticleById(articleId);
      }
      case 1 -> {
        // 페이지 조회
        articleReadService.searchArticles(
            ArticleSearchCriteria.builder().status(Status.ACTIVE).build(),
            ArticleCursorPageRequest.builder().size(20).build());
      }
      case 2 -> {
        // 검색
        Board board = boards.get(random.nextInt(boards.size()));
        articleReadService.searchArticles(
            ArticleSearchCriteria.builder().status(Status.ACTIVE).board(board).build(),
            ArticleCursorPageRequest.builder().size(10).build());
      }
    }

    long queryCountAfter = stats.getQueryExecutionCount();
    long elapsedNanos = System.nanoTime() - startTime;

    metricsCollector.recordResponseTime(elapsedNanos);
    metricsCollector.recordQueryCount(queryCountAfter - queryCountBefore);
  }

  private List<Keyword> selectRandomKeywords(int count) {
    List<Keyword> selected = new ArrayList<>();
    List<Integer> indices = IntStream.range(0, Math.min(keywords.size(), count)).boxed().toList();

    for (int idx : indices) {
      selected.add(keywords.get(idx));
    }
    return selected;
  }

  /** Epoch 결과들의 평균 계산 및 출력 */
  private void printAverageResults(String scenario, List<PerformanceMetrics> epochResults) {
    if (epochResults.isEmpty()) {
      log.warn("결과 데이터가 없습니다: {}", scenario);
      return;
    }

    // 평균 계산
    double avgP50 =
        epochResults.stream()
            .mapToDouble(PerformanceMetrics::getP50ResponseTime)
            .average()
            .orElse(0);

    double avgP95 =
        epochResults.stream()
            .mapToDouble(PerformanceMetrics::getP95ResponseTime)
            .average()
            .orElse(0);

    double avgP99 =
        epochResults.stream()
            .mapToDouble(PerformanceMetrics::getP99ResponseTime)
            .average()
            .orElse(0);

    double avgQueries =
        epochResults.stream()
            .mapToDouble(PerformanceMetrics::getAverageQueryCount)
            .average()
            .orElse(0);

    int totalRequests = epochResults.stream().mapToInt(m -> m.getTotalRequests()).sum();

    // 표준편차 계산 (안정성 지표)
    double stdP95 =
        calculateStandardDeviation(
            epochResults.stream().mapToDouble(PerformanceMetrics::getP95ResponseTime).toArray());

    log.info("\n┌─────────────────────────────────────────┐");
    log.info("│ {} - 평균 결과 ({} Epochs)", scenario, epochs);
    log.info("├─────────────────────────────────────────┤");
    log.info("│ 응답 시간:                              │");
    log.info("│   P50: {} ms                            │", String.format("%.2f", avgP50));
    log.info(
        "│   P95: {} ms (±{})                      │",
        String.format("%.2f", avgP95),
        String.format("%.2f", stdP95));
    log.info("│   P99: {} ms                            │", String.format("%.2f", avgP99));
    log.info("│ 쿼리:                                   │");
    log.info("│   평균 쿼리 수: {}                      │", String.format("%.2f", avgQueries));
    log.info("│ 기타:                                   │");
    log.info("│   총 요청 수: {}                        │", totalRequests);
    log.info("└─────────────────────────────────────────┘");

    // 성능 기준 체크
    checkPerformanceCriteria(scenario, avgP95, avgP99, avgQueries);

    // 결과 저장
    PerformanceMetrics avgMetrics = new PerformanceMetrics();
    avgMetrics.setP50ResponseTime(avgP50);
    avgMetrics.setP95ResponseTime(avgP95);
    avgMetrics.setP99ResponseTime(avgP99);
    avgMetrics.setAverageQueryCount(avgQueries);
    avgMetrics.setTotalRequests(totalRequests);
    avgMetrics.saveToJson(scenario);
    avgMetrics.saveToCsv(scenario);
  }

  /** 표준편차 계산 */
  private double calculateStandardDeviation(double[] values) {
    if (values.length == 0) return 0;

    double mean = 0;
    for (double v : values) mean += v;
    mean /= values.length;

    double variance = 0;
    for (double v : values) {
      variance += Math.pow(v - mean, 2);
    }
    variance /= values.length;

    return Math.sqrt(variance);
  }

  /** 성능 기준 체크 */
  private void checkPerformanceCriteria(String scenario, double p95, double p99, double queries) {
    boolean passed = true;
    StringBuilder issues = new StringBuilder();

    // 기준값 (application-performance-test.yml에서 설정 가능)
    double targetP95 = 50.0;
    double targetP99 = 150.0;
    double maxQueries = 5.0;

    if (p95 > targetP95) {
      passed = false;
      issues.append(String.format("P95 초과 (목표: %.0fms, 실제: %.2fms) ", targetP95, p95));
    }
    if (p99 > targetP99) {
      passed = false;
      issues.append(String.format("P99 초과 (목표: %.0fms, 실제: %.2fms) ", targetP99, p99));
    }
    if (queries > maxQueries) {
      passed = false;
      issues.append(String.format("쿼리 과다 (목표: %.0f, 실제: %.2f) ", maxQueries, queries));
    }

    if (passed) {
      log.info(" {} - 성능 기준 PASS", scenario);
    } else {
      log.warn("️ {} - 성능 기준 FAIL: {}", scenario, issues.toString());
    }
  }

  /** 최종 요약 */
  private void printFinalSummary() {
    Statistics stats = sessionFactory.getStatistics();

    log.info("\n╔══════════════════════════════════════════╗");
    log.info("║          최종 성능 측정 요약             ║");
    log.info("╚══════════════════════════════════════════╝");
    log.info("총 실행 Epochs: {}", epochs);
    log.info("Hibernate 통계:");
    log.info("  - 총 쿼리 실행: {}", stats.getQueryExecutionCount());
    log.info("  - Entity 로드: {}", stats.getEntityLoadCount());
    log.info("  - Collection 로드: {}", stats.getCollectionLoadCount());
    log.info("  - 2차 캐시 Hit: {}", stats.getSecondLevelCacheHitCount());
    log.info("  - 2차 캐시 Miss: {}", stats.getSecondLevelCacheMissCount());
    log.info("\n결과 파일: performance-results/ 디렉토리 확인");
  }
}

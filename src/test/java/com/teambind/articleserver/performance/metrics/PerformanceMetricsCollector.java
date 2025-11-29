package com.teambind.articleserver.performance.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Component;

/** 성능 메트릭 수집기 테스트 실행 중 메트릭을 수집하고 관리 */
@Slf4j
@Component
public class PerformanceMetricsCollector {

  private final Map<String, List<Long>> responseTimesMap = new ConcurrentHashMap<>();
  private final Map<String, PerformanceMetrics> metricsMap = new ConcurrentHashMap<>();
  private final List<String> testExecutionOrder = new ArrayList<>();

  /** 응답 시간 기록 */
  public void recordResponseTime(String testName, long responseTimeMs) {
    responseTimesMap.computeIfAbsent(testName, k -> new ArrayList<>()).add(responseTimeMs);
  }

  /** 응답 시간 일괄 기록 */
  public void recordResponseTimes(String testName, List<Long> responseTimes) {
    responseTimesMap.computeIfAbsent(testName, k -> new ArrayList<>()).addAll(responseTimes);
  }

  /** 메트릭 계산 및 저장 */
  public PerformanceMetrics calculateAndStore(String testName) {
    List<Long> responseTimes = responseTimesMap.get(testName);
    if (responseTimes == null || responseTimes.isEmpty()) {
      log.warn("No response times recorded for test: {}", testName);
      return PerformanceMetrics.builder().testName(testName).build();
    }

    PerformanceMetrics metrics = PerformanceMetrics.calculate(testName, responseTimes);
    metricsMap.put(testName, metrics);
    testExecutionOrder.add(testName);

    // 즉시 로그 출력
    logMetrics(metrics);

    return metrics;
  }

  /** 메트릭 로그 출력 */
  private void logMetrics(PerformanceMetrics metrics) {
    log.info("========================================");
    log.info("성능 테스트 결과: {}", metrics.getTestName());
    log.info("========================================");
    log.info("총 요청 수: {}", metrics.getTotalRequests());
    log.info("성공: {} | 실패: {}", metrics.getSuccessfulRequests(), metrics.getFailedRequests());
    log.info("----------------------------------------");
    log.info("응답 시간 (ms):");
    log.info("  Min: {:.2f}", metrics.getMin());
    log.info("  Max: {:.2f}", metrics.getMax());
    log.info("  Mean: {:.2f}", metrics.getMean());
    log.info("  P50: {:.2f}", metrics.getP50());
    log.info("  P95: {:.2f}", metrics.getP95());
    log.info("  P99: {:.2f}", metrics.getP99());
    log.info("----------------------------------------");
    log.info("처리량: {:.2f} req/sec", metrics.getThroughput());
    log.info("성능 등급: {} ({})", metrics.getGrade(), metrics.getGrade().getDescription());
    log.info("========================================");
  }

  /** Hibernate 통계 기반 N+1 감지 */
  public void detectN1Queries(SessionFactory sessionFactory, String testName) {
    Statistics stats = sessionFactory.getStatistics();

    long queryCount = stats.getQueryExecutionCount();
    long entityLoadCount = stats.getEntityLoadCount();
    long collectionLoadCount = stats.getCollectionLoadCount();

    double ratio = entityLoadCount > 0 ? (double) queryCount / entityLoadCount : 0;

    log.info("========================================");
    log.info("N+1 쿼리 감지: {}", testName);
    log.info("========================================");
    log.info("실행된 쿼리 수: {}", queryCount);
    log.info("로드된 엔티티 수: {}", entityLoadCount);
    log.info("로드된 컬렉션 수: {}", collectionLoadCount);
    log.info("쿼리/엔티티 비율: {:.2f}", ratio);

    if (ratio > 2.0) {
      log.warn("⚠️ N+1 쿼리 가능성 감지! (비율: {:.2f})", ratio);
    } else {
      log.info("✅ N+1 쿼리 없음");
    }
    log.info("========================================");

    stats.clear();
  }

  /** 전체 테스트 요약 리포트 */
  public void generateSummaryReport() {
    log.info("========================================");
    log.info("전체 성능 테스트 요약");
    log.info("========================================");

    for (String testName : testExecutionOrder) {
      PerformanceMetrics metrics = metricsMap.get(testName);
      if (metrics != null) {
        log.info(metrics.getSummary());
      }
    }

    // 전체 통계
    long totalRequests =
        metricsMap.values().stream().mapToLong(PerformanceMetrics::getTotalRequests).sum();
    long totalSuccess =
        metricsMap.values().stream().mapToLong(PerformanceMetrics::getSuccessfulRequests).sum();
    long totalFailed =
        metricsMap.values().stream().mapToLong(PerformanceMetrics::getFailedRequests).sum();

    log.info("========================================");
    log.info("전체 통계:");
    log.info("  총 테스트: {}", metricsMap.size());
    log.info("  총 요청: {}", totalRequests);
    log.info("  성공: {}", totalSuccess);
    log.info("  실패: {}", totalFailed);
    log.info("  성공률: {:.2f}%", totalSuccess * 100.0 / totalRequests);
    log.info("========================================");

    // 등급별 분포
    Map<PerformanceMetrics.Grade, Long> gradeDistribution = new ConcurrentHashMap<>();
    metricsMap.values().forEach(m -> gradeDistribution.merge(m.getGrade(), 1L, Long::sum));

    log.info("성능 등급 분포:");
    for (PerformanceMetrics.Grade grade : PerformanceMetrics.Grade.values()) {
      long count = gradeDistribution.getOrDefault(grade, 0L);
      if (count > 0) {
        log.info("  {} ({}): {} 테스트", grade, grade.getDescription(), count);
      }
    }
    log.info("========================================");
  }

  /** 메트릭 초기화 */
  public void reset() {
    responseTimesMap.clear();
    metricsMap.clear();
    testExecutionOrder.clear();
  }

  /** 특정 테스트의 메트릭 조회 */
  public PerformanceMetrics getMetrics(String testName) {
    return metricsMap.get(testName);
  }

  /** 모든 메트릭 조회 */
  public Map<String, PerformanceMetrics> getAllMetrics() {
    return new ConcurrentHashMap<>(metricsMap);
  }
}

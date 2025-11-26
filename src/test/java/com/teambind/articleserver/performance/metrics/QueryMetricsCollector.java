package com.teambind.articleserver.performance.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 쿼리 성능 메트릭 수집기
 *
 * <p>Thread-safe하게 여러 스레드에서 동시에 메트릭을 수집할 수 있음
 */
@Component
@Slf4j
public class QueryMetricsCollector {

  private final ConcurrentLinkedQueue<Long> responseTimes = new ConcurrentLinkedQueue<>();
  private final ConcurrentLinkedQueue<Long> queryCounts = new ConcurrentLinkedQueue<>();
  private final AtomicInteger totalRequests = new AtomicInteger(0);
  private final AtomicInteger errorCount = new AtomicInteger(0);
  private final AtomicLong totalResponseTimeNanos = new AtomicLong(0);
  private final AtomicLong totalQueryCount = new AtomicLong(0);

  /** 응답 시간 기록 (나노초 단위) */
  public void recordResponseTime(long nanos) {
    responseTimes.offer(nanos);
    totalResponseTimeNanos.addAndGet(nanos);
    totalRequests.incrementAndGet();
  }

  /** 쿼리 실행 횟수 기록 */
  public void recordQueryCount(long count) {
    queryCounts.offer(count);
    totalQueryCount.addAndGet(count);
  }

  /** 에러 기록 */
  public void recordError() {
    errorCount.incrementAndGet();
    totalRequests.incrementAndGet();
  }

  /** 메트릭 초기화 */
  public void reset() {
    responseTimes.clear();
    queryCounts.clear();
    totalRequests.set(0);
    errorCount.set(0);
    totalResponseTimeNanos.set(0);
    totalQueryCount.set(0);
    log.debug("메트릭 수집기 초기화됨");
  }

  /** 수집된 메트릭 분석 및 반환 */
  public PerformanceMetrics getMetrics() {
    List<Long> sortedResponseTimes = new ArrayList<>(responseTimes);
    Collections.sort(sortedResponseTimes);

    List<Long> sortedQueryCounts = new ArrayList<>(queryCounts);
    Collections.sort(sortedQueryCounts);

    PerformanceMetrics metrics = new PerformanceMetrics();

    // 응답 시간 분석 (나노초 -> 밀리초 변환)
    if (!sortedResponseTimes.isEmpty()) {
      metrics.setP50ResponseTime(calculatePercentile(sortedResponseTimes, 50) / 1_000_000.0);
      metrics.setP95ResponseTime(calculatePercentile(sortedResponseTimes, 95) / 1_000_000.0);
      metrics.setP99ResponseTime(calculatePercentile(sortedResponseTimes, 99) / 1_000_000.0);
      metrics.setMinResponseTime(sortedResponseTimes.get(0) / 1_000_000.0);
      metrics.setMaxResponseTime(
          sortedResponseTimes.get(sortedResponseTimes.size() - 1) / 1_000_000.0);
      metrics.setAverageResponseTime(
          totalRequests.get() > 0
              ? (totalResponseTimeNanos.get() / totalRequests.get()) / 1_000_000.0
              : 0);
    }

    // 쿼리 카운트 분석
    if (!sortedQueryCounts.isEmpty()) {
      metrics.setAverageQueryCount(
          sortedQueryCounts.stream().mapToLong(Long::longValue).average().orElse(0));
      metrics.setMaxQueryCount(sortedQueryCounts.get(sortedQueryCounts.size() - 1));
      metrics.setMinQueryCount(sortedQueryCounts.get(0));
    }

    // 기타 메트릭
    metrics.setTotalRequests(totalRequests.get());
    metrics.setTotalQueries(totalQueryCount.get());
    metrics.setErrorCount(errorCount.get());
    metrics.setErrorRate(
        totalRequests.get() > 0 ? (double) errorCount.get() / totalRequests.get() : 0);

    // 처리량 계산 (요청/초)
    if (!sortedResponseTimes.isEmpty()) {
      long totalTimeNanos = sortedResponseTimes.stream().mapToLong(Long::longValue).sum();
      double totalTimeSeconds = totalTimeNanos / 1_000_000_000.0;
      metrics.setThroughput(
          totalTimeSeconds > 0 ? sortedResponseTimes.size() / totalTimeSeconds : 0);
    }

    return metrics;
  }

  /** 백분위수 계산 */
  private long calculatePercentile(List<Long> sortedValues, int percentile) {
    if (sortedValues.isEmpty()) {
      return 0;
    }

    int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
    index = Math.max(0, Math.min(index, sortedValues.size() - 1));
    return sortedValues.get(index);
  }

  /** 현재 수집 상태 출력 */
  public void printCurrentStats() {
    log.info("현재 수집 상태:");
    log.info("  - 총 요청 수: {}", totalRequests.get());
    log.info("  - 에러 수: {}", errorCount.get());
    log.info("  - 응답 시간 샘플 수: {}", responseTimes.size());
    log.info("  - 쿼리 카운트 샘플 수: {}", queryCounts.size());
  }
}

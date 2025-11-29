package com.teambind.articleserver.performance.metrics;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/** 성능 측정 메트릭 */
@Data
@Builder
public class PerformanceMetrics {

  private String testName;
  private long totalRequests;
  private long successfulRequests;
  private long failedRequests;

  // 응답 시간 메트릭 (밀리초)
  private double min;
  private double max;
  private double mean;
  private double median;
    private double p50;
    private double p95;
    private double p99;

  // 처리량 메트릭
  private double throughput; // requests per second
  private double avgResponseTime;

  // 원본 데이터
  private List<Long> responseTimes;

  // 성능 등급
  private Grade grade;

  /** 응답 시간 리스트로부터 메트릭 계산 */
  public static PerformanceMetrics calculate(String testName, List<Long> responseTimes) {
    if (responseTimes == null || responseTimes.isEmpty()) {
      return PerformanceMetrics.builder().testName(testName).totalRequests(0).build();
    }

    Collections.sort(responseTimes);

    int size = responseTimes.size();
    double sum = responseTimes.stream().mapToLong(Long::longValue).sum();

    double min = responseTimes.get(0);
    double max = responseTimes.get(size - 1);
    double mean = sum / size;
    double median = getPercentile(responseTimes, 50);
    double p50 = median;
    double p95 = getPercentile(responseTimes, 95);
    double p99 = getPercentile(responseTimes, 99);

    // 전체 실행 시간 (첫 요청부터 마지막 요청까지)
    double totalTimeSeconds = sum / 1000.0;
    double throughput = totalTimeSeconds > 0 ? size / totalTimeSeconds : 0;

    Grade grade = calculateGrade(p95);

    return PerformanceMetrics.builder()
        .testName(testName)
        .totalRequests(size)
        .successfulRequests(size)
        .failedRequests(0)
        .min(min)
        .max(max)
        .mean(mean)
        .median(median)
        .p50(p50)
        .p95(p95)
        .p99(p99)
        .throughput(throughput)
        .avgResponseTime(mean)
        .responseTimes(responseTimes)
        .grade(grade)
        .build();
  }

  /** 백분위수 계산 */
  private static double getPercentile(List<Long> sortedList, int percentile) {
    if (sortedList.isEmpty()) return 0;

    int index = (int) Math.ceil(percentile / 100.0 * sortedList.size()) - 1;
    index = Math.max(0, Math.min(index, sortedList.size() - 1));
    return sortedList.get(index);
  }

  /** 성능 등급 계산 (P95 기준) */
  private static Grade calculateGrade(double p95) {
    if (p95 < 10) return Grade.S;
    if (p95 < 20) return Grade.A;
    if (p95 < 50) return Grade.B;
    if (p95 < 100) return Grade.C;
    return Grade.D;
  }

  /** 메트릭 요약 문자열 */
  public String getSummary() {
    return String.format(
        "[%s] Total: %d | Success: %d | Failed: %d | "
            + "P50: %.2fms | P95: %.2fms | P99: %.2fms | "
            + "TPS: %.2f | Grade: %s",
        testName,
        totalRequests,
        successfulRequests,
        failedRequests,
        p50,
        p95,
        p99,
        throughput,
        grade);
  }

  /** 성능 등급 */
  public enum Grade {
    S("최우수", "P95 < 10ms"),
    A("우수", "P95 < 20ms"),
    B("양호", "P95 < 50ms"),
    C("보통", "P95 < 100ms"),
    D("개선필요", "P95 >= 100ms");

    private final String description;
    private final String criteria;

    Grade(String description, String criteria) {
      this.description = description;
      this.criteria = criteria;
    }

    public String getDescription() {
      return description;
    }

    public String getCriteria() {
      return criteria;
    }
  }
}

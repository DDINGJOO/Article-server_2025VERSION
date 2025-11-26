package com.teambind.articleserver.performance.metrics;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/** 성능 측정 메트릭 데이터 클래스 */
@Data
@Slf4j
public class PerformanceMetrics {

  // 응답 시간 메트릭 (밀리초 단위)
  @JsonProperty("response_time_ms")
  private ResponseTimeMetrics responseTime = new ResponseTimeMetrics();

  // 쿼리 실행 메트릭
  @JsonProperty("query_metrics")
  private QueryMetrics query = new QueryMetrics();

  // 처리량 메트릭
  @JsonProperty("throughput_metrics")
  private ThroughputMetrics throughput = new ThroughputMetrics();

  // 에러 메트릭
  @JsonProperty("error_metrics")
  private ErrorMetrics error = new ErrorMetrics();

  // 메타데이터
  @JsonProperty("metadata")
  private MetricsMetadata metadata = new MetricsMetadata();

  public void setMinResponseTime(double value) {
    this.responseTime.min = value;
  }

  public void setAverageResponseTime(double value) {
    this.responseTime.average = value;
  }

  public void setMinQueryCount(long value) {
    this.query.minCount = value;
  }

  public void setThroughput(double value) {
    this.throughput.requestsPerSecond = value;
  }

  // 단순 getter들
  public double getP50ResponseTime() {
    return this.responseTime.p50;
  }

  // 단순 setter들 (기존 코드 호환성을 위해)
  public void setP50ResponseTime(double value) {
    this.responseTime.p50 = value;
  }

  public double getP95ResponseTime() {
    return this.responseTime.p95;
  }

  public void setP95ResponseTime(double value) {
    this.responseTime.p95 = value;
  }

  public double getP99ResponseTime() {
    return this.responseTime.p99;
  }

  public void setP99ResponseTime(double value) {
    this.responseTime.p99 = value;
  }

  public double getMaxResponseTime() {
    return this.responseTime.max;
  }

  public void setMaxResponseTime(double value) {
    this.responseTime.max = value;
  }

  public double getAverageQueryCount() {
    return this.query.averageCount;
  }

  public void setAverageQueryCount(double value) {
    this.query.averageCount = value;
  }

  public long getMaxQueryCount() {
    return this.query.maxCount;
  }

  public void setMaxQueryCount(long value) {
    this.query.maxCount = value;
  }

  public long getTotalQueries() {
    return this.query.totalCount;
  }

  public void setTotalQueries(long value) {
    this.query.totalCount = value;
  }

  public int getTotalRequests() {
    return this.throughput.totalRequests;
  }

  public void setTotalRequests(int value) {
    this.throughput.totalRequests = value;
  }

  public int getErrorCount() {
    return this.error.count;
  }

  public void setErrorCount(int value) {
    this.error.count = value;
  }

  public double getErrorRate() {
    return this.error.rate;
  }

  public void setErrorRate(double value) {
    this.error.rate = value;
  }

  /** JSON 파일로 저장 */
  public void saveToJson(String scenario) {
    this.metadata.scenario = scenario;
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    String fileName =
        String.format("performance-metrics-%s-%s.json", scenario.replaceAll(" ", "_"), timestamp);

    try {
      Path dir = Paths.get("performance-results");
      Files.createDirectories(dir);
      File file = dir.resolve(fileName).toFile();

      ObjectMapper mapper = new ObjectMapper();
      mapper.enable(SerializationFeature.INDENT_OUTPUT);
      mapper.writeValue(file, this);

      log.info("메트릭 저장 완료: {}", file.getAbsolutePath());
    } catch (IOException e) {
      log.error("메트릭 저장 실패", e);
    }
  }

  /** CSV 파일로 저장 */
  public void saveToCsv(String scenario) {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    String fileName =
        String.format("performance-metrics-%s-%s.csv", scenario.replaceAll(" ", "_"), timestamp);

    try {
      Path dir = Paths.get("performance-results");
      Files.createDirectories(dir);
      File file = dir.resolve(fileName).toFile();

      try (FileWriter writer = new FileWriter(file)) {
        // CSV 헤더
        writer.write(
            "Timestamp,Scenario,P50(ms),P95(ms),P99(ms),Avg Query Count,Max Query Count,Total Requests,Error Rate(%),Throughput(req/s)\n");

        // 데이터
        writer.write(
            String.format(
                "%s,%s,%.2f,%.2f,%.2f,%.2f,%d,%d,%.2f,%.2f\n",
                metadata.timestamp,
                scenario,
                responseTime.p50,
                responseTime.p95,
                responseTime.p99,
                query.averageCount,
                query.maxCount,
                throughput.totalRequests,
                error.rate * 100,
                throughput.requestsPerSecond));
      }

      log.info("CSV 저장 완료: {}", file.getAbsolutePath());
    } catch (IOException e) {
      log.error("CSV 저장 실패", e);
    }
  }

  /** Markdown 형식으로 리포트 생성 */
  public String toMarkdownReport(String scenario) {
    StringBuilder sb = new StringBuilder();
    sb.append("## Performance Test Report: ").append(scenario).append("\n\n");
    sb.append("**Timestamp**: ").append(metadata.timestamp).append("\n\n");

    sb.append("### Response Time Metrics\n");
    sb.append("| Metric | Value |\n");
    sb.append("|--------|-------|\n");
    sb.append(String.format("| P50 | %.2f ms |\n", responseTime.p50));
    sb.append(String.format("| P95 | %.2f ms |\n", responseTime.p95));
    sb.append(String.format("| P99 | %.2f ms |\n", responseTime.p99));
    sb.append(String.format("| Min | %.2f ms |\n", responseTime.min));
    sb.append(String.format("| Max | %.2f ms |\n", responseTime.max));
    sb.append(String.format("| Average | %.2f ms |\n", responseTime.average));
    sb.append("\n");

    sb.append("### Query Execution Metrics\n");
    sb.append("| Metric | Value |\n");
    sb.append("|--------|-------|\n");
    sb.append(String.format("| Average Queries/Request | %.2f |\n", query.averageCount));
    sb.append(String.format("| Min Queries | %d |\n", query.minCount));
    sb.append(String.format("| Max Queries | %d |\n", query.maxCount));
    sb.append(String.format("| Total Queries | %d |\n", query.totalCount));
    sb.append("\n");

    sb.append("### Throughput & Error Metrics\n");
    sb.append("| Metric | Value |\n");
    sb.append("|--------|-------|\n");
    sb.append(String.format("| Throughput | %.2f req/s |\n", throughput.requestsPerSecond));
    sb.append(String.format("| Total Requests | %d |\n", throughput.totalRequests));
    sb.append(String.format("| Error Count | %d |\n", error.count));
    sb.append(String.format("| Error Rate | %.2f%% |\n", error.rate * 100));

    return sb.toString();
  }

  /** 성능 목표 대비 검증 */
  public boolean meetsPerformanceGoals(double targetP95, double targetP99, long maxQueriesAllowed) {
    boolean p95Goal = responseTime.p95 <= targetP95;
    boolean p99Goal = responseTime.p99 <= targetP99;
    boolean queryGoal = query.maxCount <= maxQueriesAllowed;

    log.info("=== 성능 목표 검증 ===");
    log.info(
        "P95 목표: {}ms, 실제: {:.2f}ms - {}", targetP95, responseTime.p95, p95Goal ? "PASS" : "FAIL");
    log.info(
        "P99 목표: {}ms, 실제: {:.2f}ms - {}", targetP99, responseTime.p99, p99Goal ? "PASS" : "FAIL");
    log.info(
        "최대 쿼리 목표: {}, 실제: {} - {}",
        maxQueriesAllowed,
        query.maxCount,
        queryGoal ? "PASS" : "FAIL");

    return p95Goal && p99Goal && queryGoal;
  }

  @Data
  public static class ResponseTimeMetrics {
    private double p50;
    private double p95;
    private double p99;
    private double min;
    private double max;
    private double average;
  }

  @Data
  public static class QueryMetrics {
    private double averageCount;
    private long minCount;
    private long maxCount;
    private long totalCount;
  }

  @Data
  public static class ThroughputMetrics {
    private double requestsPerSecond;
    private int totalRequests;
  }

  @Data
  public static class ErrorMetrics {
    private int count;
    private double rate;
  }

  @Data
  public static class MetricsMetadata {
    private String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    private String scenario;
    private String environment = "performance-test";
  }
}

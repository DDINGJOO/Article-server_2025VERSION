package com.teambind.articleserver.performance.report;

import com.teambind.articleserver.performance.metrics.PerformanceMetrics;
import com.teambind.articleserver.performance.metrics.PerformanceMetricsCollector;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 성능 테스트 리포트 생성기 HTML, Markdown 형식으로 리포트 생성 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PerformanceReportGenerator {

  private static final String REPORT_DIR = "performance-reports";
  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
  private final PerformanceMetricsCollector metricsCollector;

  /** HTML 리포트 생성 */
  public void generateHTMLReport() {
    String timestamp = LocalDateTime.now().format(DATE_FORMAT);
    String fileName = String.format("%s/report_%s.html", REPORT_DIR, timestamp);

    try {
      createReportDirectory();
      Map<String, PerformanceMetrics> allMetrics = metricsCollector.getAllMetrics();

      StringBuilder html = new StringBuilder();
      html.append("<!DOCTYPE html>\n");
      html.append("<html>\n<head>\n");
      html.append("<title>Performance Test Report - ").append(timestamp).append("</title>\n");
      html.append("<style>\n");
      html.append(getCSS());
      html.append("</style>\n");
      html.append("</head>\n<body>\n");

      // Header
      html.append("<h1>Performance Test Report</h1>\n");
      html.append("<div class='timestamp'>Generated: ")
          .append(LocalDateTime.now())
          .append("</div>\n");

      // Summary
      html.append("<div class='summary'>\n");
      html.append("<h2>Summary</h2>\n");
      html.append("<table>\n");
      html.append("<tr><th>Total Tests</th><td>").append(allMetrics.size()).append("</td></tr>\n");
      html.append("<tr><th>Total Requests</th><td>")
          .append(
              allMetrics.values().stream().mapToLong(PerformanceMetrics::getTotalRequests).sum())
          .append("</td></tr>\n");
      html.append("</table>\n");
      html.append("</div>\n");

      // Test Results
      html.append("<h2>Test Results</h2>\n");
      html.append("<table class='results'>\n");
      html.append("<thead>\n");
      html.append("<tr>\n");
      html.append("<th>Test Name</th>\n");
      html.append("<th>Requests</th>\n");
      html.append("<th>P50 (ms)</th>\n");
      html.append("<th>P95 (ms)</th>\n");
      html.append("<th>P99 (ms)</th>\n");
      html.append("<th>TPS</th>\n");
      html.append("<th>Grade</th>\n");
      html.append("</tr>\n");
      html.append("</thead>\n");
      html.append("<tbody>\n");

      for (PerformanceMetrics metrics : allMetrics.values()) {
        String gradeClass = "grade-" + metrics.getGrade().name().toLowerCase();
        html.append("<tr>\n");
        html.append("<td>").append(metrics.getTestName()).append("</td>\n");
        html.append("<td>").append(metrics.getTotalRequests()).append("</td>\n");
        html.append("<td>").append(String.format("%.2f", metrics.getP50())).append("</td>\n");
        html.append("<td>").append(String.format("%.2f", metrics.getP95())).append("</td>\n");
        html.append("<td>").append(String.format("%.2f", metrics.getP99())).append("</td>\n");
        html.append("<td>")
            .append(String.format("%.2f", metrics.getThroughput()))
            .append("</td>\n");
        html.append("<td class='")
            .append(gradeClass)
            .append("'>")
            .append(metrics.getGrade())
            .append("</td>\n");
        html.append("</tr>\n");
      }

      html.append("</tbody>\n");
      html.append("</table>\n");
      html.append("</body>\n</html>");

      try (FileWriter writer = new FileWriter(fileName)) {
        writer.write(html.toString());
      }

      log.info("HTML 리포트 생성 완료: {}", fileName);

    } catch (IOException e) {
      log.error("HTML 리포트 생성 실패", e);
    }
  }

  /** Markdown 리포트 생성 */
  public void generateMarkdownReport() {
    String timestamp = LocalDateTime.now().format(DATE_FORMAT);
    String fileName = String.format("%s/report_%s.md", REPORT_DIR, timestamp);

    try {
      createReportDirectory();
      Map<String, PerformanceMetrics> allMetrics = metricsCollector.getAllMetrics();

      StringBuilder md = new StringBuilder();

      // Header
      md.append("# Performance Test Report\n\n");
      md.append("Generated: ").append(LocalDateTime.now()).append("\n\n");

      // Summary
      md.append("## Summary\n\n");
      md.append("- **Total Tests**: ").append(allMetrics.size()).append("\n");
      md.append("- **Total Requests**: ")
          .append(
              allMetrics.values().stream().mapToLong(PerformanceMetrics::getTotalRequests).sum())
          .append("\n\n");

      // Test Results
      md.append("## Test Results\n\n");
      md.append("| Test Name | Requests | P50 (ms) | P95 (ms) | P99 (ms) | TPS | Grade |\n");
      md.append("|-----------|----------|----------|----------|----------|-----|-------|\n");

      for (PerformanceMetrics metrics : allMetrics.values()) {
        md.append("| ").append(metrics.getTestName());
        md.append(" | ").append(metrics.getTotalRequests());
        md.append(" | ").append(String.format("%.2f", metrics.getP50()));
        md.append(" | ").append(String.format("%.2f", metrics.getP95()));
        md.append(" | ").append(String.format("%.2f", metrics.getP99()));
        md.append(" | ").append(String.format("%.2f", metrics.getThroughput()));
        md.append(" | **").append(metrics.getGrade()).append("** |\n");
      }

      // Grade Distribution
      md.append("\n## Performance Grade Distribution\n\n");
      Map<PerformanceMetrics.Grade, Long> gradeCount = new java.util.HashMap<>();
      allMetrics.values().forEach(m -> gradeCount.merge(m.getGrade(), 1L, Long::sum));

      for (PerformanceMetrics.Grade grade : PerformanceMetrics.Grade.values()) {
        long count = gradeCount.getOrDefault(grade, 0L);
        if (count > 0) {
          md.append("- **")
              .append(grade)
              .append("** (")
              .append(grade.getDescription())
              .append("): ")
              .append(count)
              .append(" tests\n");
        }
      }

      // Grade Criteria
      md.append("\n## Grade Criteria\n\n");
      for (PerformanceMetrics.Grade grade : PerformanceMetrics.Grade.values()) {
        md.append("- **").append(grade).append("**: ").append(grade.getCriteria()).append("\n");
      }

      try (FileWriter writer = new FileWriter(fileName)) {
        writer.write(md.toString());
      }

      log.info("Markdown 리포트 생성 완료: {}", fileName);

    } catch (IOException e) {
      log.error("Markdown 리포트 생성 실패", e);
    }
  }

  /** 리포트 디렉토리 생성 */
  private void createReportDirectory() throws IOException {
    Path reportPath = Paths.get(REPORT_DIR);
    if (!Files.exists(reportPath)) {
      Files.createDirectories(reportPath);
    }
  }

  /** HTML 스타일 정의 */
  private String getCSS() {
    return """
            body {
                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                margin: 20px;
                background-color: #f5f5f5;
            }
            h1 { color: #333; border-bottom: 2px solid #007bff; padding-bottom: 10px; }
            h2 { color: #555; margin-top: 30px; }
            .timestamp { color: #888; font-size: 14px; margin-bottom: 20px; }
            .summary {
                background-color: white;
                padding: 20px;
                border-radius: 8px;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                margin-bottom: 30px;
            }
            table {
                width: 100%;
                border-collapse: collapse;
                background-color: white;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            }
            th, td {
                padding: 12px;
                text-align: left;
                border-bottom: 1px solid #ddd;
            }
            th {
                background-color: #007bff;
                color: white;
                font-weight: bold;
            }
            tr:hover { background-color: #f5f5f5; }
            .grade-s { color: #28a745; font-weight: bold; }
            .grade-a { color: #5cb85c; font-weight: bold; }
            .grade-b { color: #f0ad4e; font-weight: bold; }
            .grade-c { color: #d9534f; font-weight: bold; }
            .grade-d { color: #c9302c; font-weight: bold; }
            """;
  }
}

package com.teambind.articleserver.performance;

import static org.assertj.core.api.Assertions.assertThat;

import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import com.teambind.articleserver.adapter.out.persistence.repository.ArticleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Detailed performance test measuring in microseconds (μs) and nanoseconds to detect cache effects
 * more accurately
 */
@SpringBootTest
@ActiveProfiles("performance-test")
@Slf4j
public class DetailedPerformanceTest {

  @Autowired private ArticleRepository articleRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  @PersistenceContext private EntityManager entityManager;

  @Test
  @Transactional
  void detailedCacheComparison() {
    log.info("=================================================");
    log.info("DETAILED CACHE PERFORMANCE ANALYSIS");
    log.info("Measuring in microseconds (μs) for better precision");
    log.info("=================================================\n");

    // Get test data
    List<String> articleIds =
        jdbcTemplate.queryForList("SELECT article_id FROM articles LIMIT 100", String.class);

    if (articleIds.size() < 10) {
      log.warn("Not enough test data");
      return;
    }

    // Test 1: First Load (Cold Cache)
    log.info("TEST 1: COLD CACHE (First Load)");
    log.info("---------------------------------");
    List<Long> coldCacheTimes = new ArrayList<>();
    entityManager.clear();

    for (int i = 0; i < 20; i++) {
      String id = articleIds.get(i);
      entityManager.clear(); // Ensure cache is cleared

      long start = System.nanoTime();
      Optional<Article> article = articleRepository.findById(id);
      long end = System.nanoTime();

      assertThat(article).isPresent();
      coldCacheTimes.add(end - start);
    }

    logDetailedMetrics(coldCacheTimes, "COLD CACHE (Database Hit)");

    // Test 2: Warm Cache (Same entities)
    log.info("\nTEST 2: WARM CACHE (Second Load)");
    log.info("---------------------------------");
    List<Long> warmCacheTimes = new ArrayList<>();

    // Load all entities first
    for (int i = 0; i < 20; i++) {
      articleRepository.findById(articleIds.get(i));
    }

    // Now measure cached reads
    for (int i = 0; i < 20; i++) {
      String id = articleIds.get(i);

      long start = System.nanoTime();
      Optional<Article> article = articleRepository.findById(id);
      long end = System.nanoTime();

      assertThat(article).isPresent();
      warmCacheTimes.add(end - start);
    }

    logDetailedMetrics(warmCacheTimes, "WARM CACHE (Memory Hit)");

    // Test 3: JDBC Direct (No JPA overhead)
    log.info("\nTEST 3: JDBC DIRECT (Baseline)");
    log.info("---------------------------------");
    List<Long> jdbcTimes = new ArrayList<>();

    for (int i = 0; i < 20; i++) {
      String id = articleIds.get(i);

      long start = System.nanoTime();
      List<Map<String, Object>> results =
          jdbcTemplate.queryForList("SELECT * FROM articles WHERE article_id = ?", id);
      long end = System.nanoTime();

      assertThat(results).isNotEmpty();
      jdbcTimes.add(end - start);
    }

    logDetailedMetrics(jdbcTimes, "JDBC DIRECT");

    // Test 4: Bulk operations comparison
    log.info("\nTEST 4: BULK OPERATIONS");
    log.info("---------------------------------");

    // JPA bulk load
    entityManager.clear();
    long jpaStart = System.nanoTime();
    List<Article> jpaArticles = articleRepository.findAllById(articleIds.subList(0, 50));
    long jpaEnd = System.nanoTime();
    long jpaBulkTime = jpaEnd - jpaStart;

    // JDBC bulk load
    long jdbcStart = System.nanoTime();
    String inClause = String.join(",", Collections.nCopies(50, "?"));
    jdbcTemplate.queryForList(
        "SELECT * FROM articles WHERE article_id IN (" + inClause + ")",
        articleIds.subList(0, 50).toArray());
    long jdbcEnd = System.nanoTime();
    long jdbcBulkTime = jdbcEnd - jdbcStart;

    log.info("Bulk Load (50 records):");
    log.info("  JPA:  {} μs ({} ms)", jpaBulkTime / 1000, jpaBulkTime / 1_000_000);
    log.info("  JDBC: {} μs ({} ms)", jdbcBulkTime / 1000, jdbcBulkTime / 1_000_000);
    log.info(
        "  JPA overhead: {}%",
        String.format("%.1f", ((double) (jpaBulkTime - jdbcBulkTime) / jdbcBulkTime) * 100));

    // Summary
    log.info("\n=================================================");
    log.info("PERFORMANCE SUMMARY");
    log.info("=================================================");

    double coldAvg = coldCacheTimes.stream().mapToLong(Long::longValue).average().orElse(0);
    double warmAvg = warmCacheTimes.stream().mapToLong(Long::longValue).average().orElse(0);
    double jdbcAvg = jdbcTimes.stream().mapToLong(Long::longValue).average().orElse(0);

    log.info("Average Response Times:");
    log.info("  Cold Cache (DB):   {} μs", String.format("%.2f", coldAvg / 1000));
    log.info("  Warm Cache (Mem):  {} μs", String.format("%.2f", warmAvg / 1000));
    log.info("  JDBC Direct:       {} μs", String.format("%.2f", jdbcAvg / 1000));

    if (warmAvg > 0) {
      log.info("\nCache Performance Gain:");
      log.info("  Speed improvement: {}x faster", String.format("%.1f", coldAvg / warmAvg));
      log.info("  Time saved per query: {} μs", String.format("%.2f", (coldAvg - warmAvg) / 1000));
    }

    if (jdbcAvg > 0) {
      log.info("\nJPA Overhead:");
      log.info("  Cold cache vs JDBC: {}x slower", String.format("%.1f", coldAvg / jdbcAvg));
      log.info("  Warm cache vs JDBC: {}x", String.format("%.1f", warmAvg / jdbcAvg));
    }

    // Check if caching is actually working
    if (warmAvg >= coldAvg * 0.9) {
      log.warn("\n⚠️ WARNING: Cache doesn't seem to be providing benefits!");
      log.warn("Warm cache is not significantly faster than cold cache.");
      log.warn("This could indicate:");
      log.warn("  1. Queries are too fast for cache to matter");
      log.warn("  2. Cache is not working properly");
      log.warn("  3. Database is already caching effectively");
    } else {
      log.info("\n✅ Cache is working effectively!");
      log.info(
          "Warm cache is {}% faster than cold cache",
          String.format("%.0f", ((coldAvg - warmAvg) / coldAvg) * 100));
    }

    log.info("=================================================\n");
  }

  private void logDetailedMetrics(List<Long> nanoseconds, String testName) {
    if (nanoseconds.isEmpty()) return;

    Collections.sort(nanoseconds);
    int size = nanoseconds.size();

    // Convert to microseconds for better readability
    List<Double> microseconds = nanoseconds.stream().map(n -> n / 1000.0).toList();

    double min = microseconds.get(0);
    double max = microseconds.get(size - 1);
    double avg = microseconds.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    double p50 = microseconds.get(size / 2);
    double p95 = microseconds.get(Math.min(size - 1, size * 95 / 100));
    double p99 = microseconds.get(Math.min(size - 1, size * 99 / 100));

    log.info("{}:", testName);
    log.info("  Samples: {}", size);
    log.info("  Min:     {} μs", String.format("%8.2f", min));
    log.info("  Max:     {} μs", String.format("%8.2f", max));
    log.info("  Average: {} μs", String.format("%8.2f", avg));
    log.info("  P50:     {} μs", String.format("%8.2f", p50));
    log.info("  P95:     {} μs", String.format("%8.2f", p95));
    log.info("  P99:     {} μs", String.format("%8.2f", p99));

    // Also show in milliseconds for reference
    if (avg > 1000) {
      log.info("  (Average in ms: {})", String.format("%.3f", avg / 1000));
    }
  }
}

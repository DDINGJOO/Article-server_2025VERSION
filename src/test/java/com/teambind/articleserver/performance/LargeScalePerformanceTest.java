package com.teambind.articleserver.performance;

import static org.assertj.core.api.Assertions.assertThat;

import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import com.teambind.articleserver.adapter.out.persistence.entity.board.Board;
import com.teambind.articleserver.adapter.out.persistence.repository.ArticleRepository;
import com.teambind.articleserver.adapter.out.persistence.repository.BoardRepository;
import com.teambind.articleserver.performance.framework.PerformanceTestBase;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Large-scale performance test with 600K articles */
@SpringBootTest
@ActiveProfiles("performance-test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
public class LargeScalePerformanceTest extends PerformanceTestBase {

  private static final int TARGET_ARTICLE_COUNT = 600_000;
  private static final int BATCH_SIZE = 5000;
  private static final int THREAD_POOL_SIZE = 8;
  private final Random random = new Random();
  @Autowired private ArticleRepository articleRepository;
  @Autowired private BoardRepository boardRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  private List<Board> boards;

  @BeforeAll
  void setupTestData() {
    log.info("========================================");
    log.info("Starting Large-scale Performance Test Setup");
    log.info("Target: {} articles", TARGET_ARTICLE_COUNT);
    log.info("========================================");

    // Load existing boards
    boards = boardRepository.findAll();
    if (boards.isEmpty()) {
      log.error("No boards found in database!");
      throw new IllegalStateException("At least one board must exist in the database");
    }
    log.info("Found {} boards to use for test data", boards.size());

    // Check current article count
    long currentCount = articleRepository.count();
    log.info("Current article count: {}", currentCount);

    if (currentCount < TARGET_ARTICLE_COUNT) {
      log.info("Need to generate {} more articles", TARGET_ARTICLE_COUNT - currentCount);
      generateTestData(TARGET_ARTICLE_COUNT - (int) currentCount);
    } else {
      log.info("Sufficient test data already exists");
    }
  }

  private void generateTestData(int articlesToGenerate) {
    log.info("Generating {} articles using {} threads", articlesToGenerate, THREAD_POOL_SIZE);

    long startTime = System.currentTimeMillis();
    ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    AtomicInteger totalGenerated = new AtomicInteger(0);
    AtomicInteger batchNumber = new AtomicInteger(0);

    try {
      List<Future<Integer>> futures = new ArrayList<>();
      int articlesPerThread = articlesToGenerate / THREAD_POOL_SIZE;
      int remainder = articlesToGenerate % THREAD_POOL_SIZE;

      for (int i = 0; i < THREAD_POOL_SIZE; i++) {
        final int threadArticles = articlesPerThread + (i < remainder ? 1 : 0);
        final int threadId = i;

        futures.add(
            executor.submit(
                () -> {
                  return generateArticlesInBatches(
                      threadId, threadArticles, totalGenerated, batchNumber);
                }));
      }

      // Wait for all threads to complete
      int totalCreated = 0;
      for (Future<Integer> future : futures) {
        try {
          totalCreated += future.get();
        } catch (Exception e) {
          log.error("Error in thread execution", e);
        }
      }

      long endTime = System.currentTimeMillis();
      long duration = endTime - startTime;

      log.info("========================================");
      log.info("Data generation completed");
      log.info("Articles generated: {}", totalCreated);
      log.info("Time taken: {} seconds", duration / 1000);
      log.info("Average rate: {} articles/second", totalCreated * 1000 / duration);
      log.info("========================================");

    } finally {
      executor.shutdown();
      try {
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
          executor.shutdownNow();
        }
      } catch (InterruptedException e) {
        executor.shutdownNow();
      }
    }
  }

  private int generateArticlesInBatches(
      int threadId, int totalArticles, AtomicInteger globalCounter, AtomicInteger batchCounter) {
    int generated = 0;
    List<String> insertStatements = new ArrayList<>();

    for (int i = 0; i < totalArticles; i++) {
      Board board = boards.get(random.nextInt(boards.size()));
      String articleType = getRandomArticleType();
      LocalDateTime now = LocalDateTime.now();

      // Generate INSERT statement directly for better performance
      String sql =
          String.format(
              "INSERT INTO articles (board_id, article_type, title, content, author_id, view_count, like_count, created_at, updated_at) "
                  + "VALUES (%d, '%s', '%s', '%s', %d, %d, %d, '%s', '%s')",
              board.getId(),
              articleType,
              generateTitle(threadId, i),
              generateContent(),
              1L, // author_id
              random.nextInt(10000),
              random.nextInt(1000),
              now.toString(),
              now.toString());

      insertStatements.add(sql);

      // Execute batch when size is reached
      if (insertStatements.size() >= BATCH_SIZE) {
        executeBatch(insertStatements);
        generated += insertStatements.size();

        int currentBatch = batchCounter.incrementAndGet();
        int globalTotal = globalCounter.addAndGet(insertStatements.size());
        if (currentBatch % 10 == 0) {
          log.info(
              "Thread-{}: Batch {} completed. Global progress: {}/{}",
              threadId,
              currentBatch,
              globalTotal,
              totalArticles);
        }

        insertStatements.clear();
      }
    }

    // Execute remaining
    if (!insertStatements.isEmpty()) {
      executeBatch(insertStatements);
      generated += insertStatements.size();
      globalCounter.addAndGet(insertStatements.size());
    }

    return generated;
  }

  private void executeBatch(List<String> statements) {
    try {
      jdbcTemplate.batchUpdate(statements.toArray(new String[0]));
    } catch (Exception e) {
      log.error("Error executing batch insert", e);
    }
  }

  private String getRandomArticleType() {
    double rand = random.nextDouble();
    if (rand < 0.7) return "REGULAR";
    else if (rand < 0.9) return "NOTICE";
    else return "EVENT";
  }

  private String generateTitle(int threadId, int index) {
    String[] prefixes = {"Important", "Update", "News", "Announcement", "Info", "Report"};
    String[] topics = {
      "Development", "Performance", "Testing", "Architecture", "Security", "Database"
    };

    return String.format(
        "[T%d-%d] %s %s #%d",
        threadId,
        index,
        prefixes[random.nextInt(prefixes.length)],
        topics[random.nextInt(topics.length)],
        System.currentTimeMillis() % 10000);
  }

  private String generateContent() {
    String[] sentences = {
      "This is test content for performance testing.",
      "The system should handle large amounts of data efficiently.",
      "Query optimization is crucial for good performance.",
      "Caching strategies can significantly improve response times.",
      "Database indexing plays a vital role in query performance."
    };

    StringBuilder content = new StringBuilder();
    int paragraphs = 3 + random.nextInt(3);
    for (int i = 0; i < paragraphs; i++) {
      int sentenceCount = 3 + random.nextInt(4);
      for (int j = 0; j < sentenceCount; j++) {
        content.append(sentences[random.nextInt(sentences.length)]).append(" ");
      }
      content.append("\n\n");
    }

    return content.toString();
  }

  @Test
  void testSingleArticleReadPerformance() {
    log.info("========================================");
    log.info("Testing single article read performance");
    log.info("========================================");

    // Check if we have enough articles
    long articleCount = articleRepository.count();
    if (articleCount == 0) {
      log.warn("No articles in database, skipping performance test");
      log.warn("Run setupTestData() first or generate test data");
      return;
    }

    // Warm-up
    for (int i = 0; i < 50; i++) {
      articleRepository.count();
    }

    // Test random article reads
    List<Long> responseTimes = new ArrayList<>();
    List<Article> sampleArticles = articleRepository.findAll(PageRequest.of(0, 100)).getContent();

    if (sampleArticles.isEmpty()) {
      log.warn("No sample articles found, skipping test");
      return;
    }

    for (int i = 0; i < Math.min(100, sampleArticles.size() * 2); i++) {
      Article sample = sampleArticles.get(random.nextInt(sampleArticles.size()));

      long startTime = System.nanoTime();
      Optional<Article> article = articleRepository.findById(sample.getId());
      long endTime = System.nanoTime();

      assertThat(article).isPresent();
      responseTimes.add((endTime - startTime) / 1_000_000); // Convert to ms
    }

    // Calculate statistics (only if we have data)
    if (!responseTimes.isEmpty()) {
      Collections.sort(responseTimes);
      int size = responseTimes.size();
      long p50 = responseTimes.get(size * 50 / 100);
      long p95 = responseTimes.get(Math.min(size - 1, size * 95 / 100));
      long p99 = responseTimes.get(Math.min(size - 1, size * 99 / 100));
      double avg = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);

      log.info("Single Article Read Performance:");
      log.info("  Total requests: {}", size);
      log.info("  Average: {:.2f} ms", avg);
      log.info("  P50: {} ms", p50);
      log.info("  P95: {} ms", p95);
      log.info("  P99: {} ms", p99);

      // Assert performance targets
      assertThat(p95).isLessThan(50); // P95 should be less than 50ms
    }
  }

  @Test
  void testPaginationPerformance() {
    log.info("========================================");
    log.info("Testing pagination performance");
    log.info("========================================");

    List<Long> responseTimes = new ArrayList<>();

    // Test different page sizes
    int[] pageSizes = {10, 20, 50, 100};

    for (int pageSize : pageSizes) {
      long startTime = System.currentTimeMillis();
      Page<Article> page = articleRepository.findAll(PageRequest.of(0, pageSize));
      long endTime = System.currentTimeMillis();

      responseTimes.add(endTime - startTime);
      log.info(
          "Page size {}: {} ms ({} articles fetched)",
          pageSize,
          endTime - startTime,
          page.getNumberOfElements());
    }

    // Test deep pagination
    long startTime = System.currentTimeMillis();
    Page<Article> deepPage = articleRepository.findAll(PageRequest.of(1000, 20));
    long endTime = System.currentTimeMillis();

    log.info("Deep pagination (page 1000, size 20): {} ms", endTime - startTime);
    assertThat(endTime - startTime)
        .isLessThan(200); // Should be less than 200ms even for deep pages
  }

  @Test
  void testConcurrentReadPerformance() throws InterruptedException, ExecutionException {
    log.info("========================================");
    log.info("Testing concurrent read performance");
    log.info("========================================");

    // Check if we have enough articles
    long articleCount = articleRepository.count();
    if (articleCount == 0) {
      log.warn("No articles in database, skipping concurrent performance test");
      return;
    }

    int concurrentUsers = 50;
    int requestsPerUser = 20;
    ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);

    List<Article> sampleArticles = articleRepository.findAll(PageRequest.of(0, 1000)).getContent();
    if (sampleArticles.isEmpty()) {
      log.warn("No sample articles found, skipping concurrent test");
      return;
    }
    AtomicLong totalResponseTime = new AtomicLong(0);
    AtomicInteger successCount = new AtomicInteger(0);
    CountDownLatch latch = new CountDownLatch(concurrentUsers);

    long testStartTime = System.currentTimeMillis();

    for (int i = 0; i < concurrentUsers; i++) {
      executor.submit(
          () -> {
            try {
              for (int j = 0; j < requestsPerUser; j++) {
                Article sample = sampleArticles.get(random.nextInt(sampleArticles.size()));

                long startTime = System.nanoTime();
                Optional<Article> article = articleRepository.findById(sample.getId());
                long endTime = System.nanoTime();

                if (article.isPresent()) {
                  successCount.incrementAndGet();
                  totalResponseTime.addAndGet((endTime - startTime) / 1_000_000);
                }
              }
            } finally {
              latch.countDown();
            }
          });
    }

    latch.await(60, TimeUnit.SECONDS);
    executor.shutdown();

    long testEndTime = System.currentTimeMillis();
    long testDuration = testEndTime - testStartTime;
    int totalRequests = concurrentUsers * requestsPerUser;
    double avgResponseTime = totalResponseTime.get() / (double) successCount.get();
    double throughput = (successCount.get() * 1000.0) / testDuration;

    log.info("Concurrent Read Performance:");
    log.info("  Concurrent users: {}", concurrentUsers);
    log.info("  Requests per user: {}", requestsPerUser);
    log.info("  Total requests: {}", totalRequests);
    log.info("  Successful requests: {}", successCount.get());
    log.info("  Average response time: {:.2f} ms", avgResponseTime);
    log.info("  Throughput: {:.2f} requests/second", throughput);
    log.info("  Test duration: {} ms", testDuration);

    assertThat(successCount.get()).isEqualTo(totalRequests);
    assertThat(avgResponseTime).isLessThan(100); // Average should be less than 100ms under load
  }

  @Test
  void testMemoryUsageUnderLoad() {
    log.info("========================================");
    log.info("Testing memory usage under load");
    log.info("========================================");

    Runtime runtime = Runtime.getRuntime();
    runtime.gc(); // Force GC before test

    long initialMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
    log.info("Initial memory usage: {} MB", initialMemory);

    // Perform memory-intensive operations
    List<Article> articles = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      Page<Article> page = articleRepository.findAll(PageRequest.of(i, 1000));
      articles.addAll(page.getContent());
    }

    long peakMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
    log.info("Peak memory usage: {} MB", peakMemory);
    log.info("Memory increase: {} MB", peakMemory - initialMemory);

    // Clear references and force GC
    articles.clear();
    articles = null;
    runtime.gc();
    Thread.yield();
    runtime.gc();

    long finalMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
    log.info("Final memory usage after GC: {} MB", finalMemory);

    // Memory should return close to initial levels after GC
    assertThat(finalMemory).isLessThan(initialMemory + 100); // Within 100MB of initial
  }

  @Test
  void testDatabaseConnectionPoolPerformance() throws InterruptedException {
    log.info("========================================");
    log.info("Testing database connection pool performance");
    log.info("========================================");

    int threads = 100;
    CountDownLatch startSignal = new CountDownLatch(1);
    CountDownLatch doneSignal = new CountDownLatch(threads);
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);

    for (int i = 0; i < threads; i++) {
      executor.submit(
          () -> {
            try {
              startSignal.await(); // Wait for all threads to be ready

              // Try to get a connection and perform a query
              long count = articleRepository.count();
              successCount.incrementAndGet(); // Count as success if query executes
            } catch (Exception e) {
              failureCount.incrementAndGet();
              log.error("Connection pool error", e);
            } finally {
              doneSignal.countDown();
            }
          });
    }

    long startTime = System.currentTimeMillis();
    startSignal.countDown(); // Start all threads simultaneously
    doneSignal.await(30, TimeUnit.SECONDS);
    long endTime = System.currentTimeMillis();

    executor.shutdown();

    log.info("Connection Pool Performance:");
    log.info("  Concurrent threads: {}", threads);
    log.info("  Successful connections: {}", successCount.get());
    log.info("  Failed connections: {}", failureCount.get());
    log.info("  Total time: {} ms", endTime - startTime);

    assertThat(failureCount.get()).isZero(); // No connection failures
    assertThat(successCount.get()).isEqualTo(threads); // All threads succeeded
  }
}

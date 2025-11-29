package com.teambind.articleserver;

import com.teambind.articleserver.adapter.out.persistence.entity.board.Board;
import com.teambind.articleserver.adapter.out.persistence.repository.ArticleRepository;
import com.teambind.articleserver.adapter.out.persistence.repository.BoardRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Standalone test to generate 600K test articles
 * Run this test to populate the database with test data for performance testing
 */
@SpringBootTest
@ActiveProfiles("performance-test")
@Slf4j
public class DataGeneratorTest {

    private static final int TARGET_ARTICLE_COUNT = 600_000;
    private static final int BATCH_SIZE = 1000;
    private static final int THREAD_POOL_SIZE = 8;
    private final Random random = new Random();
    @Autowired
    private ArticleRepository articleRepository;
    @Autowired
    private BoardRepository boardRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private List<Board> boards;

    @Test
    void generateTestData() {
        log.info("========================================");
        log.info("Starting Test Data Generation");
        log.info("Target: {} articles", TARGET_ARTICLE_COUNT);
        log.info("========================================");

        // Load existing boards
        boards = boardRepository.findAll();
        if (boards.isEmpty()) {
            log.error("No boards found in database!");
            log.info("Creating default boards...");
            createDefaultBoards();
            boards = boardRepository.findAll();
        }
        log.info("Found {} boards to use for test data", boards.size());

        // Check current article count
        long currentCount = articleRepository.count();
        log.info("Current article count: {}", currentCount);

        if (currentCount >= TARGET_ARTICLE_COUNT) {
            log.info("Already have {} articles, no need to generate more", currentCount);
            return;
        }

        int articlesToGenerate = TARGET_ARTICLE_COUNT - (int) currentCount;
        log.info("Need to generate {} more articles", articlesToGenerate);

        long startTime = System.currentTimeMillis();
        generateArticles(articlesToGenerate);
        long endTime = System.currentTimeMillis();

        long finalCount = articleRepository.count();
        log.info("========================================");
        log.info("Data Generation Completed");
        log.info("Final article count: {}", finalCount);
        log.info("Time taken: {} seconds", (endTime - startTime) / 1000);
        log.info("========================================");
    }

    private void createDefaultBoards() {
        String[] boardNames = {"General", "Tech", "News", "Events", "Announcements"};
        for (String name : boardNames) {
            try {
                jdbcTemplate.update(
                    "INSERT INTO boards (name, board_name, description, is_active, display_order, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    name, name, name + " Board", true, 0, LocalDateTime.now(), LocalDateTime.now()
                );
            } catch (Exception e) {
                log.warn("Could not create board {}: {}", name, e.getMessage());
            }
        }
    }

    private void generateArticles(int totalArticles) {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        AtomicInteger totalGenerated = new AtomicInteger(0);
        AtomicInteger batchCounter = new AtomicInteger(0);

        try {
            List<Future<Integer>> futures = new ArrayList<>();
            int articlesPerThread = totalArticles / THREAD_POOL_SIZE;
            int remainder = totalArticles % THREAD_POOL_SIZE;

            for (int i = 0; i < THREAD_POOL_SIZE; i++) {
                final int threadArticles = articlesPerThread + (i < remainder ? 1 : 0);
                final int threadId = i;

                futures.add(executor.submit(() -> {
                    return generateBatch(threadId, threadArticles, totalGenerated, batchCounter);
                }));
            }

            // Wait for all threads
            int totalCreated = 0;
            for (Future<Integer> future : futures) {
                try {
                    totalCreated += future.get();
                } catch (Exception e) {
                    log.error("Thread execution error", e);
                }
            }

            log.info("Total articles created: {}", totalCreated);

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

    private int generateBatch(int threadId, int articlesToGenerate, AtomicInteger globalCounter, AtomicInteger batchCounter) {
        int generated = 0;
        List<Object[]> batchData = new ArrayList<>();

        for (int i = 0; i < articlesToGenerate; i++) {
            Board board = boards.get(random.nextInt(boards.size()));
            LocalDateTime now = LocalDateTime.now();
            String articleId = String.format("ART-%d-%d-%d", threadId, i, System.currentTimeMillis());

            Object[] row = new Object[] {
                articleId,                    // article_id
                board.getId(),               // board_id
                getRandomArticleType(),      // article_type
                generateTitle(threadId, i),  // title
                generateContent(),           // contents
                "user-" + random.nextInt(100), // writer_id
                "ACTIVE",                    // status
                random.nextInt(10000),       // view_count
                "test-value",                // value
                now,                         // created_at
                now                          // updated_at
            };

            batchData.add(row);

            // Execute batch
            if (batchData.size() >= BATCH_SIZE) {
                executeBatch(batchData);
                generated += batchData.size();

                int currentBatch = batchCounter.incrementAndGet();
                int globalTotal = globalCounter.addAndGet(batchData.size());

                if (currentBatch % 10 == 0) {
                    log.info("Thread-{}: Batch {} completed. Global progress: {}/{}",
                        threadId, currentBatch, globalTotal, articlesToGenerate);
                }

                batchData.clear();
            }
        }

        // Execute remaining
        if (!batchData.isEmpty()) {
            executeBatch(batchData);
            generated += batchData.size();
            globalCounter.addAndGet(batchData.size());
        }

        return generated;
    }

    private void executeBatch(List<Object[]> batchData) {
        try {
            String sql = "INSERT INTO articles (article_id, board_id, article_type, title, contents, writer_id, status, view_count, value, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            jdbcTemplate.batchUpdate(sql, batchData);
        } catch (Exception e) {
            log.error("Batch insert error: {}", e.getMessage());
        }
    }

    private String getRandomArticleType() {
        double rand = random.nextDouble();
        if (rand < 0.7) return "REGULAR";
        else if (rand < 0.9) return "NOTICE";
        else return "EVENT";
    }

    private String generateTitle(int threadId, int index) {
        String[] prefixes = {"Important", "Update", "News", "Announcement", "Info", "Report", "Alert", "Notice"};
        String[] topics = {"Development", "Performance", "Testing", "Architecture", "Security", "Database", "Feature", "Bug Fix"};

        return String.format("[T%d-%05d] %s: %s Update #%d",
            threadId,
            index,
            prefixes[random.nextInt(prefixes.length)],
            topics[random.nextInt(topics.length)],
            System.currentTimeMillis() % 10000
        );
    }

    private String generateContent() {
        String[] sentences = {
            "This is test content for performance testing.",
            "The system should handle large amounts of data efficiently.",
            "Query optimization is crucial for good performance.",
            "Caching strategies can significantly improve response times.",
            "Database indexing plays a vital role in query performance.",
            "Monitoring and metrics are essential for production systems.",
            "Load balancing helps distribute traffic evenly.",
            "Microservices architecture provides better scalability."
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
}

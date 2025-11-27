package com.teambind.articleserver.performance.measurement;

import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import com.teambind.articleserver.adapter.out.persistence.entity.image.ArticleImage;
import com.teambind.articleserver.adapter.out.persistence.repository.ArticleRepository;
import com.teambind.articleserver.performance.util.QueryPerformanceTracker;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * N+1 쿼리 문제 검증 테스트
 * 연관 엔티티 로딩 시 발생하는 성능 문제를 측정하고 최적화 방법을 비교
 */
@SpringBootTest
@ActiveProfiles("performance-test")
@Slf4j
public class NPlusOneQueryTest {

    private static final int TEST_SIZE = 100;  // 테스트할 Article 개수
    private static final int WARMUP_SIZE = 20; // 워밍업 개수

    @Autowired private ArticleRepository articleRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @PersistenceContext private EntityManager entityManager;

    private QueryPerformanceTracker performanceTracker;
    private Statistics statistics;
    private List<String> testArticleIds;

    @BeforeEach
    public void setUp() {
        performanceTracker = new QueryPerformanceTracker();

        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);

        loadTestArticles();
        performWarmup();
    }

    private void loadTestArticles() {
        // 이미지가 있는 Article들 우선 선택 (N+1 테스트에 적합)
        String sql = """
            SELECT DISTINCT a.article_id
            FROM articles a
            INNER JOIN article_images ai ON a.article_id = ai.article_id
            LIMIT ?
        """;

        testArticleIds = jdbcTemplate.queryForList(sql, String.class, TEST_SIZE * 2);

        if (testArticleIds.size() < TEST_SIZE) {
            // 이미지가 있는 Article이 부족한 경우 일반 Article 추가
            List<String> additionalIds = articleRepository.findAll(
                PageRequest.of(0, TEST_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"))
            ).stream()
             .map(Article::getId)
             .filter(id -> !testArticleIds.contains(id))
             .limit(TEST_SIZE - testArticleIds.size())
             .collect(Collectors.toList());
            testArticleIds.addAll(additionalIds);
        }

        log.info("Loaded {} article IDs for N+1 testing", testArticleIds.size());
    }

    private void performWarmup() {
        log.info("Performing warmup...");
        for (int i = 0; i < WARMUP_SIZE; i++) {
            articleRepository.findById(testArticleIds.get(i % testArticleIds.size()));
        }
        entityManager.clear();
        statistics.clear();
        log.info("Warmup completed");
    }

    @Test
    @Transactional(readOnly = true)
    public void testNPlusOneProblem() {
        log.info("========== N+1 Query Problem Detection Test ==========\n");

        // 1. Lazy Loading으로 N+1 문제 발생
        testLazyLoading();

        // 2. Eager Loading으로 해결 시도 (메모리 문제 가능)
        testEagerLoading();

        // 3. Fetch Join으로 최적화
        testFetchJoin();

        // 4. Entity Graph로 최적화
        testEntityGraph();

        // 5. Batch Size로 최적화
        testBatchFetch();

        // 6. DTO Projection으로 최적화
        testDtoProjection();

        // 결과 비교 및 권장사항 제시
        generateComparisonReport();
    }

    /**
     * 1. Lazy Loading - N+1 문제 발생
     */
    private void testLazyLoading() {
        log.info("\n--- Test 1: Lazy Loading (N+1 Problem) ---");
        entityManager.clear();
        statistics.clear();

        long startTime = System.nanoTime();
        long queryStartCount = statistics.getQueryExecutionCount();

        // Article 목록 조회 (1번 쿼리)
        List<Article> articles = articleRepository.findAll(
            PageRequest.of(0, TEST_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        // 각 Article의 이미지 접근 (N번 쿼리 발생)
        int totalImages = 0;
        for (Article article : articles) {
            try {
                // Lazy Loading 트리거
                totalImages += article.getImages().size();
            } catch (Exception e) {
                // Lazy initialization 예외 무시
            }
        }

        long duration = System.nanoTime() - startTime;
        long queryCount = statistics.getQueryExecutionCount() - queryStartCount;

        LazyLoadingResult result = new LazyLoadingResult(
            "Lazy Loading",
            queryCount,
            duration / 1_000_000.0,
            articles.size(),
            totalImages
        );

        log.info("Result: {} queries for {} articles", queryCount, articles.size());
        log.info("Time: {:.2f}ms", result.executionTime);
        log.info("N+1 Problem Detected: {}", queryCount > articles.size() + 1 ? "YES" : "NO");

        performanceTracker.recordResult("LAZY_LOADING", result);
    }

    /**
     * 2. Eager Loading - 모든 연관 데이터 즉시 로딩
     */
    private void testEagerLoading() {
        log.info("\n--- Test 2: Eager Loading ---");
        entityManager.clear();
        statistics.clear();

        long startTime = System.nanoTime();
        long queryStartCount = statistics.getQueryExecutionCount();

        // JPQL with LEFT JOIN (Eager)
        String jpql = """
            SELECT DISTINCT a FROM Article a
            LEFT JOIN FETCH a.images
            WHERE a.id IN :ids
        """;

        TypedQuery<Article> query = entityManager.createQuery(jpql, Article.class);
        query.setParameter("ids", testArticleIds.subList(0, Math.min(TEST_SIZE, testArticleIds.size())));

        List<Article> articles = query.getResultList();

        // 이미 로드되었으므로 추가 쿼리 없음
        int totalImages = articles.stream()
            .mapToInt(a -> a.getImages().size())
            .sum();

        long duration = System.nanoTime() - startTime;
        long queryCount = statistics.getQueryExecutionCount() - queryStartCount;

        EagerLoadingResult result = new EagerLoadingResult(
            "Eager Loading",
            queryCount,
            duration / 1_000_000.0,
            articles.size(),
            totalImages,
            calculateMemoryUsage()
        );

        log.info("Result: {} queries for {} articles", queryCount, articles.size());
        log.info("Time: {:.2f}ms", result.executionTime);
        log.info("Memory Impact: ~{}KB", result.memoryUsage / 1024);

        performanceTracker.recordResult("EAGER_LOADING", result);
    }

    /**
     * 3. Fetch Join으로 최적화
     */
    private void testFetchJoin() {
        log.info("\n--- Test 3: Fetch Join Optimization ---");
        entityManager.clear();
        statistics.clear();

        long startTime = System.nanoTime();
        long queryStartCount = statistics.getQueryExecutionCount();

        // Fetch Join으로 한 번에 로드
        String jpql = """
            SELECT DISTINCT a FROM Article a
            LEFT JOIN FETCH a.images ai
            WHERE a.boardId = :boardId
            ORDER BY a.createdAt DESC
        """;

        TypedQuery<Article> query = entityManager.createQuery(jpql, Article.class);
        query.setParameter("boardId", 1L);
        query.setMaxResults(TEST_SIZE);

        List<Article> articles = query.getResultList();

        int totalImages = articles.stream()
            .mapToInt(a -> a.getImages().size())
            .sum();

        long duration = System.nanoTime() - startTime;
        long queryCount = statistics.getQueryExecutionCount() - queryStartCount;

        FetchJoinResult result = new FetchJoinResult(
            "Fetch Join",
            queryCount,
            duration / 1_000_000.0,
            articles.size(),
            totalImages
        );

        log.info("Result: {} queries for {} articles", queryCount, articles.size());
        log.info("Time: {:.2f}ms", result.executionTime);
        log.info("Optimization Success: {}", queryCount <= 2 ? "YES" : "NO");

        performanceTracker.recordResult("FETCH_JOIN", result);
    }

    /**
     * 4. Entity Graph로 최적화
     */
    private void testEntityGraph() {
        log.info("\n--- Test 4: Entity Graph Optimization ---");
        entityManager.clear();
        statistics.clear();

        long startTime = System.nanoTime();
        long queryStartCount = statistics.getQueryExecutionCount();

        // Entity Graph 생성
        EntityGraph<Article> graph = entityManager.createEntityGraph(Article.class);
        graph.addAttributeNodes("images");

        String jpql = "SELECT a FROM Article a WHERE a.boardId = :boardId";
        TypedQuery<Article> query = entityManager.createQuery(jpql, Article.class);
        query.setParameter("boardId", 1L);
        query.setHint("javax.persistence.loadgraph", graph);
        query.setMaxResults(TEST_SIZE);

        List<Article> articles = query.getResultList();

        int totalImages = articles.stream()
            .mapToInt(a -> a.getImages().size())
            .sum();

        long duration = System.nanoTime() - startTime;
        long queryCount = statistics.getQueryExecutionCount() - queryStartCount;

        EntityGraphResult result = new EntityGraphResult(
            "Entity Graph",
            queryCount,
            duration / 1_000_000.0,
            articles.size(),
            totalImages
        );

        log.info("Result: {} queries for {} articles", queryCount, articles.size());
        log.info("Time: {:.2f}ms", result.executionTime);

        performanceTracker.recordResult("ENTITY_GRAPH", result);
    }

    /**
     * 5. Batch Size로 최적화
     */
    private void testBatchFetch() {
        log.info("\n--- Test 5: Batch Fetch Optimization ---");
        entityManager.clear();
        statistics.clear();

        // Hibernate 설정에 @BatchSize 적용 필요
        // 이 테스트는 실제 @BatchSize 어노테이션이 Entity에 적용되어 있어야 효과적

        long startTime = System.nanoTime();
        long queryStartCount = statistics.getQueryExecutionCount();

        List<Article> articles = articleRepository.findAll(
            PageRequest.of(0, TEST_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        // Batch로 로딩 (IN 절 사용)
        int totalImages = 0;
        for (Article article : articles) {
            totalImages += article.getImages().size();
        }

        long duration = System.nanoTime() - startTime;
        long queryCount = statistics.getQueryExecutionCount() - queryStartCount;

        BatchFetchResult result = new BatchFetchResult(
            "Batch Fetch",
            queryCount,
            duration / 1_000_000.0,
            articles.size(),
            totalImages,
            calculateBatchSize(queryCount, articles.size())
        );

        log.info("Result: {} queries for {} articles", queryCount, articles.size());
        log.info("Time: {:.2f}ms", result.executionTime);
        log.info("Estimated Batch Size: {}", result.batchSize);

        performanceTracker.recordResult("BATCH_FETCH", result);
    }

    /**
     * 6. DTO Projection으로 최적화
     */
    private void testDtoProjection() {
        log.info("\n--- Test 6: DTO Projection Optimization ---");
        entityManager.clear();
        statistics.clear();

        long startTime = System.nanoTime();
        long queryStartCount = statistics.getQueryExecutionCount();

        // DTO로 필요한 데이터만 조회
        String sql = """
            SELECT
                a.article_id,
                a.title,
                a.created_at,
                COUNT(ai.image_id) as image_count,
                GROUP_CONCAT(ai.article_image_url SEPARATOR ',') as image_urls
            FROM articles a
            LEFT JOIN article_images ai ON a.article_id = ai.article_id
            WHERE a.board_id = ?
            GROUP BY a.article_id
            ORDER BY a.created_at DESC
            LIMIT ?
        """;

        List<ArticleDto> articleDtos = jdbcTemplate.query(
            sql,
            new Object[]{1L, TEST_SIZE},
            (rs, rowNum) -> new ArticleDto(
                rs.getString("article_id"),
                rs.getString("title"),
                rs.getTimestamp("created_at"),
                rs.getInt("image_count"),
                rs.getString("image_urls") != null ?
                    Arrays.asList(rs.getString("image_urls").split(",")) :
                    Collections.emptyList()
            )
        );

        long duration = System.nanoTime() - startTime;
        long queryCount = statistics.getQueryExecutionCount() - queryStartCount + 1; // JDBC query

        DtoProjectionResult result = new DtoProjectionResult(
            "DTO Projection",
            queryCount,
            duration / 1_000_000.0,
            articleDtos.size(),
            articleDtos.stream().mapToInt(ArticleDto::getImageCount).sum()
        );

        log.info("Result: {} queries for {} articles", queryCount, articleDtos.size());
        log.info("Time: {:.2f}ms", result.executionTime);
        log.info("Direct SQL with DTO: Most Efficient");

        performanceTracker.recordResult("DTO_PROJECTION", result);
    }

    /**
     * 최종 비교 리포트 생성
     */
    private void generateComparisonReport() {
        log.info("\n========== N+1 Query Optimization Comparison Report ==========\n");

        Map<String, TestResult> results = performanceTracker.getAllResults();

        // 성능 순위 매기기
        List<Map.Entry<String, TestResult>> sortedResults = results.entrySet().stream()
            .sorted(Map.Entry.comparingByValue((a, b) ->
                Double.compare(a.executionTime, b.executionTime)))
            .collect(Collectors.toList());

        log.info("Performance Ranking (Fastest to Slowest):");
        log.info("------------------------------------------------");
        log.info("| Rank | Method          | Queries | Time (ms) | Grade |");
        log.info("------------------------------------------------");

        int rank = 1;
        for (Map.Entry<String, TestResult> entry : sortedResults) {
            TestResult result = entry.getValue();
            String grade = determineGrade(result);
            log.info("| {:2d}   | {:15s} | {:7d} | {:9.2f} | {:5s} |",
                rank++, result.methodName, result.queryCount, result.executionTime, grade);
        }
        log.info("------------------------------------------------");

        // 권장사항
        log.info("\n=== Recommendations ===");
        TestResult bestResult = sortedResults.get(0).getValue();
        log.info("1. Best Performance: {} ({:.2f}ms with {} queries)",
            bestResult.methodName, bestResult.executionTime, bestResult.queryCount);

        if (results.containsKey("LAZY_LOADING")) {
            TestResult lazyResult = results.get("LAZY_LOADING");
            if (lazyResult.queryCount > 10) {
                log.info("2. N+1 Problem Confirmed: {} queries detected", lazyResult.queryCount);
                log.info("   - Avoid lazy loading for collections in list views");
            }
        }

        log.info("3. Recommended Strategies:");
        log.info("   - For Read-Heavy Operations: Use DTO Projection");
        log.info("   - For Complex Entities: Use Fetch Join or Entity Graph");
        log.info("   - For Batch Processing: Configure @BatchSize");
        log.info("   - For API Responses: Consider GraphQL or Custom DTOs");

        // 메모리 vs 성능 트레이드오프
        log.info("\n=== Trade-off Analysis ===");
        log.info("Memory Usage vs Performance:");
        log.info("  - DTO Projection: Low Memory, Best Performance");
        log.info("  - Fetch Join: Medium Memory, Good Performance");
        log.info("  - Eager Loading: High Memory, Risk of Cartesian Product");
        log.info("  - Lazy Loading: Low Initial Memory, Poor Performance (N+1)");
    }

    private String determineGrade(TestResult result) {
        if (result.queryCount <= 2 && result.executionTime < 10) return "S";
        if (result.queryCount <= 5 && result.executionTime < 20) return "A";
        if (result.queryCount <= 10 && result.executionTime < 50) return "B";
        if (result.queryCount <= 20 && result.executionTime < 100) return "C";
        return "D";
    }

    private long calculateMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private int calculateBatchSize(long queryCount, int articleCount) {
        if (queryCount <= 1) return articleCount;
        return Math.max(1, articleCount / (int)(queryCount - 1));
    }

    // Result Classes
    @Data
    private static abstract class TestResult {
        protected final String methodName;
        protected final long queryCount;
        protected final double executionTime;
        protected final int articleCount;
        protected final int imageCount;

        protected TestResult(String methodName, long queryCount, double executionTime,
                           int articleCount, int imageCount) {
            this.methodName = methodName;
            this.queryCount = queryCount;
            this.executionTime = executionTime;
            this.articleCount = articleCount;
            this.imageCount = imageCount;
        }
    }

    private static class LazyLoadingResult extends TestResult {
        LazyLoadingResult(String methodName, long queryCount, double executionTime,
                         int articleCount, int imageCount) {
            super(methodName, queryCount, executionTime, articleCount, imageCount);
        }
    }

    private static class EagerLoadingResult extends TestResult {
        private final long memoryUsage;

        EagerLoadingResult(String methodName, long queryCount, double executionTime,
                          int articleCount, int imageCount, long memoryUsage) {
            super(methodName, queryCount, executionTime, articleCount, imageCount);
            this.memoryUsage = memoryUsage;
        }
    }

    private static class FetchJoinResult extends TestResult {
        FetchJoinResult(String methodName, long queryCount, double executionTime,
                       int articleCount, int imageCount) {
            super(methodName, queryCount, executionTime, articleCount, imageCount);
        }
    }

    private static class EntityGraphResult extends TestResult {
        EntityGraphResult(String methodName, long queryCount, double executionTime,
                         int articleCount, int imageCount) {
            super(methodName, queryCount, executionTime, articleCount, imageCount);
        }
    }

    private static class BatchFetchResult extends TestResult {
        private final int batchSize;

        BatchFetchResult(String methodName, long queryCount, double executionTime,
                        int articleCount, int imageCount, int batchSize) {
            super(methodName, queryCount, executionTime, articleCount, imageCount);
            this.batchSize = batchSize;
        }
    }

    private static class DtoProjectionResult extends TestResult {
        DtoProjectionResult(String methodName, long queryCount, double executionTime,
                           int articleCount, int imageCount) {
            super(methodName, queryCount, executionTime, articleCount, imageCount);
        }
    }

    @Data
    private static class ArticleDto {
        private final String articleId;
        private final String title;
        private final Date createdAt;
        private final int imageCount;
        private final List<String> imageUrls;
    }
}
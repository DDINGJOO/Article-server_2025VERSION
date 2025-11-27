package com.teambind.articleserver.performance.measurement;

import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import com.teambind.articleserver.adapter.out.persistence.repository.ArticleRepository;
import com.teambind.articleserver.performance.util.QueryPerformanceTracker;
import com.teambind.articleserver.service.crud.impl.ArticleReadService;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 동시성 부하 테스트
 * 100명 이상의 동시 사용자가 시스템을 사용하는 상황 시뮬레이션
 */
@SpringBootTest
@ActiveProfiles("performance-test")
@Slf4j
public class ConcurrencyLoadTest {

    // 테스트 설정
    private static final int CONCURRENT_USERS = 100;         // 동시 사용자 수
    private static final int REQUESTS_PER_USER = 50;        // 사용자당 요청 수
    private static final int RAMP_UP_SECONDS = 5;           // 사용자 증가 시간
    private static final int TEST_DURATION_SECONDS = 60;    // 테스트 지속 시간

    @Autowired private ArticleReadService articleReadService;
    @Autowired private ArticleRepository articleRepository;
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbcTemplate;
    @PersistenceContext private EntityManager entityManager;

    private QueryPerformanceTracker performanceTracker;
    private Statistics statistics;
    private List<String> testArticleIds;
    private Random random = new Random();

    // 메트릭 수집
    private final ConcurrentHashMap<String, List<Long>> responseTimesByScenario = new ConcurrentHashMap<>();
    private final AtomicInteger successCount = new AtomicInteger();
    private final AtomicInteger errorCount = new AtomicInteger();
    private final AtomicLong totalResponseTime = new AtomicLong();

    @BeforeEach
    public void setUp() {
        performanceTracker = new QueryPerformanceTracker();

        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        // 테스트용 Article ID 목록 로드 (10,000개)
        loadTestData();

        // Connection Pool 상태 확인
        checkConnectionPoolStatus();
    }

    private void loadTestData() {
        log.info("Loading test article IDs...");
        testArticleIds = articleRepository.findAll(
            PageRequest.of(0, 10000, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).stream()
         .map(Article::getId)
         .collect(Collectors.toList());
        log.info("Loaded {} article IDs for testing", testArticleIds.size());
    }

    private void checkConnectionPoolStatus() {
        if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            log.info("Connection Pool Configuration:");
            log.info("  Maximum Pool Size: {}", hikariDataSource.getMaximumPoolSize());
            log.info("  Minimum Idle: {}", hikariDataSource.getMinimumIdle());
            log.info("  Connection Timeout: {}ms", hikariDataSource.getConnectionTimeout());
        }
    }

    @Test
    public void testHighConcurrencyLoad() throws Exception {
        log.info("========== Starting High Concurrency Load Test ==========");
        log.info("Concurrent Users: {}", CONCURRENT_USERS);
        log.info("Requests per User: {}", REQUESTS_PER_USER);
        log.info("Total Expected Requests: {}", CONCURRENT_USERS * REQUESTS_PER_USER);

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(CONCURRENT_USERS);

        List<Future<UserSimulationResult>> futures = new ArrayList<>();

        // 사용자 시뮬레이션 시작
        for (int userId = 1; userId <= CONCURRENT_USERS; userId++) {
            final int userIdFinal = userId;
            futures.add(executor.submit(() -> {
                try {
                    // 모든 스레드가 동시에 시작하도록 대기
                    startLatch.await();
                    return simulateUser(userIdFinal, completionLatch);
                } catch (Exception e) {
                    log.error("User {} simulation failed", userIdFinal, e);
                    return new UserSimulationResult(userIdFinal, 0, 0, 0);
                }
            }));

            // Ramp-up 시뮬레이션
            if (userId % (CONCURRENT_USERS / RAMP_UP_SECONDS) == 0) {
                Thread.sleep(1000);
                log.info("Ramped up to {} users", userId);
            }
        }

        // 테스트 시작
        long testStartTime = System.currentTimeMillis();
        startLatch.countDown(); // 모든 사용자 동시 시작!

        // 진행 상황 모니터링
        monitorProgress(completionLatch, testStartTime);

        // 모든 사용자 시뮬레이션 완료 대기
        boolean completed = completionLatch.await(TEST_DURATION_SECONDS * 2, TimeUnit.SECONDS);
        if (!completed) {
            log.warn("Test timeout - some users did not complete");
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        // 결과 수집 및 분석
        analyzeResults(futures, System.currentTimeMillis() - testStartTime);
    }

    /**
     * 개별 사용자 시뮬레이션
     */
    private UserSimulationResult simulateUser(int userId, CountDownLatch completionLatch) {
        UserSimulationResult result = new UserSimulationResult(userId, 0, 0, 0);
        List<Long> userResponseTimes = new ArrayList<>();

        try {
            for (int requestNum = 1; requestNum <= REQUESTS_PER_USER; requestNum++) {
                // 다양한 시나리오 랜덤 선택
                ScenarioType scenario = ScenarioType.values()[random.nextInt(ScenarioType.values().length)];

                long startTime = System.nanoTime();
                boolean success = executeScenario(scenario, userId);
                long responseTime = System.nanoTime() - startTime;

                userResponseTimes.add(responseTime);
                responseTimesByScenario.computeIfAbsent(scenario.name(), k -> new CopyOnWriteArrayList<>())
                    .add(responseTime);

                if (success) {
                    result.successCount++;
                    successCount.incrementAndGet();
                } else {
                    result.errorCount++;
                    errorCount.incrementAndGet();
                }

                totalResponseTime.addAndGet(responseTime);

                // 실제 사용자처럼 약간의 대기 시간 추가 (10-50ms)
                Thread.sleep(10 + random.nextInt(40));
            }

            // 사용자별 평균 응답시간 계산
            result.avgResponseTime = userResponseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0) / 1_000_000.0; // nano to millis

        } catch (Exception e) {
            log.error("User {} encountered error", userId, e);
        } finally {
            completionLatch.countDown();
        }

        return result;
    }

    /**
     * 다양한 시나리오 실행
     */
    private boolean executeScenario(ScenarioType scenario, int userId) {
        try {
            switch (scenario) {
                case SINGLE_ARTICLE_FETCH:
                    return fetchSingleArticle();

                case LIST_PAGINATION:
                    return fetchArticleList();

                case SEARCH_BY_TITLE:
                    return searchArticles();

                case FILTER_BY_BOARD:
                    return filterByBoard();

                case JDBC_DIRECT_QUERY:
                    return executeDirectJdbcQuery();

                default:
                    return false;
            }
        } catch (Exception e) {
            log.debug("Scenario {} failed for user {}: {}", scenario, userId, e.getMessage());
            return false;
        }
    }

    private boolean fetchSingleArticle() {
        performanceTracker.startQuery("FETCH_SINGLE");
        try {
            String articleId = testArticleIds.get(random.nextInt(testArticleIds.size()));
            Article article = articleRepository.findById(articleId).orElse(null);
            return article != null;
        } finally {
            performanceTracker.endQuery("FETCH_SINGLE");
        }
    }

    private boolean fetchArticleList() {
        performanceTracker.startQuery("FETCH_LIST");
        try {
            PageRequest pageRequest = PageRequest.of(random.nextInt(100), 20,
                Sort.by(Sort.Direction.DESC, "createdAt"));
            var page = articleRepository.findAll(pageRequest);
            return !page.isEmpty();
        } finally {
            performanceTracker.endQuery("FETCH_LIST");
        }
    }

    private boolean searchArticles() {
        performanceTracker.startQuery("SEARCH");
        try {
            // JPQL로 직접 검색
            String searchTerm = "Performance Test Article #" + random.nextInt(1000);
            String jpql = "SELECT a FROM Article a WHERE a.title LIKE :title";
            var query = entityManager.createQuery(jpql, Article.class);
            query.setParameter("title", "%" + searchTerm + "%");
            query.setMaxResults(10);
            var results = query.getResultList();
            return !results.isEmpty();
        } finally {
            performanceTracker.endQuery("SEARCH");
        }
    }

    private boolean filterByBoard() {
        performanceTracker.startQuery("FILTER");
        try {
            Long boardId = (long)(1 + random.nextInt(5));
            // JPQL로 직접 필터링
            String jpql = "SELECT a FROM Article a WHERE a.boardId = :boardId ORDER BY a.createdAt DESC";
            var query = entityManager.createQuery(jpql, Article.class);
            query.setParameter("boardId", boardId);
            query.setMaxResults(20);
            var results = query.getResultList();
            return !results.isEmpty();
        } finally {
            performanceTracker.endQuery("FILTER");
        }
    }

    private boolean executeDirectJdbcQuery() {
        performanceTracker.startQuery("JDBC_DIRECT");
        try {
            String sql = "SELECT COUNT(*) FROM articles WHERE board_id = ?";
            Long boardId = (long)(1 + random.nextInt(5));
            Integer count = performanceTracker.measureJdbcQuery(
                jdbcTemplate, sql, new Object[]{boardId}, Integer.class);
            return count != null && count > 0;
        } finally {
            performanceTracker.endQuery("JDBC_DIRECT");
        }
    }

    /**
     * 실시간 진행 상황 모니터링
     */
    private void monitorProgress(CountDownLatch completionLatch, long startTime) {
        ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor();
        monitor.scheduleAtFixedRate(() -> {
            long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
            int remainingUsers = (int) completionLatch.getCount();
            int completedUsers = CONCURRENT_USERS - remainingUsers;

            double tps = successCount.get() / Math.max(1.0, elapsedSeconds);
            double errorRate = errorCount.get() * 100.0 / Math.max(1, successCount.get() + errorCount.get());

            log.info("Progress: {}s - Active Users: {}/{} - TPS: {:.1f} - Success: {} - Errors: {} ({:.1f}%)",
                elapsedSeconds, completedUsers, CONCURRENT_USERS, tps,
                successCount.get(), errorCount.get(), errorRate);

            // Connection Pool 상태 확인
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikari = (HikariDataSource) dataSource;
                log.info("  Pool Status - Active: {} / Idle: {} / Total: {} / Waiting: {}",
                    hikari.getHikariPoolMXBean().getActiveConnections(),
                    hikari.getHikariPoolMXBean().getIdleConnections(),
                    hikari.getHikariPoolMXBean().getTotalConnections(),
                    hikari.getHikariPoolMXBean().getThreadsAwaitingConnection());
            }

            if (completionLatch.getCount() == 0) {
                monitor.shutdown();
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    /**
     * 테스트 결과 분석 및 리포트 생성
     */
    private void analyzeResults(List<Future<UserSimulationResult>> futures, long totalTestTime) throws Exception {
        log.info("\n========== Test Results Analysis ==========");

        // 사용자별 결과 수집
        List<UserSimulationResult> userResults = new ArrayList<>();
        for (Future<UserSimulationResult> future : futures) {
            try {
                userResults.add(future.get(5, TimeUnit.SECONDS));
            } catch (TimeoutException e) {
                log.warn("Timeout waiting for user result");
            }
        }

        // 전체 통계
        log.info("\n=== Overall Statistics ===");
        log.info("Test Duration: {}s", totalTestTime / 1000);
        log.info("Total Requests: {}", successCount.get() + errorCount.get());
        log.info("Successful Requests: {}", successCount.get());
        log.info("Failed Requests: {}", errorCount.get());
        log.info("Error Rate: {:.2f}%", errorCount.get() * 100.0 / (successCount.get() + errorCount.get()));
        log.info("Average TPS: {:.2f}", (successCount.get() + errorCount.get()) / (totalTestTime / 1000.0));

        // 시나리오별 성능 메트릭
        log.info("\n=== Performance by Scenario ===");
        for (Map.Entry<String, List<Long>> entry : responseTimesByScenario.entrySet()) {
            calculateAndPrintMetrics(entry.getKey(), entry.getValue());
        }

        // 쿼리 성능 메트릭
        log.info("\n=== Query Performance Metrics ===");
        performanceTracker.printAllMetrics();

        // Hibernate 통계
        log.info("\n=== Hibernate Statistics ===");
        performanceTracker.collectHibernateMetrics(statistics, "Concurrency Load Test");

        // 성능 등급 판정
        determinePerformanceGrade();
    }

    private void calculateAndPrintMetrics(String scenario, List<Long> responseTimes) {
        if (responseTimes.isEmpty()) return;

        List<Long> sorted = new ArrayList<>(responseTimes);
        Collections.sort(sorted);

        int size = sorted.size();
        double p50 = sorted.get(size / 2) / 1_000_000.0;
        double p95 = sorted.get((int)(size * 0.95)) / 1_000_000.0;
        double p99 = sorted.get((int)(size * 0.99)) / 1_000_000.0;
        double avg = sorted.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000.0;

        log.info("[{}]", scenario);
        log.info("  Count: {}", size);
        log.info("  P50: {:.2f}ms", p50);
        log.info("  P95: {:.2f}ms", p95);
        log.info("  P99: {:.2f}ms", p99);
        log.info("  Avg: {:.2f}ms", avg);
    }

    private void determinePerformanceGrade() {
        log.info("\n=== Performance Grade ===");

        double avgResponseTime = totalResponseTime.get() / (double)(successCount.get() + errorCount.get()) / 1_000_000.0;
        String grade;

        if (avgResponseTime < 10 && errorCount.get() < 10) {
            grade = "S - Excellent (Production Ready)";
        } else if (avgResponseTime < 50 && errorCount.get() < 50) {
            grade = "A - Very Good (Minor Optimizations Needed)";
        } else if (avgResponseTime < 100 && errorCount.get() < 100) {
            grade = "B - Good (Some Optimizations Recommended)";
        } else if (avgResponseTime < 200 && errorCount.get() < 200) {
            grade = "C - Fair (Significant Optimizations Required)";
        } else {
            grade = "D - Poor (Major Performance Issues)";
        }

        log.info("Final Grade: {}", grade);
        log.info("Average Response Time: {:.2f}ms", avgResponseTime);
    }

    /**
     * 시나리오 타입 정의
     */
    private enum ScenarioType {
        SINGLE_ARTICLE_FETCH,
        LIST_PAGINATION,
        SEARCH_BY_TITLE,
        FILTER_BY_BOARD,
        JDBC_DIRECT_QUERY
    }

    /**
     * 사용자 시뮬레이션 결과
     */
    @Data
    private static class UserSimulationResult {
        private final int userId;
        private int successCount;
        private int errorCount;
        private double avgResponseTime;

        public UserSimulationResult(int userId, int successCount, int errorCount, double avgResponseTime) {
            this.userId = userId;
            this.successCount = successCount;
            this.errorCount = errorCount;
            this.avgResponseTime = avgResponseTime;
        }
    }
}
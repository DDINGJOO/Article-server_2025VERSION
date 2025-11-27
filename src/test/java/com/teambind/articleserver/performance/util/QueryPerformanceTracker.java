package com.teambind.articleserver.performance.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.stat.QueryStatistics;
import org.hibernate.stat.Statistics;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * DB 쿼리 실행 시간을 정밀하게 측정하는 유틸리티
 * JDBC 레벨과 Hibernate 레벨 모두에서 측정
 */
@Slf4j
@Component
public class QueryPerformanceTracker {

    private final Map<String, List<Long>> queryExecutionTimes = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> queryCounters = new ConcurrentHashMap<>();
    private final ThreadLocal<Long> queryStartTime = new ThreadLocal<>();
    private final Map<String, Object> testResults = new ConcurrentHashMap<>();

    /**
     * 쿼리 실행 시작 시간 기록
     */
    public void startQuery(String queryType) {
        queryStartTime.set(System.nanoTime());
    }

    /**
     * 쿼리 실행 종료 및 시간 기록
     */
    public void endQuery(String queryType) {
        Long startTime = queryStartTime.get();
        if (startTime != null) {
            long executionTime = System.nanoTime() - startTime;
            queryExecutionTimes.computeIfAbsent(queryType, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(executionTime);
            queryCounters.computeIfAbsent(queryType, k -> new AtomicLong(0)).incrementAndGet();
            queryStartTime.remove();
        }
    }

    /**
     * Hibernate Statistics를 사용한 쿼리 메트릭 수집
     */
    public void collectHibernateMetrics(Statistics statistics, String testName) {
        if (statistics == null) {
            log.warn("Hibernate Statistics is null for test: {}", testName);
            return;
        }

        log.info("=== Hibernate Query Metrics for {} ===", testName);
        log.info("Total Query Count: {}", statistics.getQueryExecutionCount());
        log.info("Query Cache Hit Count: {}", statistics.getQueryCacheHitCount());
        log.info("Query Cache Miss Count: {}", statistics.getQueryCacheMissCount());
        log.info("Query Cache Put Count: {}", statistics.getQueryCachePutCount());
        log.info("Max Query Execution Time: {}ms", statistics.getQueryExecutionMaxTime());

        // 개별 쿼리 통계
        String[] queries = statistics.getQueries();
        if (queries != null && queries.length > 0) {
            for (String query : queries) {
                QueryStatistics queryStats = statistics.getQueryStatistics(query);
                log.info("Query: {}", query.substring(0, Math.min(query.length(), 50)) + "...");
                log.info("  - Execution Count: {}", queryStats.getExecutionCount());
                log.info("  - Row Count: {}", queryStats.getExecutionRowCount());
                log.info("  - Avg Time: {}ms", queryStats.getExecutionAvgTime());
                log.info("  - Max Time: {}ms", queryStats.getExecutionMaxTime());
                log.info("  - Min Time: {}ms", queryStats.getExecutionMinTime());
            }
        }
    }

    /**
     * JDBC 레벨에서 직접 쿼리 실행 시간 측정
     */
    public <T> T measureJdbcQuery(JdbcTemplate jdbcTemplate, String sql, Object[] params, Class<T> returnType) {
        long startTime = System.nanoTime();
        try {
            T result = jdbcTemplate.queryForObject(sql, params, returnType);
            long executionTime = System.nanoTime() - startTime;
            recordJdbcQueryTime(sql, executionTime);
            return result;
        } catch (Exception e) {
            long executionTime = System.nanoTime() - startTime;
            recordJdbcQueryTime(sql + " (FAILED)", executionTime);
            throw e;
        }
    }

    /**
     * JDBC 쿼리 실행 시간 기록
     */
    private void recordJdbcQueryTime(String query, long nanoTime) {
        String queryType = extractQueryType(query);
        queryExecutionTimes.computeIfAbsent("JDBC_" + queryType, k -> Collections.synchronizedList(new ArrayList<>()))
            .add(nanoTime);

        double millis = nanoTime / 1_000_000.0;
        if (millis > 10) {
            log.warn("Slow Query Detected: {}ms - {}", String.format("%.2f", millis),
                query.substring(0, Math.min(query.length(), 100)));
        }
    }

    /**
     * 쿼리 타입 추출 (SELECT, INSERT, UPDATE, DELETE)
     */
    private String extractQueryType(String query) {
        String upperQuery = query.trim().toUpperCase();
        if (upperQuery.startsWith("SELECT")) return "SELECT";
        if (upperQuery.startsWith("INSERT")) return "INSERT";
        if (upperQuery.startsWith("UPDATE")) return "UPDATE";
        if (upperQuery.startsWith("DELETE")) return "DELETE";
        return "OTHER";
    }

    /**
     * 성능 메트릭 계산 (P50, P95, P99)
     */
    public PerformanceMetrics calculateMetrics(String queryType) {
        List<Long> times = queryExecutionTimes.get(queryType);
        if (times == null || times.isEmpty()) {
            return PerformanceMetrics.empty(queryType);
        }

        List<Long> sortedTimes = new ArrayList<>(times);
        Collections.sort(sortedTimes);

        int size = sortedTimes.size();
        double p50 = sortedTimes.get((int)(size * 0.50)) / 1_000_000.0;
        double p95 = sortedTimes.get((int)(size * 0.95)) / 1_000_000.0;
        double p99 = sortedTimes.get((int)(size * 0.99)) / 1_000_000.0;
        double avg = sortedTimes.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000.0;
        double min = sortedTimes.get(0) / 1_000_000.0;
        double max = sortedTimes.get(size - 1) / 1_000_000.0;

        return new PerformanceMetrics(queryType, size, p50, p95, p99, avg, min, max);
    }

    /**
     * 모든 쿼리 타입의 메트릭 출력
     */
    public void printAllMetrics() {
        log.info("\n========== Query Performance Report ==========");

        for (String queryType : queryExecutionTimes.keySet()) {
            PerformanceMetrics metrics = calculateMetrics(queryType);
            metrics.print();
        }

        log.info("\n========== Query Count Summary ==========");
        queryCounters.forEach((type, count) ->
            log.info("{}: {} queries", type, count.get()));
    }

    /**
     * 메트릭 초기화
     */
    public void reset() {
        queryExecutionTimes.clear();
        queryCounters.clear();
        queryStartTime.remove();
        testResults.clear();
    }

    /**
     * 테스트 결과 기록
     */
    public void recordResult(String testName, Object result) {
        testResults.put(testName, result);
    }

    /**
     * 모든 테스트 결과 반환
     */
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getAllResults() {
        return (Map<String, T>) new HashMap<>(testResults);
    }

    /**
     * 성능 메트릭 데이터 클래스
     */
    public static class PerformanceMetrics {
        public final String queryType;
        public final int count;
        public final double p50;
        public final double p95;
        public final double p99;
        public final double avg;
        public final double min;
        public final double max;

        public PerformanceMetrics(String queryType, int count, double p50, double p95, double p99, double avg, double min, double max) {
            this.queryType = queryType;
            this.count = count;
            this.p50 = p50;
            this.p95 = p95;
            this.p99 = p99;
            this.avg = avg;
            this.min = min;
            this.max = max;
        }

        public static PerformanceMetrics empty(String queryType) {
            return new PerformanceMetrics(queryType, 0, 0, 0, 0, 0, 0, 0);
        }

        public void print() {
            log.info("\n[{}] Query Performance:", queryType);
            log.info("  Count: {}", count);
            log.info("  P50: {:.2f}ms", p50);
            log.info("  P95: {:.2f}ms", p95);
            log.info("  P99: {:.2f}ms", p99);
            log.info("  Avg: {:.2f}ms", avg);
            log.info("  Min: {:.2f}ms", min);
            log.info("  Max: {:.2f}ms", max);

            // 성능 등급 판정
            String grade = determineGrade();
            log.info("  Grade: {}", grade);
        }

        private String determineGrade() {
            if (p95 < 0.5) return "S (Excellent)";
            if (p95 < 1.0) return "A (Very Good)";
            if (p95 < 2.0) return "B (Good)";
            if (p95 < 5.0) return "C (Needs Improvement)";
            return "D (Critical)";
        }
    }
}
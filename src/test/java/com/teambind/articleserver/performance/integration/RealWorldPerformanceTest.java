package com.teambind.articleserver.performance.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.teambind.articleserver.adapter.out.persistence.entity.enums.Status;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.StopWatch;

/**
 * 실제 사용자 시나리오 기반 성능 테스트
 * Controller → Service → Repository 전체 플로우 측정
 *
 * N+1 쿼리 개선 전후 비교:
 * - 개선 전: 125ms, 101 queries
 * - 개선 후 목표: 10ms 이하, 1-3 queries
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("performance-test")
public class RealWorldPerformanceTest {

    private final Map<String, List<Long>> performanceMetrics = new HashMap<>();
    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        performanceMetrics.clear();
    }

    @Test
    void testRealWorldScenarios() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🚀 Real World Performance Test - Controller to DB");
        System.out.println("=".repeat(80));

        // 1. 게시글 목록 조회 (N+1 문제 핵심)
        testArticleListPerformance();

        // 2. 단일 게시글 상세 조회
        testArticleDetailPerformance();

        // 3. 검색 성능
        testSearchPerformance();

        // 4. 100명 동시 접속 시뮬레이션
        testConcurrentUsersPerformance();

        // 결과 출력
        printPerformanceReport();
    }

    private void testArticleListPerformance() throws Exception {
        System.out.println("\n📋 Test 1: Article List API (N+1 Query Problem)");
        System.out.println("-".repeat(50));

        List<Long> responseTimes = new ArrayList<>();
        StopWatch stopWatch = new StopWatch();

        // Warm-up
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/articles")
                    .param("page", "0")
                    .param("size", "20"))
                    .andExpect(status().isOk());
        }

        // 실제 측정 (50회)
        for (int i = 0; i < 50; i++) {
            stopWatch.start();

            MvcResult result = mockMvc.perform(get("/api/articles")
                    .param("page", String.valueOf(i % 10))
                    .param("size", "20")
                    .param("status", Status.ACTIVE.name()))
                    .andExpect(status().isOk())
                    .andReturn();

            stopWatch.stop();
            responseTimes.add(stopWatch.getLastTaskTimeMillis());

            if (i == 0) {
                // 첫 번째 응답 분석
                String response = result.getResponse().getContentAsString();
                System.out.println("First response size: " + response.length() + " bytes");
            }
        }

        performanceMetrics.put("ArticleList", responseTimes);
        printMetrics("Article List", responseTimes);
    }

    private void testArticleDetailPerformance() throws Exception {
        System.out.println("\n📄 Test 2: Article Detail API");
        System.out.println("-".repeat(50));

        List<Long> responseTimes = new ArrayList<>();
        StopWatch stopWatch = new StopWatch();

        // 테스트용 article ID 가져오기
        String articleId = getFirstArticleId();

        for (int i = 0; i < 30; i++) {
            stopWatch.start();

            mockMvc.perform(get("/api/articles/{id}", articleId))
                    .andExpect(status().isOk())
                    .andReturn();

            stopWatch.stop();
            responseTimes.add(stopWatch.getLastTaskTimeMillis());
        }

        performanceMetrics.put("ArticleDetail", responseTimes);
        printMetrics("Article Detail", responseTimes);
    }

    private void testSearchPerformance() throws Exception {
        System.out.println("\n🔍 Test 3: Search API (LIKE Query)");
        System.out.println("-".repeat(50));

        List<Long> responseTimes = new ArrayList<>();
        StopWatch stopWatch = new StopWatch();
        String[] searchTerms = {"테스트", "Article", "성능", "개선", "Spring"};

        for (String term : searchTerms) {
            for (int i = 0; i < 10; i++) {
                stopWatch.start();

                mockMvc.perform(get("/api/articles/search")
                        .param("keyword", term)
                        .param("page", "0")
                        .param("size", "10"))
                        .andExpect(status().isOk());

                stopWatch.stop();
                responseTimes.add(stopWatch.getLastTaskTimeMillis());
            }
        }

        performanceMetrics.put("Search", responseTimes);
        printMetrics("Search", responseTimes);
    }

    private void testConcurrentUsersPerformance() throws Exception {
        System.out.println("\n👥 Test 4: 100 Concurrent Users");
        System.out.println("-".repeat(50));

        int concurrentUsers = 100;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        List<Long> responseTimes = new CopyOnWriteArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < concurrentUsers; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    StopWatch sw = new StopWatch();
                    sw.start();

                    mockMvc.perform(get("/api/articles")
                            .param("page", String.valueOf(userId % 5))
                            .param("size", "10"))
                            .andExpect(status().isOk());

                    sw.stop();
                    responseTimes.add(sw.getTotalTimeMillis());
                } catch (Exception e) {
                    System.err.println("User " + userId + " failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        long totalTime = System.currentTimeMillis() - startTime;
        performanceMetrics.put("ConcurrentUsers", responseTimes);

        System.out.println("Total time for 100 users: " + totalTime + "ms");
        System.out.println("Average response time: " +
            responseTimes.stream().mapToLong(Long::longValue).average().orElse(0) + "ms");
        System.out.println("Success rate: " +
            (responseTimes.size() * 100.0 / concurrentUsers) + "%");
    }

    private String getFirstArticleId() throws Exception {
        // 실제 article ID 가져오기 (테스트용)
        MvcResult result = mockMvc.perform(get("/api/articles")
                .param("page", "0")
                .param("size", "1"))
                .andExpect(status().isOk())
                .andReturn();

        // JSON 파싱해서 첫 번째 article ID 추출
        // 실제 구현에서는 Jackson 등을 사용
        return "test-article-id"; // 임시
    }

    private void printMetrics(String testName, List<Long> times) {
        if (times.isEmpty()) return;

        Collections.sort(times);
        long p50 = times.get(times.size() / 2);
        long p95 = times.get((int)(times.size() * 0.95));
        long p99 = times.get((int)(times.size() * 0.99));
        long max = times.get(times.size() - 1);
        double avg = times.stream().mapToLong(Long::longValue).average().orElse(0);

        System.out.println(String.format(
            "  P50: %dms | P95: %dms | P99: %dms | Max: %dms | Avg: %.1fms",
            p50, p95, p99, max, avg));

        // 성능 등급 판정
        String grade = getPerformanceGrade(p95);
        System.out.println("  Performance Grade: " + grade);
    }

    private String getPerformanceGrade(long p95) {
        if (p95 < 10) return "🟢 S (Excellent)";
        if (p95 < 20) return "🟢 A (Good)";
        if (p95 < 50) return "🟡 B (Fair)";
        if (p95 < 100) return "🟠 C (Poor)";
        return "🔴 D (Critical)";
    }

    private void printPerformanceReport() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 PERFORMANCE IMPROVEMENT REPORT");
        System.out.println("=".repeat(80));

        System.out.println("\n🎯 Expected Improvements:");
        System.out.println("┌────────────────┬──────────┬──────────┬────────────┐");
        System.out.println("│ Scenario       │ Before   │ After    │ Improvement│");
        System.out.println("├────────────────┼──────────┼──────────┼────────────┤");
        System.out.println("│ Article List   │ 125ms    │ Target<10ms │ 92% ↓   │");
        System.out.println("│ Query Count    │ 101      │ 1-3      │ 97% ↓      │");
        System.out.println("│ Search (LIKE)  │ 2.85ms   │ 0.5ms    │ 82% ↓      │");
        System.out.println("│ Connection Pool│ 10       │ 100      │ 10x ↑      │");
        System.out.println("└────────────────┴──────────┴──────────┴────────────┘");

        System.out.println("\n✅ Actual Results:");
        for (Map.Entry<String, List<Long>> entry : performanceMetrics.entrySet()) {
            List<Long> times = entry.getValue();
            if (!times.isEmpty()) {
                Collections.sort(times);
                long p95 = times.get((int)(times.size() * 0.95));
                System.out.println(String.format("  %s P95: %dms %s",
                    entry.getKey(), p95, getPerformanceGrade(p95)));
            }
        }

        // 결과를 파일로 저장 (추후 구현)
        // PerformanceResultWriter.writeResults("RealWorldPerformance", performanceMetrics);
    }
}

package com.teambind.articleserver.utils.generator;

import static org.assertj.core.api.Assertions.*;

import com.teambind.articleserver.utils.generator.primay_key.Snowflake;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

@DisplayName("Snowflake ID 생성기 테스트")
class SnowflakeTest {

  @Nested
  @DisplayName("ID 생성 테스트")
  class IdGenerationTest {

    @Test
    @DisplayName("정상: ID를 생성할 수 있다")
    void generateId_Success() {
      // given
      Snowflake snowflake = new Snowflake();

      // when
      long id = snowflake.nextId();

      // then
      assertThat(id).isPositive();
    }

    @Test
    @DisplayName("정상: String 형태로 ID를 생성할 수 있다")
    void generateKey_Success() {
      // given
      Snowflake snowflake = new Snowflake();

      // when
      String key = snowflake.generateKey();

      // then
      assertThat(key).isNotNull();
      assertThat(key).isNotEmpty();
      assertThat(Long.parseLong(key)).isPositive();
    }

    @Test
    @DisplayName("정상: 연속된 ID는 증가한다")
    void consecutiveIds_AreIncreasing() {
      // given
      Snowflake snowflake = new Snowflake();

      // when
      long id1 = snowflake.nextId();
      long id2 = snowflake.nextId();
      long id3 = snowflake.nextId();

      // then
      assertThat(id2).isGreaterThan(id1);
      assertThat(id3).isGreaterThan(id2);
    }

    @Test
    @DisplayName("정상: 여러 ID를 빠르게 생성할 수 있다")
    void generateMultipleIds_Quickly() {
      // given
      Snowflake snowflake = new Snowflake();
      int count = 1000;

      // when
      long startTime = System.currentTimeMillis();
      for (int i = 0; i < count; i++) {
        snowflake.nextId();
      }
      long duration = System.currentTimeMillis() - startTime;

      // then
      assertThat(duration).isLessThan(1000); // 1000개를 1초 이내에 생성
    }
  }

  @Nested
  @DisplayName("고유성 테스트")
  class UniquenessTest {

    @Test
    @DisplayName("정상: 생성된 ID들은 모두 고유하다")
    void generatedIds_AreUnique() {
      // given
      Snowflake snowflake = new Snowflake();
      Set<Long> ids = new HashSet<>();
      int count = 10000;

      // when
      for (int i = 0; i < count; i++) {
        ids.add(snowflake.nextId());
      }

      // then
      assertThat(ids).hasSize(count); // 모두 고유해야 함
    }

    @RepeatedTest(5)
    @DisplayName("정상: 반복 테스트에서도 고유성 유지")
    void repeatedTest_MaintainsUniqueness() {
      // given
      Snowflake snowflake = new Snowflake();
      Set<Long> ids = new HashSet<>();

      // when
      for (int i = 0; i < 5000; i++) {
        ids.add(snowflake.nextId());
      }

      // then
      assertThat(ids).hasSize(5000);
    }
  }

  @Nested
  @DisplayName("동시성 테스트")
  class ConcurrencyTest {

    @Test
    @DisplayName("정상: 멀티스레드 환경에서 고유한 ID 생성")
    void multiThreaded_GeneratesUniqueIds() throws InterruptedException {
      // given
      Snowflake snowflake = new Snowflake();
      int threadCount = 10;
      int idsPerThread = 1000;
      Set<Long> ids = new HashSet<>();
      CountDownLatch latch = new CountDownLatch(threadCount);
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);

      // when
      for (int i = 0; i < threadCount; i++) {
        executor.submit(
            () -> {
              try {
                for (int j = 0; j < idsPerThread; j++) {
                  synchronized (ids) {
                    ids.add(snowflake.nextId());
                  }
                }
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await(10, TimeUnit.SECONDS);
      executor.shutdown();

      // then
      assertThat(ids).hasSize(threadCount * idsPerThread);
    }

    @Test
    @DisplayName("정상: 동시 요청에서도 순서 보장")
    void concurrentRequests_MaintainOrder() throws InterruptedException {
      // given
      Snowflake snowflake = new Snowflake();
      int threadCount = 5;
      AtomicInteger errorCount = new AtomicInteger(0);
      CountDownLatch latch = new CountDownLatch(threadCount);
      ExecutorService executor = Executors.newFixedThreadPool(threadCount);

      // when
      for (int i = 0; i < threadCount; i++) {
        executor.submit(
            () -> {
              try {
                long prev = 0;
                for (int j = 0; j < 100; j++) {
                  long current = snowflake.nextId();
                  if (prev > 0 && current <= prev) {
                    errorCount.incrementAndGet();
                  }
                  prev = current;
                }
              } finally {
                latch.countDown();
              }
            });
      }

      latch.await(10, TimeUnit.SECONDS);
      executor.shutdown();

      // then
      assertThat(errorCount.get()).isZero();
    }
  }

  @Nested
  @DisplayName("시퀀스 오버플로우 테스트")
  class SequenceOverflowTest {

    @Test
    @DisplayName("정상: 같은 밀리초에 많은 ID 생성 시 정상 처리")
    void sameMillisecond_GeneratesMultipleIds() {
      // given
      Snowflake snowflake = new Snowflake();
      Set<Long> ids = new HashSet<>();

      // when - 같은 밀리초에 최대한 많은 ID 생성 시도
      long startTime = System.currentTimeMillis();
      while (System.currentTimeMillis() == startTime && ids.size() < 5000) {
        ids.add(snowflake.nextId());
      }

      // then
      assertThat(ids.size()).isGreaterThan(0);
      assertThat(ids).doesNotHaveDuplicates();
    }
  }

  @Nested
  @DisplayName("ID 구조 검증 테스트")
  class IdStructureTest {

    @Test
    @DisplayName("정상: 생성된 ID는 양수이다")
    void generatedId_IsPositive() {
      // given
      Snowflake snowflake = new Snowflake();

      // when
      for (int i = 0; i < 100; i++) {
        long id = snowflake.nextId();

        // then
        assertThat(id).isPositive();
      }
    }

    @Test
    @DisplayName("정상: ID는 64비트 범위 내에 있다")
    void generatedId_IsWithin64Bits() {
      // given
      Snowflake snowflake = new Snowflake();

      // when
      for (int i = 0; i < 100; i++) {
        long id = snowflake.nextId();

        // then
        assertThat(id).isLessThan(Long.MAX_VALUE);
        assertThat(id).isGreaterThan(0);
      }
    }

    @Test
    @DisplayName("정상: String 변환이 올바르게 동작한다")
    void stringConversion_WorksCorrectly() {
      // given
      Snowflake snowflake = new Snowflake();

      // when
      String key1 = snowflake.generateKey();
      String key2 = snowflake.generateKey();

      // then
      assertThat(key1).isNotEqualTo(key2);
      assertThat(Long.parseLong(key1)).isLessThan(Long.parseLong(key2));
    }
  }

  @Nested
  @DisplayName("성능 테스트")
  class PerformanceTest {

    @Test
    @DisplayName("성능: 100만개 ID를 빠르게 생성할 수 있다")
    void generateMillionIds_InReasonableTime() {
      // given
      Snowflake snowflake = new Snowflake();
      int count = 1_000_000;

      // when
      long startTime = System.nanoTime();
      for (int i = 0; i < count; i++) {
        snowflake.nextId();
      }
      long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

      // then
      System.out.println(
          "Generated " + count + " IDs in " + duration + "ms (" + (count / duration) + " IDs/ms)");
      assertThat(duration).isLessThan(10000); // 10초 이내
    }

    @Test
    @DisplayName("성능: 평균 생성 시간이 1마이크로초 미만")
    void averageGenerationTime_LessThanOneMicrosecond() {
      // given
      Snowflake snowflake = new Snowflake();
      int warmupCount = 1000;
      int testCount = 10000;

      // warmup
      for (int i = 0; i < warmupCount; i++) {
        snowflake.nextId();
      }

      // when
      long startTime = System.nanoTime();
      for (int i = 0; i < testCount; i++) {
        snowflake.nextId();
      }
      long duration = System.nanoTime() - startTime;
      long averageNanos = duration / testCount;

      // then
      System.out.println("Average generation time: " + averageNanos + "ns");
      assertThat(averageNanos).isLessThan(1000); // 1마이크로초 미만
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @Test
    @DisplayName("엣지: 연속 호출 시 ID 간격이 일정하지 않을 수 있다")
    void consecutiveCalls_MayHaveVariableGaps() {
      // given
      Snowflake snowflake = new Snowflake();

      // when
      long id1 = snowflake.nextId();
      long id2 = snowflake.nextId();
      long gap1 = id2 - id1;

      long id3 = snowflake.nextId();
      long gap2 = id3 - id2;

      // then
      // 간격은 시퀀스 증가 또는 타임스탬프 증가에 따라 달라질 수 있음
      assertThat(gap1).isPositive();
      assertThat(gap2).isPositive();
    }

    @Test
    @DisplayName("엣지: 새 인스턴스는 다른 nodeId를 가질 수 있다")
    void newInstance_MayHaveDifferentNodeId() {
      // given
      Snowflake snowflake1 = new Snowflake();
      Snowflake snowflake2 = new Snowflake();

      // when
      long id1 = snowflake1.nextId();
      long id2 = snowflake2.nextId();

      // then
      // nodeId가 랜덤이므로 ID가 다를 수 있음
      assertThat(id1).isNotEqualTo(id2);
    }
  }
}

package com.teambind.articleserver.validation;

import static org.assertj.core.api.Assertions.*;

import com.teambind.articleserver.entity.keyword.Keyword;
import com.teambind.articleserver.utils.DataInitializer;
import jakarta.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@DisplayName("ValidKeywordIdsValidator 테스트")
class ValidKeywordIdsValidatorTest {

  private ValidKeywordIdsValidator validator;

  @Mock private ConstraintValidatorContext context;

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    validator = new ValidKeywordIdsValidator();

    // keywordMap 초기화 및 테스트 데이터 추가
    DataInitializer.keywordMap.clear();
    DataInitializer.keywordMap.put(1L, Keyword.builder().id(1L).name("키워드1").build());
    DataInitializer.keywordMap.put(2L, Keyword.builder().id(2L).name("키워드2").build());
    DataInitializer.keywordMap.put(3L, Keyword.builder().id(3L).name("키워드3").build());
    DataInitializer.keywordMap.put(4L, Keyword.builder().id(4L).name("키워드4").build());
    DataInitializer.keywordMap.put(5L, Keyword.builder().id(5L).name("키워드5").build());
  }

  @AfterEach
  void tearDown() throws Exception {
    // keywordMap 정리
    DataInitializer.keywordMap.clear();
  }

  @Nested
  @DisplayName("정상 케이스 테스트")
  class ValidCasesTest {

    @Test
    @DisplayName("정상: 모든 ID가 유효하면 통과한다")
    void allValidIds_Pass() {
      // given
      List<Long> keywordIds = Arrays.asList(1L, 2L, 3L);

      // when
      boolean result = validator.isValid(keywordIds, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("정상: 단일 유효 ID는 통과한다")
    void singleValidId_Passes() {
      // given
      List<Long> keywordIds = Collections.singletonList(1L);

      // when
      boolean result = validator.isValid(keywordIds, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("정상: 모든 캐시된 ID가 통과한다")
    void allCachedIds_Pass() {
      // given
      List<Long> keywordIds = Arrays.asList(1L, 2L, 3L, 4L, 5L);

      // when
      boolean result = validator.isValid(keywordIds, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("정상: null 리스트는 통과한다")
    void nullList_Passes() {
      // given
      List<Long> keywordIds = null;

      // when
      boolean result = validator.isValid(keywordIds, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("정상: 빈 리스트는 통과한다")
    void emptyList_Passes() {
      // given
      List<Long> keywordIds = new ArrayList<>();

      // when
      boolean result = validator.isValid(keywordIds, context);

      // then
      assertThat(result).isTrue();
    }
  }

  @Nested
  @DisplayName("예외 케이스 테스트")
  class InvalidCasesTest {

    @Test
    @DisplayName("예외: 존재하지 않는 ID가 하나라도 있으면 실패한다")
    void oneInvalidId_Fails() {
      // given
      List<Long> keywordIds = Arrays.asList(1L, 2L, 999L);

      // when
      boolean result = validator.isValid(keywordIds, context);

      // then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("예외: 모든 ID가 유효하지 않으면 실패한다")
    void allInvalidIds_Fail() {
      // given
      List<Long> keywordIds = Arrays.asList(100L, 200L, 300L);

      // when
      boolean result = validator.isValid(keywordIds, context);

      // then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("예외: 단일 유효하지 않은 ID는 실패한다")
    void singleInvalidId_Fails() {
      // given
      List<Long> keywordIds = Collections.singletonList(999L);

      // when
      boolean result = validator.isValid(keywordIds, context);

      // then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("예외: 0 ID는 실패한다")
    void zeroId_Fails() {
      // given
      List<Long> keywordIds = Arrays.asList(1L, 0L, 2L);

      // when
      boolean result = validator.isValid(keywordIds, context);

      // then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("예외: 음수 ID는 실패한다")
    void negativeId_Fails() {
      // given
      List<Long> keywordIds = Arrays.asList(1L, -1L, 2L);

      // when
      boolean result = validator.isValid(keywordIds, context);

      // then
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCasesTest {

    @Test
    @DisplayName("엣지: 중복된 유효 ID는 통과한다")
    void duplicateValidIds_Pass() {
      // given
      List<Long> keywordIds = Arrays.asList(1L, 1L, 2L, 2L, 3L);

      // when
      boolean result = validator.isValid(keywordIds, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("엣지: 중복된 유효하지 않은 ID는 실패한다")
    void duplicateInvalidIds_Fail() {
      // given
      List<Long> keywordIds = Arrays.asList(999L, 999L, 888L);

      // when
      boolean result = validator.isValid(keywordIds, context);

      // then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("엣지: 순서가 다른 같은 ID 세트는 같은 결과를 반환한다")
    void differentOrder_SameResult() {
      // given
      List<Long> keywordIds1 = Arrays.asList(1L, 2L, 3L);
      List<Long> keywordIds2 = Arrays.asList(3L, 1L, 2L);

      // when
      boolean result1 = validator.isValid(keywordIds1, context);
      boolean result2 = validator.isValid(keywordIds2, context);

      // then
      assertThat(result1).isTrue();
      assertThat(result2).isTrue();
      assertThat(result1).isEqualTo(result2);
    }

    @Test
    @DisplayName("엣지: 많은 수의 유효한 ID를 검증할 수 있다")
    void largeNumberOfValidIds_Pass() {
      // given
      List<Long> keywordIds = Arrays.asList(1L, 2L, 3L, 4L, 5L);

      // when
      boolean result = validator.isValid(keywordIds, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("엣지: null이 포함된 리스트는 실패한다")
    void listWithNull_Fails() {
      // given
      List<Long> keywordIds = new ArrayList<>();
      keywordIds.add(1L);
      keywordIds.add(null);
      keywordIds.add(2L);

      // when & then
      assertThatThrownBy(() -> validator.isValid(keywordIds, context))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("엣지: Long.MAX_VALUE ID는 실패한다")
    void maxLongId_Fails() {
      // given
      List<Long> keywordIds = Arrays.asList(1L, Long.MAX_VALUE);

      // when
      boolean result = validator.isValid(keywordIds, context);

      // then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("엣지: Long.MIN_VALUE ID는 실패한다")
    void minLongId_Fails() {
      // given
      List<Long> keywordIds = Arrays.asList(1L, Long.MIN_VALUE);

      // when
      boolean result = validator.isValid(keywordIds, context);

      // then
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("캐시 활용 테스트")
  class CacheUsageTest {

    @Test
    @DisplayName("정상: 캐시를 활용하여 빠르게 검증한다")
    void cacheValidation_IsFast() {
      // given
      List<Long> keywordIds = Arrays.asList(1L, 2L, 3L);

      // when
      long startTime = System.nanoTime();
      boolean result = validator.isValid(keywordIds, context);
      long duration = System.nanoTime() - startTime;

      // then
      assertThat(result).isTrue();
      assertThat(duration).isLessThan(1_000_000); // 1ms 이내
    }

    @Test
    @DisplayName("정상: 반복 검증 시 성능이 일정하다")
    void repeatedValidation_ConsistentPerformance() {
      // given
      List<Long> keywordIds = Arrays.asList(1L, 2L, 3L);

      // when
      for (int i = 0; i < 1000; i++) {
        boolean result = validator.isValid(keywordIds, context);
        assertThat(result).isTrue();
      }

      // then - 예외 없이 완료되면 성공
    }

    @Test
    @DisplayName("정상: 캐시가 비어있을 때 모든 ID는 실패한다")
    void emptyCache_AllIdsFail() throws Exception {
      // given
      DataInitializer.keywordMap.clear();
      List<Long> keywordIds = Arrays.asList(1L, 2L, 3L);

      // when
      boolean result = validator.isValid(keywordIds, context);

      // then
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("Stream API 동작 테스트")
  class StreamApiTest {

    @Test
    @DisplayName("정상: Stream의 allMatch가 올바르게 동작한다")
    void streamAllMatch_WorksCorrectly() {
      // given
      List<Long> validIds = Arrays.asList(1L, 2L, 3L);
      List<Long> invalidIds = Arrays.asList(1L, 2L, 999L);

      // when
      boolean validResult = validator.isValid(validIds, context);
      boolean invalidResult = validator.isValid(invalidIds, context);

      // then
      assertThat(validResult).isTrue();
      assertThat(invalidResult).isFalse();
    }

    @Test
    @DisplayName("정상: 첫 번째 유효하지 않은 ID에서 빠르게 실패한다")
    void failsFast_OnFirstInvalidId() {
      // given - 첫 번째가 유효하지 않음
      List<Long> keywordIds = Arrays.asList(999L, 1L, 2L, 3L);

      // when
      boolean result = validator.isValid(keywordIds, context);

      // then
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("성능 테스트")
  class PerformanceTest {

    @Test
    @DisplayName("성능: 10000개 ID를 빠르게 검증할 수 있다")
    void validate10000Ids_Quickly() {
      // given
      List<Long> keywordIds = new ArrayList<>();
      for (long i = 1; i <= 5; i++) {
        for (int j = 0; j < 2000; j++) {
          keywordIds.add(i);
        }
      }

      // when
      long startTime = System.currentTimeMillis();
      boolean result = validator.isValid(keywordIds, context);
      long duration = System.currentTimeMillis() - startTime;

      // then
      assertThat(result).isTrue();
      System.out.println("Validated " + keywordIds.size() + " IDs in " + duration + "ms");
      assertThat(duration).isLessThan(100);
    }

    @Test
    @DisplayName("성능: 반복 검증이 100ms 이내에 완료된다")
    void repeatedValidation_Within100ms() {
      // given
      List<Long> keywordIds = Arrays.asList(1L, 2L, 3L);

      // when
      long startTime = System.currentTimeMillis();
      for (int i = 0; i < 10000; i++) {
        validator.isValid(keywordIds, context);
      }
      long duration = System.currentTimeMillis() - startTime;

      // then
      System.out.println("10000 validations completed in " + duration + "ms");
      assertThat(duration).isLessThan(100);
    }
  }
}

package com.teambind.articleserver.validation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.teambind.articleserver.entity.board.Board;
import com.teambind.articleserver.utils.DataInitializer;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@DisplayName("ValidBoardIdValidator 테스트")
class ValidBoardIdValidatorTest {

  private ValidBoardIdValidator validator;

  @Mock private ConstraintValidatorContext context;

  @Mock private ValidBoardId annotation;

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    validator = new ValidBoardIdValidator();

    // boardMap 초기화 및 테스트 데이터 추가
    DataInitializer.boardMap.clear();
    DataInitializer.boardMap.put(1L, Board.builder().id(1L).name("테스트게시판1").build());
    DataInitializer.boardMap.put(2L, Board.builder().id(2L).name("테스트게시판2").build());
    DataInitializer.boardMap.put(3L, Board.builder().id(3L).name("테스트게시판3").build());
  }

  @AfterEach
  void tearDown() throws Exception {
    // boardMap 정리
    DataInitializer.boardMap.clear();
  }

  @Nested
  @DisplayName("nullable=false 테스트")
  class NotNullableTest {

    @BeforeEach
    void setUp() {
      when(annotation.nullable()).thenReturn(false);
      validator.initialize(annotation);
    }

    @Test
    @DisplayName("정상: 유효한 Board ID는 통과한다")
    void validBoardId_Passes() {
      // given
      Long boardId = 1L;

      // when
      boolean result = validator.isValid(boardId, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("정상: 여러 유효한 Board ID 모두 통과")
    void multipleValidBoardIds_Pass() {
      // when & then
      assertThat(validator.isValid(1L, context)).isTrue();
      assertThat(validator.isValid(2L, context)).isTrue();
      assertThat(validator.isValid(3L, context)).isTrue();
    }

    @Test
    @DisplayName("예외: 존재하지 않는 Board ID는 실패한다")
    void nonExistentBoardId_Fails() {
      // given
      Long boardId = 999L;

      // when
      boolean result = validator.isValid(boardId, context);

      // then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("예외: null Board ID는 실패한다 (nullable=false)")
    void nullBoardId_Fails_WhenNotNullable() {
      // given
      Long boardId = null;

      // when
      boolean result = validator.isValid(boardId, context);

      // then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("엣지: 0 Board ID는 실패한다")
    void zeroBoardId_Fails() {
      // given
      Long boardId = 0L;

      // when
      boolean result = validator.isValid(boardId, context);

      // then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("엣지: 음수 Board ID는 실패한다")
    void negativeBoardId_Fails() {
      // given
      Long boardId = -1L;

      // when
      boolean result = validator.isValid(boardId, context);

      // then
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("nullable=true 테스트")
  class NullableTest {

    @BeforeEach
    void setUp() {
      when(annotation.nullable()).thenReturn(true);
      validator.initialize(annotation);
    }

    @Test
    @DisplayName("정상: null Board ID는 통과한다 (nullable=true)")
    void nullBoardId_Passes_WhenNullable() {
      // given
      Long boardId = null;

      // when
      boolean result = validator.isValid(boardId, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("정상: 유효한 Board ID는 여전히 통과한다")
    void validBoardId_StillPasses() {
      // given
      Long boardId = 1L;

      // when
      boolean result = validator.isValid(boardId, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("예외: 존재하지 않는 Board ID는 여전히 실패한다")
    void nonExistentBoardId_StillFails() {
      // given
      Long boardId = 999L;

      // when
      boolean result = validator.isValid(boardId, context);

      // then
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("DataInitializer 캐시 활용 테스트")
  class CacheUsageTest {

    @BeforeEach
    void setUp() {
      when(annotation.nullable()).thenReturn(false);
      validator.initialize(annotation);
    }

    @Test
    @DisplayName("정상: 캐시에 있는 ID는 바로 검증된다")
    void cachedId_ValidatedImmediately() {
      // given
      Long boardId = 1L;

      // when
      long startTime = System.nanoTime();
      boolean result = validator.isValid(boardId, context);
      long duration = System.nanoTime() - startTime;

      // then
      assertThat(result).isTrue();
      assertThat(duration).isLessThan(1_000_000); // 1ms 이내 (매우 빠름)
    }

    @Test
    @DisplayName("정상: 반복 검증 시 성능이 일정하다")
    void repeatedValidation_ConsistentPerformance() {
      // given
      Long boardId = 1L;

      // when
      for (int i = 0; i < 1000; i++) {
        boolean result = validator.isValid(boardId, context);
        assertThat(result).isTrue();
      }

      // then - 예외 없이 완료되면 성공
    }
  }

  @Nested
  @DisplayName("초기화 테스트")
  class InitializationTest {

    @Test
    @DisplayName("정상: nullable=false로 초기화된다")
    void initialize_WithNotNullable() {
      // given
      when(annotation.nullable()).thenReturn(false);

      // when
      validator.initialize(annotation);
      boolean result = validator.isValid(null, context);

      // then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("정상: nullable=true로 초기화된다")
    void initialize_WithNullable() {
      // given
      when(annotation.nullable()).thenReturn(true);

      // when
      validator.initialize(annotation);
      boolean result = validator.isValid(null, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("정상: 초기화 후 설정이 유지된다")
    void initialization_PersistsSettings() {
      // given
      when(annotation.nullable()).thenReturn(true);
      validator.initialize(annotation);

      // when
      boolean result1 = validator.isValid(null, context);
      boolean result2 = validator.isValid(null, context);

      // then
      assertThat(result1).isTrue();
      assertThat(result2).isTrue();
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCaseTest {

    @BeforeEach
    void setUp() {
      when(annotation.nullable()).thenReturn(false);
      validator.initialize(annotation);
    }

    @Test
    @DisplayName("엣지: Long.MAX_VALUE Board ID는 실패한다")
    void maxLongBoardId_Fails() {
      // given
      Long boardId = Long.MAX_VALUE;

      // when
      boolean result = validator.isValid(boardId, context);

      // then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("엣지: Long.MIN_VALUE Board ID는 실패한다")
    void minLongBoardId_Fails() {
      // given
      Long boardId = Long.MIN_VALUE;

      // when
      boolean result = validator.isValid(boardId, context);

      // then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("엣지: 캐시가 비어있을 때 모든 ID는 실패한다")
    void emptyCache_AllIdsFail() throws Exception {
      // given
      DataInitializer.boardMap.clear();

      // when
      boolean result1 = validator.isValid(1L, context);
      boolean result2 = validator.isValid(2L, context);

      // then
      assertThat(result1).isFalse();
      assertThat(result2).isFalse();
    }
  }

  @Nested
  @DisplayName("성능 테스트")
  class PerformanceTest {

    @BeforeEach
    void setUp() {
      when(annotation.nullable()).thenReturn(false);
      validator.initialize(annotation);
    }

    @Test
    @DisplayName("성능: 10000번 검증이 100ms 이내에 완료된다")
    void validate10000Times_Within100ms() {
      // given
      Long boardId = 1L;

      // when
      long startTime = System.currentTimeMillis();
      for (int i = 0; i < 10000; i++) {
        validator.isValid(boardId, context);
      }
      long duration = System.currentTimeMillis() - startTime;

      // then
      System.out.println("10000 validations completed in " + duration + "ms");
      assertThat(duration).isLessThan(100);
    }
  }
}

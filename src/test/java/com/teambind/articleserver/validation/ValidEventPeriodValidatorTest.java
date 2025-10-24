package com.teambind.articleserver.validation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@DisplayName("ValidEventPeriodValidator 테스트")
class ValidEventPeriodValidatorTest {

  private ValidEventPeriodValidator validator;

  @Mock private ConstraintValidatorContext context;

  @Mock private ConstraintViolationBuilder violationBuilder;

  @Mock private ValidEventPeriod annotation;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    validator = new ValidEventPeriodValidator();

    // 기본 필드명 설정
    when(annotation.startDateField()).thenReturn("eventStartDate");
    when(annotation.endDateField()).thenReturn("eventEndDate");
    validator.initialize(annotation);
  }

  // 테스트용 DTO
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  static class EventDto {
    private LocalDateTime eventStartDate;
    private LocalDateTime eventEndDate;
  }

  // 커스텀 필드명 DTO
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  static class CustomEventDto {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
  }

  @Nested
  @DisplayName("정상 케이스 테스트")
  class ValidCasesTest {

    @Test
    @DisplayName("정상: 시작일 <= 종료일이면 통과한다")
    void startBeforeEnd_Passes() {
      // given
      LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
      LocalDateTime end = LocalDateTime.of(2025, 1, 31, 23, 59);
      EventDto dto = new EventDto(start, end);

      // when
      boolean result = validator.isValid(dto, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("정상: 시작일 = 종료일이면 통과한다")
    void startEqualsEnd_Passes() {
      // given
      LocalDateTime dateTime = LocalDateTime.of(2025, 1, 1, 12, 0);
      EventDto dto = new EventDto(dateTime, dateTime);

      // when
      boolean result = validator.isValid(dto, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("정상: 둘 다 null이면 통과한다 (선택적 필드)")
    void bothNull_Passes() {
      // given
      EventDto dto = new EventDto(null, null);

      // when
      boolean result = validator.isValid(dto, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("정상: 객체가 null이면 통과한다")
    void nullObject_Passes() {
      // when
      boolean result = validator.isValid(null, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("정상: 시작일이 과거, 종료일이 미래인 경우 통과한다")
    void pastStartFutureEnd_Passes() {
      // given
      LocalDateTime start = LocalDateTime.now().minusDays(7);
      LocalDateTime end = LocalDateTime.now().plusDays(7);
      EventDto dto = new EventDto(start, end);

      // when
      boolean result = validator.isValid(dto, context);

      // then
      assertThat(result).isTrue();
    }
  }

  @Nested
  @DisplayName("예외 케이스 테스트")
  class InvalidCasesTest {

    @BeforeEach
    void setUp() {
      // Mock 설정
      when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
      when(violationBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    @DisplayName("예외: 종료일이 시작일보다 이전이면 실패한다")
    void endBeforeStart_Fails() {
      // given
      LocalDateTime start = LocalDateTime.of(2025, 1, 31, 23, 59);
      LocalDateTime end = LocalDateTime.of(2025, 1, 1, 0, 0);
      EventDto dto = new EventDto(start, end);

      // when
      boolean result = validator.isValid(dto, context);

      // then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("예외: 시작일만 null이면 실패한다")
    void startNullOnly_Fails() {
      // given
      LocalDateTime end = LocalDateTime.of(2025, 1, 31, 23, 59);
      EventDto dto = new EventDto(null, end);

      // when
      boolean result = validator.isValid(dto, context);

      // then
      assertThat(result).isFalse();
      verify(context).disableDefaultConstraintViolation();
      verify(context).buildConstraintViolationWithTemplate("이벤트 시작일과 종료일은 모두 입력되어야 합니다");
    }

    @Test
    @DisplayName("예외: 종료일만 null이면 실패한다")
    void endNullOnly_Fails() {
      // given
      LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
      EventDto dto = new EventDto(start, null);

      // when
      boolean result = validator.isValid(dto, context);

      // then
      assertThat(result).isFalse();
      verify(context).disableDefaultConstraintViolation();
      verify(context).buildConstraintViolationWithTemplate("이벤트 시작일과 종료일은 모두 입력되어야 합니다");
    }

    @Test
    @DisplayName("예외: 종료일이 1초 전이어도 실패한다")
    void endOneSecondBefore_Fails() {
      // given
      LocalDateTime start = LocalDateTime.of(2025, 1, 1, 12, 0, 0);
      LocalDateTime end = LocalDateTime.of(2025, 1, 1, 11, 59, 59);
      EventDto dto = new EventDto(start, end);

      // when
      boolean result = validator.isValid(dto, context);

      // then
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("커스텀 필드명 테스트")
  class CustomFieldNamesTest {

    @BeforeEach
    void setUp() {
      // 커스텀 필드명으로 초기화
      when(annotation.startDateField()).thenReturn("startDate");
      when(annotation.endDateField()).thenReturn("endDate");
      validator.initialize(annotation);

      when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
      when(violationBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    @DisplayName("정상: 커스텀 필드명으로 검증할 수 있다")
    void customFieldNames_Success() {
      // given
      LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
      LocalDateTime end = LocalDateTime.of(2025, 1, 31, 23, 59);
      CustomEventDto dto = new CustomEventDto(start, end);

      // when
      boolean result = validator.isValid(dto, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("예외: 커스텀 필드에서도 종료일 < 시작일이면 실패한다")
    void customFields_EndBeforeStart_Fails() {
      // given
      LocalDateTime start = LocalDateTime.of(2025, 1, 31, 23, 59);
      LocalDateTime end = LocalDateTime.of(2025, 1, 1, 0, 0);
      CustomEventDto dto = new CustomEventDto(start, end);

      // when
      boolean result = validator.isValid(dto, context);

      // then
      assertThat(result).isFalse();
    }
  }

  @Nested
  @DisplayName("엣지 케이스 테스트")
  class EdgeCasesTest {

    @BeforeEach
    void setUp() {
      when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
      when(violationBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    @DisplayName("엣지: 1초 차이로 통과한다")
    void oneSecondDifference_Passes() {
      // given
      LocalDateTime start = LocalDateTime.of(2025, 1, 1, 12, 0, 0);
      LocalDateTime end = LocalDateTime.of(2025, 1, 1, 12, 0, 1);
      EventDto dto = new EventDto(start, end);

      // when
      boolean result = validator.isValid(dto, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("엣지: 나노초 차이로 통과한다")
    void nanosecondDifference_Passes() {
      // given
      LocalDateTime start = LocalDateTime.of(2025, 1, 1, 12, 0, 0, 0);
      LocalDateTime end = LocalDateTime.of(2025, 1, 1, 12, 0, 0, 1);
      EventDto dto = new EventDto(start, end);

      // when
      boolean result = validator.isValid(dto, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("엣지: 매우 긴 기간도 통과한다")
    void veryLongPeriod_Passes() {
      // given
      LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
      LocalDateTime end = LocalDateTime.of(2030, 12, 31, 23, 59);
      EventDto dto = new EventDto(start, end);

      // when
      boolean result = validator.isValid(dto, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("엣지: 년도를 넘는 기간도 통과한다")
    void crossYear_Passes() {
      // given
      LocalDateTime start = LocalDateTime.of(2024, 12, 31, 23, 59);
      LocalDateTime end = LocalDateTime.of(2025, 1, 1, 0, 0);
      EventDto dto = new EventDto(start, end);

      // when
      boolean result = validator.isValid(dto, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("엣지: 과거 날짜도 검증 가능하다")
    void pastDates_CanBeValidated() {
      // given
      LocalDateTime start = LocalDateTime.of(2020, 1, 1, 0, 0);
      LocalDateTime end = LocalDateTime.of(2020, 12, 31, 23, 59);
      EventDto dto = new EventDto(start, end);

      // when
      boolean result = validator.isValid(dto, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("엣지: 미래 날짜도 검증 가능하다")
    void futureDates_CanBeValidated() {
      // given
      LocalDateTime start = LocalDateTime.of(2030, 1, 1, 0, 0);
      LocalDateTime end = LocalDateTime.of(2030, 12, 31, 23, 59);
      EventDto dto = new EventDto(start, end);

      // when
      boolean result = validator.isValid(dto, context);

      // then
      assertThat(result).isTrue();
    }
  }

  @Nested
  @DisplayName("Reflection 에러 처리 테스트")
  class ReflectionErrorTest {

    @Test
    @DisplayName("예외: 존재하지 않는 필드명이면 실패한다")
    void nonExistentField_Fails() {
      // given
      when(annotation.startDateField()).thenReturn("nonExistentField");
      when(annotation.endDateField()).thenReturn("eventEndDate");
      validator.initialize(annotation);

      LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
      LocalDateTime end = LocalDateTime.of(2025, 1, 31, 23, 59);
      EventDto dto = new EventDto(start, end);

      // when
      boolean result = validator.isValid(dto, context);

      // then
      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("예외: 잘못된 타입의 필드면 실패한다")
    void wrongTypeField_Fails() {
      // given
      @Data
      @AllArgsConstructor
      class WrongTypeDto {
        private String eventStartDate; // String 타입
        private LocalDateTime eventEndDate;
      }

      WrongTypeDto dto = new WrongTypeDto("2025-01-01", LocalDateTime.now());

      // when & then
      assertThatThrownBy(() -> validator.isValid(dto, context))
          .isInstanceOf(ClassCastException.class);
    }
  }

  @Nested
  @DisplayName("초기화 테스트")
  class InitializationTest {

    @Test
    @DisplayName("정상: 기본 필드명으로 초기화된다")
    void initialize_WithDefaultFields() {
      // given
      when(annotation.startDateField()).thenReturn("eventStartDate");
      when(annotation.endDateField()).thenReturn("eventEndDate");

      // when
      validator.initialize(annotation);
      LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
      LocalDateTime end = LocalDateTime.of(2025, 1, 31, 23, 59);
      EventDto dto = new EventDto(start, end);

      // then
      assertThat(validator.isValid(dto, context)).isTrue();
    }

    @Test
    @DisplayName("정상: 커스텀 필드명으로 초기화된다")
    void initialize_WithCustomFields() {
      // given
      when(annotation.startDateField()).thenReturn("startDate");
      when(annotation.endDateField()).thenReturn("endDate");

      // when
      validator.initialize(annotation);
      LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
      LocalDateTime end = LocalDateTime.of(2025, 1, 31, 23, 59);
      CustomEventDto dto = new CustomEventDto(start, end);

      // then
      assertThat(validator.isValid(dto, context)).isTrue();
    }
  }

  @Nested
  @DisplayName("경계값 테스트")
  class BoundaryTest {

    @Test
    @DisplayName("엣지: LocalDateTime.MIN과 MAX 사이는 통과한다")
    void minToMax_Passes() {
      // given
      LocalDateTime start = LocalDateTime.MIN;
      LocalDateTime end = LocalDateTime.MAX;
      EventDto dto = new EventDto(start, end);

      // when
      boolean result = validator.isValid(dto, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("엣지: 같은 LocalDateTime.MIN은 통과한다")
    void sameMin_Passes() {
      // given
      EventDto dto = new EventDto(LocalDateTime.MIN, LocalDateTime.MIN);

      // when
      boolean result = validator.isValid(dto, context);

      // then
      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("엣지: 같은 LocalDateTime.MAX는 통과한다")
    void sameMax_Passes() {
      // given
      EventDto dto = new EventDto(LocalDateTime.MAX, LocalDateTime.MAX);

      // when
      boolean result = validator.isValid(dto, context);

      // then
      assertThat(result).isTrue();
    }
  }
}

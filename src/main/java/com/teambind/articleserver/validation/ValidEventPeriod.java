package com.teambind.articleserver.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 이벤트 기간 유효성 검증 어노테이션
 *
 * <p>검증 조건: - eventStartDate가 eventEndDate보다 이전이어야 함 - 클래스 레벨에 적용
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidEventPeriodValidator.class)
public @interface ValidEventPeriod {

  String message() default "이벤트 종료일은 시작일보다 이후여야 합니다";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  /** 시작일 필드명 (기본: eventStartDate) */
  String startDateField() default "eventStartDate";

  /** 종료일 필드명 (기본: eventEndDate) */
  String endDateField() default "eventEndDate";
}

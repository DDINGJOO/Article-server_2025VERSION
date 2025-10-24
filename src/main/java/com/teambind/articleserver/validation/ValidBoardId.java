package com.teambind.articleserver.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Board ID 유효성 검증 어노테이션
 *
 * <p>검증 조건: - Board ID가 존재하는지 확인 - null은 허용 (선택적 필드)
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidBoardIdValidator.class)
public @interface ValidBoardId {

  String message() default "존재하지 않는 Board ID입니다";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  /** null 값 허용 여부 (기본: true) */
  boolean nullable() default true;
}

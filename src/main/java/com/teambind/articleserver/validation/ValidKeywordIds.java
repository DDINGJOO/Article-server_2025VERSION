package com.teambind.articleserver.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Keyword ID 리스트 유효성 검증 어노테이션
 *
 * <p>검증 조건: - 모든 Keyword ID가 존재하는지 확인 - null 또는 빈 리스트는 허용 (선택적 필드)
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidKeywordIdsValidator.class)
public @interface ValidKeywordIds {

  String message() default "존재하지 않는 Keyword ID가 포함되어 있습니다";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}

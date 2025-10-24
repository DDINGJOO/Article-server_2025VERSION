package com.teambind.articleserver.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;

/** ValidEventPeriod 어노테이션의 실제 검증 로직 */
@Slf4j
public class ValidEventPeriodValidator implements ConstraintValidator<ValidEventPeriod, Object> {

  private String startDateField;
  private String endDateField;

  @Override
  public void initialize(ValidEventPeriod constraintAnnotation) {
    this.startDateField = constraintAnnotation.startDateField();
    this.endDateField = constraintAnnotation.endDateField();
  }

  @Override
  public boolean isValid(Object obj, ConstraintValidatorContext context) {
    if (obj == null) {
      return true;
    }

    try {
      Field startField = obj.getClass().getDeclaredField(startDateField);
      Field endField = obj.getClass().getDeclaredField(endDateField);

      startField.setAccessible(true);
      endField.setAccessible(true);

      LocalDateTime startDate = (LocalDateTime) startField.get(obj);
      LocalDateTime endDate = (LocalDateTime) endField.get(obj);

      // 둘 다 null이면 통과 (선택적 필드)
      if (startDate == null && endDate == null) {
        return true;
      }

      // 하나만 null이면 실패
      if (startDate == null || endDate == null) {
        context.disableDefaultConstraintViolation();
        context
            .buildConstraintViolationWithTemplate("이벤트 시작일과 종료일은 모두 입력되어야 합니다")
            .addConstraintViolation();
        return false;
      }

      // 종료일이 시작일보다 이전이면 실패
      if (endDate.isBefore(startDate)) {
        return false;
      }

      return true;

    } catch (NoSuchFieldException | IllegalAccessException e) {
      log.error("ValidEventPeriodValidator error: {}", e.getMessage());
      return false;
    }
  }
}

package com.teambind.articleserver.validation;

import com.teambind.articleserver.utils.DataInitializer;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

/**
 * ValidBoardId 어노테이션의 실제 검증 로직
 *
 * <p>DataInitializer의 캐시된 boardMap을 사용하여 DB 조회 없이 빠른 검증
 */
@Component
public class ValidBoardIdValidator implements ConstraintValidator<ValidBoardId, Long> {

  private boolean nullable;

  @Override
  public void initialize(ValidBoardId constraintAnnotation) {
    this.nullable = constraintAnnotation.nullable();
  }

  @Override
  public boolean isValid(Long boardId, ConstraintValidatorContext context) {
    // null 허용 여부 체크
    if (boardId == null) {
      return nullable;
    }

    // DataInitializer의 캐시된 맵에서 존재 여부 확인 (DB 조회 없음!)
    return DataInitializer.boardMap.containsKey(boardId);
  }
}

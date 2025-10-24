package com.teambind.articleserver.validation;

import com.teambind.articleserver.repository.BoardRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** ValidBoardId 어노테이션의 실제 검증 로직 */
@Component
@RequiredArgsConstructor
public class ValidBoardIdValidator implements ConstraintValidator<ValidBoardId, Long> {

  private final BoardRepository boardRepository;
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

    // Board 존재 여부 확인
    return boardRepository.existsById(boardId);
  }
}

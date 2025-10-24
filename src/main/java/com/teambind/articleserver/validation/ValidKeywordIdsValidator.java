package com.teambind.articleserver.validation;

import com.teambind.articleserver.repository.KeywordRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** ValidKeywordIds 어노테이션의 실제 검증 로직 */
@Component
@RequiredArgsConstructor
public class ValidKeywordIdsValidator implements ConstraintValidator<ValidKeywordIds, List<Long>> {

  private final KeywordRepository keywordRepository;

  @Override
  public boolean isValid(List<Long> keywordIds, ConstraintValidatorContext context) {
    // null 또는 빈 리스트는 허용
    if (keywordIds == null || keywordIds.isEmpty()) {
      return true;
    }

    // 모든 ID가 존재하는지 확인
    long existingCount = keywordRepository.countByIdIn(keywordIds);
    return existingCount == keywordIds.size();
  }
}

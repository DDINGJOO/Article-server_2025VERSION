package com.teambind.articleserver.validation;

import com.teambind.articleserver.common.util.DataInitializer;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * ValidKeywordIds 어노테이션의 실제 검증 로직
 *
 * <p>DataInitializer의 캐시된 keywordMap을 사용하여 DB 조회 없이 빠른 검증
 */
@Component
public class ValidKeywordIdsValidator implements ConstraintValidator<ValidKeywordIds, List<Long>> {

  @Override
  public boolean isValid(List<Long> keywordIds, ConstraintValidatorContext context) {
    // null 또는 빈 리스트는 허용
    if (keywordIds == null || keywordIds.isEmpty()) {
      return true;
    }

    // DataInitializer의 캐시된 맵에서 모든 ID 존재 여부 확인 (DB 조회 없음!)
    return keywordIds.stream().allMatch(DataInitializer.keywordMap::containsKey);
  }
}

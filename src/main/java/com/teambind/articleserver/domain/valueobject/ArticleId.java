package com.teambind.articleserver.domain.valueobject;

import com.teambind.articleserver.common.exception.CustomException;
import com.teambind.articleserver.common.exception.ErrorCode;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글 ID Value Object
 *
 * <p>DDD의 Value Object 패턴을 적용하여 ID를 도메인 개념으로 표현합니다. - 불변성(Immutability) - 자가 검증(Self-validation) -
 * 도메인 의미 부여
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleId implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final int MIN_LENGTH = 10;
  private static final int MAX_LENGTH = 50;

  private String value;

  private ArticleId(String value) {
    validate(value);
    this.value = value;
  }

  /**
   * 정적 팩토리 메서드
   *
   * @param value ID 값
   * @return ArticleId 인스턴스
   */
  public static ArticleId of(String value) {
    return new ArticleId(value);
  }

  /**
   * 새로운 ID 생성 (Snowflake 등 ID 생성기 사용)
   *
   * @param idGenerator ID 생성기
   * @return 새로운 ArticleId
   */
  public static ArticleId generate(java.util.function.Supplier<String> idGenerator) {
    return new ArticleId(idGenerator.get());
  }

  private void validate(String value) {
    if (value == null || value.isBlank()) {
      throw new CustomException(
          ErrorCode.INVALID_INPUT_VALUE, "Article ID cannot be null or empty");
    }
    if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
      throw new CustomException(
          ErrorCode.INVALID_INPUT_VALUE,
          String.format("Article ID length must be between %d and %d", MIN_LENGTH, MAX_LENGTH));
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ArticleId articleId = (ArticleId) o;
    return Objects.equals(value, articleId.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return value;
  }
}

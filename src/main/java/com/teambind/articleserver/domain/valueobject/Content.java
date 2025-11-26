package com.teambind.articleserver.domain.valueobject;

import com.teambind.articleserver.common.exception.CustomException;
import com.teambind.articleserver.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글 내용 Value Object
 *
 * <p>게시글 내용에 대한 비즈니스 규칙을 캡슐화합니다. - 최소/최대 길이 제약 - XSS 방지 처리 - 내용 분석 기능
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Content implements Serializable {

  private static final long serialVersionUID = 1L;
  private static final int MIN_LENGTH = 1;
  private static final int MAX_LENGTH = 65535; // TEXT 타입 최대 크기

  @Column(name = "contents", nullable = false, columnDefinition = "TEXT")
  private String value;

  private Content(String value) {
    validate(value);
    this.value = sanitize(value);
  }

  /**
   * 정적 팩토리 메서드
   *
   * @param value 내용 값
   * @return Content 인스턴스
   */
  public static Content of(String value) {
    return new Content(value);
  }

  private void validate(String value) {
    if (value == null || value.isBlank()) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "Content cannot be null or empty");
    }

    if (value.length() > MAX_LENGTH) {
      throw new CustomException(
          ErrorCode.INVALID_INPUT_VALUE,
          String.format("Content length cannot exceed %d characters", MAX_LENGTH));
    }
  }

  /** 내용 정제 (XSS 방지 등) */
  private String sanitize(String value) {
    return value
        .trim()
        .replaceAll("<script[^>]*>.*?</script>", "") // 스크립트 태그 제거
        .replaceAll("javascript:", "") // javascript: 프로토콜 제거
        .replaceAll("on\\w+\\s*=", ""); // 이벤트 핸들러 제거
  }

  /** 내용 요약 생성 */
  public String getExcerpt(int maxLength) {
    String plainText = value.replaceAll("<[^>]*>", "").trim();
    if (plainText.length() <= maxLength) {
      return plainText;
    }
    return plainText.substring(0, maxLength - 3) + "...";
  }

  /** 단어 수 계산 */
  public int getWordCount() {
    String plainText = value.replaceAll("<[^>]*>", "").trim();
    if (plainText.isEmpty()) {
      return 0;
    }
    return plainText.split("\\s+").length;
  }

  /** 예상 읽기 시간 (분 단위) 평균 읽기 속도: 200 words/min */
  public int getEstimatedReadTime() {
    int wordCount = getWordCount();
    int readTime = (int) Math.ceil(wordCount / 200.0);
    return Math.max(1, readTime); // 최소 1분
  }

  /** 내용 병합 (댓글 등 추가 내용 결합용) */
  public Content append(String additionalContent) {
    if (additionalContent == null || additionalContent.isBlank()) {
      return this;
    }
    return new Content(this.value + "\n\n" + additionalContent);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Content content = (Content) o;
    return Objects.equals(value, content.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return getExcerpt(100);
  }
}

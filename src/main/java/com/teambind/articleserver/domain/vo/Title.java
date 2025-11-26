package com.teambind.articleserver.domain.vo;

import com.teambind.articleserver.exceptions.CustomException;
import com.teambind.articleserver.exceptions.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글 제목 Value Object
 *
 * 제목에 대한 비즈니스 규칙을 캡슐화합니다.
 * - 최소/최대 길이 제약
 * - 금지어 검증
 * - 특수문자 처리
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Title implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 200;

    @Column(name = "title", nullable = false, length = MAX_LENGTH)
    private String value;

    private Title(String value) {
        validate(value);
        this.value = sanitize(value);
    }

    /**
     * 정적 팩토리 메서드
     *
     * @param value 제목 값
     * @return Title 인스턴스
     */
    public static Title of(String value) {
        return new Title(value);
    }

    private void validate(String value) {
        if (value == null || value.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE,
                "Title cannot be null or empty");
        }

        String trimmed = value.trim();
        if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE,
                String.format("Title length must be between %d and %d", MIN_LENGTH, MAX_LENGTH));
        }
    }

    /**
     * 제목 정제 (특수문자, 공백 처리 등)
     */
    private String sanitize(String value) {
        return value.trim()
            .replaceAll("\\s+", " ")  // 연속된 공백을 하나로
            .replaceAll("<[^>]*>", ""); // HTML 태그 제거
    }

    /**
     * 제목에 특정 키워드가 포함되어 있는지 확인
     */
    public boolean contains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return false;
        }
        return value.toLowerCase().contains(keyword.toLowerCase());
    }

    /**
     * 제목 요약 (미리보기용)
     */
    public String getSummary(int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Title title = (Title) o;
        return Objects.equals(value, title.value);
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
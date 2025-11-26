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
 * 작성자 ID Value Object
 *
 * 작성자 식별자를 도메인 개념으로 표현합니다.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WriterId implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int MAX_LENGTH = 50;

    @Column(name = "writer_id", nullable = false, length = MAX_LENGTH)
    private String value;

    private WriterId(String value) {
        validate(value);
        this.value = value;
    }

    /**
     * 정적 팩토리 메서드
     *
     * @param value 작성자 ID 값
     * @return WriterId 인스턴스
     */
    public static WriterId of(String value) {
        return new WriterId(value);
    }

    /**
     * 시스템 사용자 (자동 생성 게시글 등)
     */
    public static WriterId system() {
        return new WriterId("SYSTEM");
    }

    /**
     * 익명 사용자
     */
    public static WriterId anonymous() {
        return new WriterId("ANONYMOUS");
    }

    private void validate(String value) {
        if (value == null || value.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE,
                "Writer ID cannot be null or empty");
        }
        if (value.length() > MAX_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE,
                String.format("Writer ID cannot exceed %d characters", MAX_LENGTH));
        }
    }

    /**
     * 시스템 사용자인지 확인
     */
    public boolean isSystem() {
        return "SYSTEM".equals(value);
    }

    /**
     * 익명 사용자인지 확인
     */
    public boolean isAnonymous() {
        return "ANONYMOUS".equals(value);
    }

    /**
     * 동일 작성자 확인
     */
    public boolean isSameWriter(WriterId other) {
        return this.equals(other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WriterId writerId = (WriterId) o;
        return Objects.equals(value, writerId.value);
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
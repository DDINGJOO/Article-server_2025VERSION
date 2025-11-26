package com.teambind.articleserver.factory;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 게시글 타입 정의
 */
@Getter
@RequiredArgsConstructor
public enum ArticleType {
    REGULAR("일반 게시글"),
    EVENT("이벤트 게시글"),
    NOTICE("공지사항");

    private final String description;

    /**
     * 요청에서 타입을 결정하는 헬퍼 메서드
     */
    public static ArticleType determineType(Long boardId,
                                           String boardName,
                                           boolean hasEventPeriod) {
        // 이벤트 기간이 있으면 EVENT
        if (hasEventPeriod) {
            return EVENT;
        }

        // 보드 이름으로 타입 결정 (기존 로직 유지)
        if ("공지사항".equals(boardName)) {
            return NOTICE;
        } else if ("이벤트".equals(boardName)) {
            return EVENT;
        }

        // 기본값은 REGULAR
        return REGULAR;
    }
}
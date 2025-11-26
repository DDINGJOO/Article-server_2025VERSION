package com.teambind.articleserver.application.port.out;

/**
 * 이벤트 발행 Port (Outbound Port)
 *
 * 도메인 이벤트를 외부 시스템으로 발행합니다.
 */
public interface PublishEventPort {

    /**
     * 게시글 생성 이벤트 발행
     *
     * @param event 게시글 생성 이벤트
     */
    void publishArticleCreatedEvent(ArticleCreatedEvent event);

    /**
     * 게시글 삭제 이벤트 발행
     *
     * @param event 게시글 삭제 이벤트
     */
    void publishArticleDeletedEvent(ArticleDeletedEvent event);

    /**
     * 게시글 생성 이벤트
     */
    record ArticleCreatedEvent(
        String articleId,
        String title,
        String writerId,
        Long boardId,
        java.time.LocalDateTime occurredAt
    ) {}

    /**
     * 게시글 삭제 이벤트
     */
    record ArticleDeletedEvent(
        String articleId,
        String title,
        String writerId,
        String reason,
        java.time.LocalDateTime occurredAt
    ) {}
}
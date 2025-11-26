package com.teambind.articleserver.domain.event;

import com.teambind.articleserver.domain.valueobject.ArticleId;
import com.teambind.articleserver.domain.valueobject.Title;
import com.teambind.articleserver.domain.valueobject.WriterId;
import lombok.Getter;

/**
 * 게시글 삭제 도메인 이벤트
 *
 * 게시글이 삭제되었을 때 발생하는 이벤트입니다.
 */
@Getter
public class ArticleDeletedDomainEvent extends ArticleDomainEvent {

    private static final String EVENT_TYPE = "article.deleted";

    private final String reason;

    public ArticleDeletedDomainEvent(
            ArticleId articleId,
            Title title,
            WriterId writerId,
            String reason) {
        super(articleId, title, writerId);
        this.reason = reason;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    /**
     * 이벤트 요약 정보
     */
    public String getSummary() {
        return String.format(
            "Article deleted: id=%s, title=%s, reason=%s",
            getArticleId().getValue(),
            getTitle().getSummary(50),
            reason != null ? reason : "User request"
        );
    }
}
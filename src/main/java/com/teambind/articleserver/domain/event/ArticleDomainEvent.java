package com.teambind.articleserver.domain.event;

import com.teambind.articleserver.domain.vo.ArticleId;
import com.teambind.articleserver.domain.vo.Title;
import com.teambind.articleserver.domain.vo.WriterId;
import java.time.LocalDateTime;
import lombok.Getter;

/**
 * 게시글 도메인 이벤트 추상 클래스
 *
 * 모든 게시글 관련 이벤트의 공통 속성을 정의합니다.
 */
@Getter
public abstract class ArticleDomainEvent implements DomainEvent {

    private final String eventId;
    private final LocalDateTime occurredAt;
    private final ArticleId articleId;
    private final Title title;
    private final WriterId writerId;

    protected ArticleDomainEvent(ArticleId articleId, Title title, WriterId writerId) {
        this.eventId = DomainEvent.generateEventId();
        this.occurredAt = LocalDateTime.now();
        this.articleId = articleId;
        this.title = title;
        this.writerId = writerId;
    }

    @Override
    public String getAggregateId() {
        return articleId.getValue();
    }
}
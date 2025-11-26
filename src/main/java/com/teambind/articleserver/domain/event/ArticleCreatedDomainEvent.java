package com.teambind.articleserver.domain.event;

import com.teambind.articleserver.domain.valueobject.ArticleId;
import com.teambind.articleserver.domain.valueobject.Content;
import com.teambind.articleserver.domain.valueobject.Title;
import com.teambind.articleserver.domain.valueobject.WriterId;
import lombok.Getter;

/**
 * 게시글 생성 도메인 이벤트
 *
 * <p>게시글이 생성되었을 때 발생하는 이벤트입니다.
 */
@Getter
public class ArticleCreatedDomainEvent extends ArticleDomainEvent {

  private static final String EVENT_TYPE = "article.created";

  private final Content content;
  private final Long boardId;

  public ArticleCreatedDomainEvent(
      ArticleId articleId, Title title, Content content, WriterId writerId, Long boardId) {
    super(articleId, title, writerId);
    this.content = content;
    this.boardId = boardId;
  }

  @Override
  public String getEventType() {
    return EVENT_TYPE;
  }

  /** 이벤트 요약 정보 */
  public String getSummary() {
    return String.format(
        "Article created: id=%s, title=%s, writer=%s",
        getArticleId().getValue(), getTitle().getSummary(50), getWriterId().getValue());
  }
}

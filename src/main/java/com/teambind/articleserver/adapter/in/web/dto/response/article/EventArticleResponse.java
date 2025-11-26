package com.teambind.articleserver.adapter.in.web.dto.response.article;

import com.teambind.articleserver.adapter.out.persistence.entity.articleType.EventArticle;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 이벤트 게시글 응답 DTO
 *
 * <p>이벤트 게시글은 기간 정보(시작일, 종료일)가 추가됩니다.
 */
@Getter
@NoArgsConstructor
@SuperBuilder
public class EventArticleResponse extends ArticleBaseResponse {

  /** 이벤트 시작일 */
  private LocalDateTime eventStartDate;

  /** 이벤트 종료일 */
  private LocalDateTime eventEndDate;

  /**
   * EventArticle 엔티티로부터 EventArticleResponse 생성
   *
   * @param article 이벤트 게시글 엔티티
   * @return EventArticleResponse
   */
  public static EventArticleResponse fromEntity(EventArticle article) {
    if (article == null) {
      return null;
    }

    EventArticleResponse response =
        EventArticleResponse.builder()
            .eventStartDate(article.getEventStartDate())
            .eventEndDate(article.getEventEndDate())
            .build();

    response.setCommonFields(article);

    return response;
  }

  /**
   * 이벤트 진행 중 여부 확인
   *
   * @return 현재 시간이 이벤트 기간 내에 있으면 true
   */
  public boolean isOngoing() {
    LocalDateTime now = LocalDateTime.now();
    return eventStartDate != null
        && eventEndDate != null
        && now.isAfter(eventStartDate)
        && now.isBefore(eventEndDate);
  }

  /**
   * 이벤트 종료 여부 확인
   *
   * @return 이벤트가 종료되었으면 true
   */
  public boolean isEnded() {
    LocalDateTime now = LocalDateTime.now();
    return eventEndDate != null && now.isAfter(eventEndDate);
  }

  /**
   * 이벤트 시작 전 여부 확인
   *
   * @return 이벤트 시작 전이면 true
   */
  public boolean isUpcoming() {
    LocalDateTime now = LocalDateTime.now();
    return eventStartDate != null && now.isBefore(eventStartDate);
  }
}

package com.teambind.articleserver.dto.response.article;

import com.teambind.articleserver.entity.articleType.NoticeArticle;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 공지사항 응답 DTO
 *
 * <p>공지사항은 현재 추가 필드가 없지만, 향후 상단 고정 등의 기능 추가 가능합니다.
 */
@Getter
@NoArgsConstructor
@SuperBuilder
public class NoticeArticleResponse extends ArticleBaseResponse {

  // 향후 추가 가능한 필드들:
  // private Boolean isPinned;  // 상단 고정 여부
  // private Integer priority;  // 우선순위
  // private LocalDateTime expiryDate;  // 공지 만료일

  /**
   * NoticeArticle 엔티티로부터 NoticeArticleResponse 생성
   *
   * @param article 공지사항 엔티티
   * @return NoticeArticleResponse
   */
  public static NoticeArticleResponse fromEntity(NoticeArticle article) {
    if (article == null) {
      return null;
    }

    NoticeArticleResponse response = new NoticeArticleResponse();
    response.setCommonFields(article);

    return response;
  }
}

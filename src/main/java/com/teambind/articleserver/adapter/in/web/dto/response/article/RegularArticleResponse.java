package com.teambind.articleserver.adapter.in.web.dto.response.article;

import com.teambind.articleserver.adapter.out.persistence.entity.articleType.RegularArticle;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 일반 게시글 응답 DTO
 *
 * <p>일반 게시글은 추가 필드가 없으므로 공통 필드만 사용합니다.
 */
@Getter
@NoArgsConstructor
@SuperBuilder
public class RegularArticleResponse extends ArticleBaseResponse {

  /**
   * RegularArticle 엔티티로부터 RegularArticleResponse 생성
   *
   * @param article 일반 게시글 엔티티
   * @return RegularArticleResponse
   */
  public static RegularArticleResponse fromEntity(RegularArticle article) {
    if (article == null) {
      return null;
    }

    RegularArticleResponse response = new RegularArticleResponse();
    response.setCommonFields(article);

    return response;
  }
}

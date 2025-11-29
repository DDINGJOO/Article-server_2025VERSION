package com.teambind.articleserver.application.port.in;

import com.teambind.articleserver.adapter.in.web.dto.request.ArticleCreateRequest;
import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;

/**
 * 게시글 생성 Use Case (Inbound Port)
 *
 * <p>Hexagonal Architecture의 Inbound Port입니다. 외부에서 애플리케이션으로 들어오는 요청을 정의합니다.
 */
public interface CreateArticleUseCase {

  /**
   * 게시글 생성
   *
   * @param request 생성 요청 DTO
   * @return 생성된 게시글
   */
  Article createArticle(ArticleCreateRequest request);

  /**
   * 게시글 수정
   *
   * @param articleId 게시글 ID
   * @param request 수정 요청 DTO
   * @return 수정된 게시글
   */
  Article updateArticle(String articleId, ArticleCreateRequest request);

  /**
   * 게시글 삭제 (Soft Delete)
   *
   * @param articleId 게시글 ID
   */
  void deleteArticle(String articleId);
}

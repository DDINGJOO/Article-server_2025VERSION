package com.teambind.articleserver.application.port.in;

import com.teambind.articleserver.adapter.in.web.dto.condition.ArticleSearchCriteria;
import com.teambind.articleserver.adapter.in.web.dto.request.ArticleCreateRequest;
import com.teambind.articleserver.adapter.in.web.dto.request.ArticleCursorPageRequest;
import com.teambind.articleserver.adapter.in.web.dto.response.ArticleCursorPageResponse;
import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;

/** 게시글 관련 UseCase 인터페이스 (Inbound Port) 컨트롤러에서 사용하는 메서드들을 정의 */
public interface ArticleUseCase {

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

  /**
   * 게시글 ID로 조회
   *
   * @param articleId 게시글 ID
   * @return 조회된 게시글
   */
  Article fetchArticleById(String articleId);

  /**
   * 게시글 검색
   *
   * @param criteria 검색 조건
   * @param pageRequest 페이지 요청
   * @return 검색 결과
   */
  ArticleCursorPageResponse searchArticles(
      ArticleSearchCriteria criteria, ArticleCursorPageRequest pageRequest);
}

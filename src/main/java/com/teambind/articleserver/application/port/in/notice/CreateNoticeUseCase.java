package com.teambind.articleserver.application.port.in.notice;

import com.teambind.articleserver.adapter.in.web.dto.request.ArticleCreateRequest;
import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import com.teambind.articleserver.adapter.out.persistence.entity.articleType.NoticeArticle;

/**
 * 공지사항 생성 UseCase (Inbound Port)
 *
 * <p>Hexagonal Architecture의 Inbound Port입니다. 공지사항 생성, 수정, 삭제 작업을 정의합니다.
 */
public interface CreateNoticeUseCase {

  /**
   * 공지사항 생성
   *
   * @param request 생성 요청 DTO
   * @return 생성된 공지사항
   */
  NoticeArticle createNoticeArticle(ArticleCreateRequest request);

  /**
   * 공지사항 수정
   *
   * @param articleId 게시글 ID
   * @param request 수정 요청 DTO
   * @return 수정된 게시글
   */
  Article updateArticle(String articleId, ArticleCreateRequest request);

  /**
   * 공지사항 삭제 (Soft Delete)
   *
   * @param articleId 게시글 ID
   */
  void deleteArticle(String articleId);
}

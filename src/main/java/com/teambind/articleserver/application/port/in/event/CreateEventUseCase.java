package com.teambind.articleserver.application.port.in.event;

import com.teambind.articleserver.adapter.in.web.dto.request.ArticleCreateRequest;
import com.teambind.articleserver.adapter.out.persistence.entity.articleType.EventArticle;

/**
 * 이벤트 게시글 생성 UseCase (Inbound Port)
 *
 * <p>Hexagonal Architecture의 Inbound Port입니다. 이벤트 게시글 생성, 수정, 삭제 작업을 정의합니다.
 */
public interface CreateEventUseCase {

  /**
   * 이벤트 게시글 생성
   *
   * @param request 생성 요청 DTO
   * @return 생성된 이벤트 게시글
   */
  EventArticle createEventArticle(ArticleCreateRequest request);

  /**
   * 이벤트 게시글 수정
   *
   * @param articleId 게시글 ID
   * @param request 수정 요청 DTO
   * @return 수정된 이벤트 게시글
   */
  EventArticle updateEventArticle(String articleId, ArticleCreateRequest request);

  /**
   * 이벤트 게시글 삭제 (Soft Delete)
   *
   * @param articleId 게시글 ID
   */
  void deleteArticle(String articleId);
}

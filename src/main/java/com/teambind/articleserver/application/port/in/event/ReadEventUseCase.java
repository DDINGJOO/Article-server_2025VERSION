package com.teambind.articleserver.application.port.in.event;

import com.teambind.articleserver.adapter.in.web.dto.response.article.EventArticleResponse;
import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import org.springframework.data.domain.Page;

/**
 * 이벤트 게시글 조회 UseCase (Inbound Port)
 *
 * <p>Hexagonal Architecture의 Inbound Port입니다. 이벤트 게시글 조회 작업을 정의합니다.
 */
public interface ReadEventUseCase {

  /**
   * ID로 이벤트 게시글 조회
   *
   * @param articleId 게시글 ID
   * @return 조회된 게시글
   */
  Article fetchArticleById(String articleId);

  /**
   * 이벤트 게시글 목록 조회 (상태별 필터링 지원)
   *
   * @param status 이벤트 상태 (all, ongoing, ended, upcoming)
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @return 이벤트 게시글 페이지
   */
  Page<EventArticleResponse> getEventArticles(String status, int page, int size);
}

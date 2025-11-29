package com.teambind.articleserver.application.port.in.notice;

import com.teambind.articleserver.adapter.in.web.dto.response.article.ArticleBaseResponse;
import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import org.springframework.data.domain.Page;

/**
 * 공지사항 조회 UseCase (Inbound Port)
 *
 * <p>Hexagonal Architecture의 Inbound Port입니다. 공지사항 조회 작업을 정의합니다.
 */
public interface ReadNoticeUseCase {

  /**
   * ID로 공지사항 조회
   *
   * @param articleId 게시글 ID
   * @return 조회된 게시글
   */
  Article fetchArticleById(String articleId);

  /**
   * 공지사항 목록 조회
   *
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @return 공지사항 페이지
   */
  Page<ArticleBaseResponse> getNoticeArticles(int page, int size);
}

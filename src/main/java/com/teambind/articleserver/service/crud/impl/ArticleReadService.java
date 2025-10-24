package com.teambind.articleserver.service.crud.impl;

import com.teambind.articleserver.aop.LogTrace;
import com.teambind.articleserver.dto.condition.ArticleSearchCriteria;
import com.teambind.articleserver.dto.request.ArticleCursorPageRequest;
import com.teambind.articleserver.dto.response.ArticleCursorPageResponse;
import com.teambind.articleserver.dto.response.ArticleResponse;
import com.teambind.articleserver.dto.response.article.ArticleBaseResponse;
import com.teambind.articleserver.dto.response.article.EventArticleResponse;
import com.teambind.articleserver.entity.article.Article;
import com.teambind.articleserver.entity.articleType.EventArticle;
import com.teambind.articleserver.entity.articleType.NoticeArticle;
import com.teambind.articleserver.entity.enums.Status;
import com.teambind.articleserver.exceptions.CustomException;
import com.teambind.articleserver.exceptions.ErrorCode;
import com.teambind.articleserver.repository.ArticleRepository;
import com.teambind.articleserver.repository.ArticleRepositoryCustomImpl;
import com.teambind.articleserver.repository.EventArticleRepository;
import com.teambind.articleserver.repository.NoticeArticleRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ArticleReadService {
  private final ArticleRepository articleRepository;
  private final ArticleRepositoryCustomImpl articleRepositoryCustom;
  private final NoticeArticleRepository noticeArticleRepository;
  private final EventArticleRepository eventArticleRepository;

  @LogTrace(value = "게시글 단건 조회", logParameters = true)
  public Article fetchArticleById(String articleId) {
    Article article =
        articleRepository
            .findById(articleId)
            .orElseThrow(() -> new CustomException(ErrorCode.ARTICLE_NOT_FOUND));
    if (article.getStatus() == Status.BLOCKED) {
      throw new CustomException(ErrorCode.ARTICLE_IS_BLOCKED);
    }
    if (article.getStatus() == Status.DELETED) {
      throw new CustomException(ErrorCode.ARTICLE_NOT_FOUND);
    }
    return article;
  }

  @LogTrace(value = "게시글 검색", logParameters = false, logResult = false)
  public ArticleCursorPageResponse searchArticles(
      ArticleSearchCriteria criteria, ArticleCursorPageRequest pageRequest) {

    int size =
        (pageRequest.getSize() == null || pageRequest.getSize() <= 0) ? 20 : pageRequest.getSize();
	
    LocalDateTime effectiveCursorUpdatedAt = pageRequest.getCursorUpdatedAt();
    String cursorId = pageRequest.getCursorId();
    if (effectiveCursorUpdatedAt == null && cursorId != null && !cursorId.isBlank()) {
      Article cursorArticle = fetchArticleById(cursorId);
      effectiveCursorUpdatedAt = cursorArticle.getUpdatedAt();
    }
	
    List<Article> articles =
        articleRepositoryCustom.searchByCursor(
            criteria, effectiveCursorUpdatedAt, cursorId, size + 1);

    boolean hasNext = articles.size() > size;
    if (hasNext) {
      articles = articles.subList(0, size);
    }

    List<ArticleBaseResponse> items = articles.stream().map(ArticleResponse::fromEntity).toList();

    String nextCursorId = null;
    java.time.LocalDateTime nextCursorUpdatedAt = null;
    if (!articles.isEmpty()) {
      Article last = articles.get(articles.size() - 1);
      nextCursorId = last.getId();
      nextCursorUpdatedAt = last.getUpdatedAt();
    }

    return ArticleCursorPageResponse.builder()
        .items(items)
        .nextCursorUpdatedAt(nextCursorUpdatedAt)
        .nextCursorId(nextCursorId)
        .hasNext(hasNext)
        .size(size)
        .build();
  }

  /**
   * 공지 게시글 목록 조회
   *
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @return 공지 게시글 페이지
   */
  @LogTrace(value = "공지 게시글 목록 조회", logParameters = true)
  public Page<ArticleBaseResponse> getNoticeArticles(int page, int size) {
    Page<NoticeArticle> notices =
        noticeArticleRepository.findByStatusOrderByCreatedAtDesc(
            Status.ACTIVE, PageRequest.of(page, size));

    return notices.map(ArticleResponse::fromEntity);
  }

  /**
   * 이벤트 게시글 목록 조회 (상태별 필터링 지원)
   *
   * @param status 이벤트 상태 (all, ongoing, ended, upcoming)
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @return 이벤트 게시글 페이지
   */
  @LogTrace(value = "이벤트 게시글 목록 조회", logParameters = true)
  public Page<EventArticleResponse> getEventArticles(String status, int page, int size) {
    Page<EventArticle> events;
    LocalDateTime now = LocalDateTime.now();

    events =
        switch (status.toLowerCase()) {
          case "ongoing" ->
              eventArticleRepository.findOngoingEvents(
                  Status.ACTIVE, now, PageRequest.of(page, size));
          case "ended" ->
              eventArticleRepository.findEndedEvents(
                  Status.ACTIVE, now, PageRequest.of(page, size));
          case "upcoming" ->
              eventArticleRepository.findUpcomingEvents(
                  Status.ACTIVE, now, PageRequest.of(page, size));
          default ->
              eventArticleRepository.findByStatusOrderByCreatedAtDesc(
                  Status.ACTIVE, PageRequest.of(page, size));
        };

    return events.map(EventArticleResponse::fromEntity);
  }
}

package com.teambind.articleserver.adapter.in.web;

import com.teambind.articleserver.dto.request.ArticleCreateRequest;
import com.teambind.articleserver.dto.response.article.EventArticleResponse;
import com.teambind.articleserver.entity.articleType.EventArticle;
import com.teambind.articleserver.service.crud.impl.ArticleCreateService;
import com.teambind.articleserver.service.crud.impl.ArticleReadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 이벤트 게시글 REST Controller V1
 *
 * <p>Hexagonal Architecture의 Inbound Adapter입니다. 이벤트 게시글 전용 엔드포인트를 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Slf4j
public class EventArticleControllerV1 {

  private final ArticleCreateService articleCreateService;
  private final ArticleReadService articleReadService;

  /** 이벤트 게시글 생성 POST /api/v1/events */
  @PostMapping
  public ResponseEntity<EventArticleResponse> createEventArticle(
      @Valid @RequestBody ArticleCreateRequest request) {
    log.info("Creating event article: title={}", request.getTitle());

    EventArticle article = articleCreateService.createEventArticle(request);
    return ResponseEntity.ok(EventArticleResponse.fromEntity(article));
  }

  /** 이벤트 게시글 수정 PUT /api/v1/events/{articleId} */
  @PutMapping("/{articleId}")
  public ResponseEntity<EventArticleResponse> updateEventArticle(
      @PathVariable String articleId, @Valid @RequestBody ArticleCreateRequest request) {
    log.info("Updating event article: id={}", articleId);

    EventArticle article = articleCreateService.updateEventArticle(articleId, request);
    return ResponseEntity.ok(EventArticleResponse.fromEntity(article));
  }

  /** 이벤트 게시글 조회 GET /api/v1/events/{articleId} */
  @GetMapping("/{articleId}")
  public ResponseEntity<EventArticleResponse> fetchEventArticle(
      @PathVariable(name = "articleId") String articleId) {
    log.info("Fetching event article: id={}", articleId);

    EventArticle article = (EventArticle) articleReadService.fetchArticleById(articleId);
    return ResponseEntity.ok(EventArticleResponse.fromEntity(article));
  }

  /** 이벤트 게시글 삭제 DELETE /api/v1/events/{articleId} */
  @DeleteMapping("/{articleId}")
  public ResponseEntity<Void> deleteEventArticle(
      @PathVariable(name = "articleId") String articleId) {
    log.info("Deleting event article: id={}", articleId);

    articleCreateService.deleteArticle(articleId);
    return ResponseEntity.noContent().build();
  }

  /**
   * 이벤트 목록 조회 (상태별 필터링 지원) GET /api/v1/events
   *
   * @param status all(전체), ongoing(진행중), ended(종료), upcoming(예정)
   * @param page 페이지 번호 (0부터 시작)
   * @param size 페이지 크기
   */
  @GetMapping
  public ResponseEntity<Page<EventArticleResponse>> getEventArticles(
      @RequestParam(required = false, defaultValue = "all") String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    log.debug("Getting event articles: status={}, page={}, size={}", status, page, size);

    Page<EventArticleResponse> events = articleReadService.getEventArticles(status, page, size);
    return ResponseEntity.ok(events);
  }
}

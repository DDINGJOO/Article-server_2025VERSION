package com.teambind.articleserver.controller;

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

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Slf4j
public class EventArticleController {
  private final ArticleCreateService articleCreateService;
  private final ArticleReadService articleReadService;

  @PostMapping()
  public ResponseEntity<EventArticleResponse> createEventArticle(
      @Valid @RequestBody ArticleCreateRequest request) {
    EventArticle article = articleCreateService.createEventArticle(request);
    return ResponseEntity.ok(EventArticleResponse.fromEntity(article));
  }

  @PutMapping("/{articleId}")
  public ResponseEntity<EventArticleResponse> updateEventArticle(
      @PathVariable String articleId, @Valid @RequestBody ArticleCreateRequest request) {
    EventArticle article = articleCreateService.updateEventArticle(articleId, request);
    return ResponseEntity.ok(EventArticleResponse.fromEntity(article));
  }

  @GetMapping("/{articleId}")
  public ResponseEntity<EventArticleResponse> fetchEventArticle(
      @PathVariable(name = "articleId") String articleId) {
    EventArticle article = (EventArticle) articleReadService.fetchArticleById(articleId);

    return ResponseEntity.ok(EventArticleResponse.fromEntity(article));
  }

  @DeleteMapping("/{articleId}")
  public ResponseEntity<Void> deleteEventArticle(
      @PathVariable(name = "articleId") String articleId) {
    articleCreateService.deleteArticle(articleId);
    return ResponseEntity.noContent().build();
  }

  // 이벤트 목록 조회 (상태별 필터링 지원)
  // status: all(전체), ongoing(진행중), ended(종료), upcoming(예정)
  @GetMapping()
  public ResponseEntity<Page<EventArticleResponse>> getEventArticles(
      @RequestParam(required = false, defaultValue = "all") String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    return ResponseEntity.ok(articleReadService.getEventArticles(status, page, size));
  }
}

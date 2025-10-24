package com.teambind.articleserver.controller;

import com.teambind.articleserver.dto.request.ArticleCreateRequest;
import com.teambind.articleserver.dto.response.EventArticleResponse;
import com.teambind.articleserver.entity.articleType.EventArticle;
import com.teambind.articleserver.entity.enums.Status;
import com.teambind.articleserver.entity.keyword.Keyword;
import com.teambind.articleserver.repository.EventArticleRepository;
import com.teambind.articleserver.service.crud.impl.ArticleCreateService;
import com.teambind.articleserver.service.crud.impl.ArticleReadService;
import com.teambind.articleserver.utils.convertor.Convertor;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles/events")
@RequiredArgsConstructor
@Slf4j
public class EventArticleController {
  private final ArticleCreateService articleCreateService;
  private final ArticleReadService articleReadService;
  private final EventArticleRepository eventArticleRepository;
  private final Convertor convertor;

  @PostMapping()
  public ResponseEntity<EventArticleResponse> createEventArticle(@RequestBody ArticleCreateRequest request) {
    List<Keyword> keywords = null;
    if (request.getKeywords() != null) {
      keywords = convertor.convertKeywords(request.getKeywords());
    }

    EventArticle article =
        articleCreateService.createEventArticle(
            request.getTitle(), request.getContent(), request.getWriterId(), keywords,
            request.getEventStartDate(), request.getEventEndDate());

    log.info("이벤트 게시글이 성공적으로 저장되었습니다. article id : {}", article.getId());

    return ResponseEntity.ok(EventArticleResponse.fromEntity(article));
  }

  @PutMapping("/{articleId}")
  public ResponseEntity<EventArticleResponse> updateEventArticle(
      @RequestBody ArticleCreateRequest request, @PathVariable String articleId) {
    List<Keyword> keywords = null;
    if (request.getKeywords() != null) {
      keywords = convertor.convertKeywords(request.getKeywords());
    }

    EventArticle article =
        articleCreateService.updateEventArticle(
            articleId,
            request.getTitle(),
            request.getContent(),
            request.getWriterId(),
            keywords,
            request.getEventStartDate(),
            request.getEventEndDate());

    log.info("이벤트 게시글이 성공적으로 수정되었습니다. article id : {}", article.getId());

    return ResponseEntity.ok(EventArticleResponse.fromEntity(article));
  }

  @GetMapping("/{articleId}")
  public ResponseEntity<EventArticleResponse> fetchEventArticle(
      @PathVariable(name = "articleId") String articleId) {
    EventArticle article = (EventArticle) articleReadService.fetchArticleById(articleId);

    return ResponseEntity.ok(EventArticleResponse.fromEntity(article));
  }

  @DeleteMapping("/{articleId}")
  public ResponseEntity<Void> deleteEventArticle(@PathVariable(name = "articleId") String articleId) {
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

    Page<EventArticle> events;
    LocalDateTime now = LocalDateTime.now();

    switch (status.toLowerCase()) {
      case "ongoing":
        events = eventArticleRepository.findOngoingEvents(Status.ACTIVE, now, PageRequest.of(page, size));
        break;
      case "ended":
        events = eventArticleRepository.findEndedEvents(Status.ACTIVE, now, PageRequest.of(page, size));
        break;
      case "upcoming":
        events = eventArticleRepository.findUpcomingEvents(Status.ACTIVE, now, PageRequest.of(page, size));
        break;
      case "all":
      default:
        events = eventArticleRepository.findByStatusOrderByCreatedAtDesc(Status.ACTIVE, PageRequest.of(page, size));
        break;
    }

    Page<EventArticleResponse> response = events.map(EventArticleResponse::fromEntity);

    return ResponseEntity.ok(response);
  }
}

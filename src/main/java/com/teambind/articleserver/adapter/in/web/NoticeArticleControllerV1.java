package com.teambind.articleserver.adapter.in.web;

import com.teambind.articleserver.adapter.in.web.dto.request.ArticleCreateRequest;
import com.teambind.articleserver.adapter.in.web.dto.response.ArticleResponse;
import com.teambind.articleserver.adapter.in.web.dto.response.article.ArticleBaseResponse;
import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import com.teambind.articleserver.adapter.out.persistence.entity.articleType.NoticeArticle;
import com.teambind.articleserver.application.port.in.notice.CreateNoticeUseCase;
import com.teambind.articleserver.application.port.in.notice.ReadNoticeUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 공지사항 REST Controller V1
 *
 * <p>Hexagonal Architecture의 Inbound Adapter입니다. 공지사항 전용 엔드포인트를 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
@Slf4j
public class NoticeArticleControllerV1 {

  private final CreateNoticeUseCase createNoticeUseCase;
  private final ReadNoticeUseCase readNoticeUseCase;

  /** 공지사항 생성 POST /api/v1/notices */
  @PostMapping
  public ResponseEntity<ArticleBaseResponse> createNoticeArticle(
      @Valid @RequestBody ArticleCreateRequest request) {
    log.info("Creating notice article: title={}", request.getTitle());

    NoticeArticle article = createNoticeUseCase.createNoticeArticle(request);
    return ResponseEntity.ok(ArticleResponse.fromEntity(article));
  }

  /** 공지사항 수정 PUT /api/v1/notices/{articleId} */
  @PutMapping("/{articleId}")
  public ResponseEntity<ArticleBaseResponse> updateNoticeArticle(
      @PathVariable String articleId, @Valid @RequestBody ArticleCreateRequest request) {
    log.info("Updating notice article: id={}", articleId);

    Article article = createNoticeUseCase.updateArticle(articleId, request);
    return ResponseEntity.ok(ArticleResponse.fromEntity(article));
  }

  /** 공지사항 조회 GET /api/v1/notices/{articleId} */
  @GetMapping("/{articleId}")
  public ResponseEntity<ArticleBaseResponse> fetchNoticeArticle(
      @PathVariable(name = "articleId") String articleId) {
    log.info("Fetching notice article: id={}", articleId);

    Article article = readNoticeUseCase.fetchArticleById(articleId);
    return ResponseEntity.ok(ArticleResponse.fromEntity(article));
  }

  /** 공지사항 삭제 DELETE /api/v1/notices/{articleId} */
  @DeleteMapping("/{articleId}")
  public ResponseEntity<Void> deleteNoticeArticle(
      @PathVariable(name = "articleId") String articleId) {
    log.info("Deleting notice article: id={}", articleId);

    createNoticeUseCase.deleteArticle(articleId);
    return ResponseEntity.noContent().build();
  }

  /**
   * 공지사항 목록 조회 GET /api/v1/notices
   *
   * @param page 페이지 번호 (0부터 시작)
   * @param size 페이지 크기
   */
  @GetMapping
  public ResponseEntity<Page<ArticleBaseResponse>> getNoticeArticles(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

    log.debug("Getting notice articles: page={}, size={}", page, size);

    Page<ArticleBaseResponse> notices = readNoticeUseCase.getNoticeArticles(page, size);
    return ResponseEntity.ok(notices);
  }
}

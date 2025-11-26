package com.teambind.articleserver.adapter.in.web;

import static com.teambind.articleserver.common.util.DataInitializer.boardMap;
import static com.teambind.articleserver.common.util.DataInitializer.keywordMap;

import com.teambind.articleserver.adapter.in.web.dto.condition.ArticleSearchCriteria;
import com.teambind.articleserver.adapter.in.web.dto.request.ArticleCreateRequest;
import com.teambind.articleserver.adapter.in.web.dto.request.ArticleCursorPageRequest;
import com.teambind.articleserver.adapter.in.web.dto.response.ArticleCursorPageResponse;
import com.teambind.articleserver.adapter.in.web.dto.response.ArticleResponse;
import com.teambind.articleserver.adapter.in.web.dto.response.article.ArticleBaseResponse;
import com.teambind.articleserver.adapter.out.persistence.entity.article.Article;
import com.teambind.articleserver.adapter.out.persistence.entity.enums.Status;
import com.teambind.articleserver.service.crud.impl.ArticleCreateService;
import com.teambind.articleserver.service.crud.impl.ArticleReadService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 게시글 REST Controller V1
 *
 * <p>Hexagonal Architecture의 Inbound Adapter입니다. 기존 v1 API의 호환성을 유지하면서 Ports & Adapters 패턴을 적용합니다.
 */
@RestController
@RequestMapping("/api/v1/articles")
@RequiredArgsConstructor
@Slf4j
public class ArticleControllerV1 {

  private final ArticleCreateService articleCreateService;
  private final ArticleReadService articleReadService;

  /** 게시글 생성 POST /api/v1/articles */
  @PostMapping
  public ResponseEntity<ArticleBaseResponse> createArticle(
      @Valid @RequestBody ArticleCreateRequest request) {
    log.info("Creating article: title={}", request.getTitle());

    Article article = articleCreateService.createArticle(request);
    return ResponseEntity.ok(ArticleResponse.fromEntity(article));
  }

  /** 게시글 수정 PUT /api/v1/articles/{articleId} */
  @PutMapping("/{articleId}")
  public ResponseEntity<ArticleBaseResponse> updateArticle(
      @PathVariable String articleId, @Valid @RequestBody ArticleCreateRequest request) {
    log.info("Updating article: id={}", articleId);

    Article article = articleCreateService.updateArticle(articleId, request);
    return ResponseEntity.ok(ArticleResponse.fromEntity(article));
  }

  /** 게시글 조회 GET /api/v1/articles/{articleId} */
  @GetMapping("/{articleId}")
  public ResponseEntity<ArticleBaseResponse> fetchArticle(
      @PathVariable(name = "articleId") String articleId) {
    log.info("Fetching article: id={}", articleId);

    Article article = articleReadService.fetchArticleById(articleId);
    return ResponseEntity.ok(ArticleResponse.fromEntity(article));
  }

  /** 게시글 삭제 (Soft Delete) DELETE /api/v1/articles/{articleId} */
  @DeleteMapping("/{articleId}")
  public ResponseEntity<Void> deleteArticle(@PathVariable(name = "articleId") String articleId) {
    log.info("Deleting article: id={}", articleId);

    articleCreateService.deleteArticle(articleId);
    return ResponseEntity.noContent().build();
  }

  /**
   * 게시글 검색 GET /api/v1/articles/search
   *
   * <p>커서 기반 페이징을 지원합니다.
   */
  @GetMapping("/search")
  public ResponseEntity<ArticleCursorPageResponse> searchArticles(
      @RequestParam(required = false) Integer size,
      @RequestParam(required = false) String cursorId,
      @RequestParam(required = false) Long boardIds,
      @RequestParam(required = false, name = "keyword") List<Long> keywordIds,
      @RequestParam(required = false) String title,
      @RequestParam(required = false) String content,
      @RequestParam(required = false) String writerId) {

    log.debug(
        "Searching articles with params: boardIds={}, keywordIds={}, title={}, writerId={}",
        boardIds,
        keywordIds,
        title,
        writerId);

    // 검색 조건 구성
    ArticleSearchCriteria.ArticleSearchCriteriaBuilder criteriaBuilder =
        ArticleSearchCriteria.builder();

    if (boardIds != null) {
      criteriaBuilder.board(boardMap.get(boardIds));
    }

    if (keywordIds != null) {
      criteriaBuilder.keywords(keywordIds.stream().map(keywordMap::get).toList());
    }

    if (title != null && !title.isBlank()) {
      criteriaBuilder.title(title);
    }

    if (content != null && !content.isBlank()) {
      criteriaBuilder.content(content);
    }

    if (writerId != null && !writerId.isBlank()) {
      criteriaBuilder.writerId(writerId);
    }

    // 기본값 설정
    if (size == null || size <= 0) {
      size = 10;
    }

    criteriaBuilder.status(Status.ACTIVE);

    ArticleSearchCriteria criteria = criteriaBuilder.build();

    // 페이지 요청 구성
    ArticleCursorPageRequest pageRequest =
        ArticleCursorPageRequest.builder().size(size).cursorId(cursorId).build();

    ArticleCursorPageResponse response = articleReadService.searchArticles(criteria, pageRequest);

    return ResponseEntity.ok(response);
  }
}

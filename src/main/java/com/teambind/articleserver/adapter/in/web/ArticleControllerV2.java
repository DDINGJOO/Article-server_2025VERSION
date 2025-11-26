package com.teambind.articleserver.adapter.in.web;

import static com.teambind.articleserver.utils.DataInitializer.boardMap;
import static com.teambind.articleserver.utils.DataInitializer.keywordMap;

import com.teambind.articleserver.dto.condition.ArticleSearchCriteria;
import com.teambind.articleserver.dto.request.ArticleCreateRequest;
import com.teambind.articleserver.dto.request.ArticleCursorPageRequest;
import com.teambind.articleserver.dto.response.ArticleCursorPageResponse;
import com.teambind.articleserver.dto.response.ArticleResponse;
import com.teambind.articleserver.dto.response.article.ArticleBaseResponse;
import com.teambind.articleserver.entity.article.Article;
import com.teambind.articleserver.entity.enums.Status;
import com.teambind.articleserver.service.crud.impl.ArticleCreateService;
import com.teambind.articleserver.service.crud.impl.ArticleReadService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 게시글 REST Controller V2
 *
 * <p>v1과 완전히 동일한 요청/응답 구조를 제공합니다. URL 경로만 v1에서 v2로 변경되었습니다.
 */
@RestController
@RequestMapping("/api/v2/articles")
@RequiredArgsConstructor
@Slf4j
public class ArticleControllerV2 {

  private final ArticleCreateService articleCreateService;
  private final ArticleReadService articleReadService;

  /** 게시글 생성 POST /api/v2/articles */
  @PostMapping
  public ResponseEntity<ArticleBaseResponse> createArticle(
      @Valid @RequestBody ArticleCreateRequest request) {
    log.info("Creating article v2: title={}", request.getTitle());

    Article article = articleCreateService.createArticle(request);
    return ResponseEntity.ok(ArticleResponse.fromEntity(article));
  }

  /** 게시글 수정 PUT /api/v2/articles/{articleId} */
  @PutMapping("/{articleId}")
  public ResponseEntity<ArticleBaseResponse> updateArticle(
      @PathVariable String articleId, @Valid @RequestBody ArticleCreateRequest request) {
    log.info("Updating article v2: id={}", articleId);

    Article article = articleCreateService.updateArticle(articleId, request);
    return ResponseEntity.ok(ArticleResponse.fromEntity(article));
    }

  /** 게시글 조회 GET /api/v2/articles/{articleId} */
  @GetMapping("/{articleId}")
  public ResponseEntity<ArticleBaseResponse> fetchArticle(
      @PathVariable(name = "articleId") String articleId) {
    log.info("Fetching article v2: id={}", articleId);

    Article article = articleReadService.fetchArticleById(articleId);
    return ResponseEntity.ok(ArticleResponse.fromEntity(article));
  }

  /** 게시글 삭제 (Soft Delete) DELETE /api/v2/articles/{articleId} */
  @DeleteMapping("/{articleId}")
  public ResponseEntity<Void> deleteArticle(@PathVariable(name = "articleId") String articleId) {
    log.info("Deleting article v2: id={}", articleId);

    articleCreateService.deleteArticle(articleId);
    return ResponseEntity.noContent().build();
    }

  /**
   * 게시글 검색 GET /api/v2/articles/search
   *
   * <p>v1과 동일한 파라미터 및 응답 구조를 제공합니다.
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
        "Searching articles v2 with params: boardIds={}, keywordIds={}, title={}, writerId={}",
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

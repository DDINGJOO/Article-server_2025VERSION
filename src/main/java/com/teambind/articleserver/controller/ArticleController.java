package com.teambind.articleserver.controller;

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

@RestController
@RequestMapping("/api/v1/articles")
@RequiredArgsConstructor
@Slf4j
public class ArticleController {
  private final ArticleCreateService articleCreateService;
  private final ArticleReadService articleReadService;

  @PostMapping()
  public ResponseEntity<ArticleBaseResponse> createArticle(
      @Valid @RequestBody ArticleCreateRequest request) {
    Article article = articleCreateService.createArticle(request);
    return ResponseEntity.ok(ArticleResponse.fromEntity(article));
  }

  @PutMapping("/{articleId}")
  public ResponseEntity<ArticleBaseResponse> updateArticle(
      @PathVariable String articleId, @Valid @RequestBody ArticleCreateRequest request) {
    Article article = articleCreateService.updateArticle(articleId, request);
    return ResponseEntity.ok(ArticleResponse.fromEntity(article));
  }

  @GetMapping("/{articleId}")
  public ResponseEntity<ArticleBaseResponse> fetchArticle(
      @PathVariable(name = "articleId") String articleId) {
    Article article = articleReadService.fetchArticleById(articleId);

    return ResponseEntity.ok(ArticleResponse.fromEntity(article));
  }

  @DeleteMapping("/{articleId}")
  public ResponseEntity<Void> deleteArticle(@PathVariable(name = "articleId") String articleId) {
    articleCreateService.deleteArticle(articleId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/search")
  public ResponseEntity<ArticleCursorPageResponse> searchGet(
      @RequestParam(required = false) Integer size,
      @RequestParam(required = false) String cursorId,
      @RequestParam(required = false) Long boardIds,
      @RequestParam(required = false, name = "keyword") List<Long> keywordIds,
      @RequestParam(required = false) String title,
      @RequestParam(required = false) String content,
      @RequestParam(required = false) String writerId) {

    ArticleSearchCriteria.ArticleSearchCriteriaBuilder criteriaBuilder =
        ArticleSearchCriteria.builder();
    if (boardIds != null) criteriaBuilder.board(boardMap.get(boardIds));
    if (keywordIds != null)
      criteriaBuilder.keywords(keywordIds.stream().map(keywordMap::get).toList());
    if (title != null && !title.isBlank()) criteriaBuilder.title(title);
    if (content != null && !content.isBlank()) criteriaBuilder.content(content);
    if (writerId != null && !writerId.isBlank()) criteriaBuilder.writerId(writerId);
    if (size == null || size <= 0) size = 10;
    criteriaBuilder.status(Status.ACTIVE);

    ArticleSearchCriteria criteria = criteriaBuilder.build();

    ArticleCursorPageRequest pageRequest =
        ArticleCursorPageRequest.builder().size(size).cursorId(cursorId).build();

    return ResponseEntity.ok(articleReadService.searchArticles(criteria, pageRequest));
  }
}

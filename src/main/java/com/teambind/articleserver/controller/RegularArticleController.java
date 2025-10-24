package com.teambind.articleserver.controller;

import com.teambind.articleserver.dto.condition.ArticleSearchCriteria;
import com.teambind.articleserver.dto.request.ArticleCreateRequest;
import com.teambind.articleserver.dto.request.ArticleCursorPageRequest;
import com.teambind.articleserver.dto.response.ArticleCursorPageResponse;
import com.teambind.articleserver.dto.response.ArticleResponse;
import com.teambind.articleserver.dto.response.article.ArticleBaseResponse;
import com.teambind.articleserver.entity.article.Article;
import com.teambind.articleserver.entity.articleType.RegularArticle;
import com.teambind.articleserver.entity.enums.Status;
import com.teambind.articleserver.service.crud.impl.ArticleCreateService;
import com.teambind.articleserver.service.crud.impl.ArticleReadService;
import com.teambind.articleserver.utils.DataInitializer;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles/regular")
@RequiredArgsConstructor
@Slf4j
public class RegularArticleController {
  private final ArticleCreateService articleCreateService;
  private final ArticleReadService articleReadService;

  @PostMapping()
  public ResponseEntity<ArticleBaseResponse> createRegularArticle(
      @Valid @RequestBody ArticleCreateRequest request) {
    RegularArticle article = articleCreateService.createRegularArticle(request);
    return ResponseEntity.ok(ArticleResponse.fromEntity(article));
  }

  @PutMapping("/{articleId}")
  public ResponseEntity<ArticleBaseResponse> updateRegularArticle(
      @PathVariable String articleId, @Valid @RequestBody ArticleCreateRequest request) {
    Article article = articleCreateService.updateArticle(articleId, request);
    return ResponseEntity.ok(ArticleResponse.fromEntity(article));
  }

  @GetMapping("/{articleId}")
  public ResponseEntity<ArticleBaseResponse> fetchRegularArticle(
      @PathVariable(name = "articleId") String articleId) {
    Article article = articleReadService.fetchArticleById(articleId);

    return ResponseEntity.ok(ArticleResponse.fromEntity(article));
  }

  @DeleteMapping("/{articleId}")
  public ResponseEntity<Void> deleteRegularArticle(@PathVariable(name = "articleId") String articleId) {
    articleCreateService.deleteArticle(articleId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/search")
  public ResponseEntity<ArticleCursorPageResponse> searchRegularArticles(
      @RequestParam(required = false) Integer size,
      @RequestParam(required = false) String cursorId,
      @RequestParam(required = false) Long boardIds,
      @RequestParam(required = false, name = "keyword") List<Long> keywordIds,
      @RequestParam(required = false) String title,
      @RequestParam(required = false) String content,
      @RequestParam(required = false, name = "writerIds") List<String> writerIds) {

    ArticleSearchCriteria.ArticleSearchCriteriaBuilder criteriaBuilder =
        ArticleSearchCriteria.builder();

    if (boardIds != null) {
      criteriaBuilder.board(DataInitializer.boardMap.get(boardIds));
    }
    if (keywordIds != null && !keywordIds.isEmpty()) {
      criteriaBuilder.keywords(keywordIds.stream().map(DataInitializer.keywordMap::get).toList());
    }
    if (title != null && !title.isBlank()) criteriaBuilder.title(title);
    if (content != null && !content.isBlank()) criteriaBuilder.content(content);
    if (writerIds != null && !writerIds.isEmpty()) criteriaBuilder.writerId(writerIds);
    if (size == null || size <= 0) size = 10;
    criteriaBuilder.status(Status.ACTIVE);

    ArticleSearchCriteria criteria = criteriaBuilder.build();

    ArticleCursorPageRequest pageRequest =
        ArticleCursorPageRequest.builder().size(size).cursorId(cursorId).build();

    return ResponseEntity.ok(articleReadService.searchArticles(criteria, pageRequest));
  }
}

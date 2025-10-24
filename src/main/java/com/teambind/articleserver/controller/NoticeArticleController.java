package com.teambind.articleserver.controller;

import com.teambind.articleserver.dto.request.ArticleCreateRequest;
import com.teambind.articleserver.dto.response.ArticleResponse;
import com.teambind.articleserver.dto.response.article.ArticleBaseResponse;
import com.teambind.articleserver.entity.article.Article;
import com.teambind.articleserver.entity.articleType.NoticeArticle;
import com.teambind.articleserver.service.crud.impl.ArticleCreateService;
import com.teambind.articleserver.service.crud.impl.ArticleReadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
@Slf4j
public class NoticeArticleController {
  private final ArticleCreateService articleCreateService;
  private final ArticleReadService articleReadService;

  @PostMapping()
  public ResponseEntity<ArticleBaseResponse> createNoticeArticle(
      @Valid @RequestBody ArticleCreateRequest request) {
    NoticeArticle article = articleCreateService.createNoticeArticle(request);
    return ResponseEntity.ok(ArticleResponse.fromEntity(article));
  }

  @PutMapping("/{articleId}")
  public ResponseEntity<ArticleBaseResponse> updateNoticeArticle(
      @PathVariable String articleId, @Valid @RequestBody ArticleCreateRequest request) {
    Article article = articleCreateService.updateArticle(articleId, request);
    return ResponseEntity.ok(ArticleResponse.fromEntity(article));
  }

  @GetMapping("/{articleId}")
  public ResponseEntity<ArticleBaseResponse> fetchNoticeArticle(
      @PathVariable(name = "articleId") String articleId) {
    Article article = articleReadService.fetchArticleById(articleId);

    return ResponseEntity.ok(ArticleResponse.fromEntity(article));
  }

  @DeleteMapping("/{articleId}")
  public ResponseEntity<Void> deleteNoticeArticle(@PathVariable(name = "articleId") String articleId) {
    articleCreateService.deleteArticle(articleId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping()
  public ResponseEntity<Page<ArticleBaseResponse>> getNoticeArticles(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
    return ResponseEntity.ok(articleReadService.getNoticeArticles(page, size));
  }
}

package com.teambind.articleserver.controller;

import com.teambind.articleserver.dto.request.ArticleCreateRequest;
import com.teambind.articleserver.dto.response.ArticleResponse;
import com.teambind.articleserver.entity.Article;
import com.teambind.articleserver.entity.Board;
import com.teambind.articleserver.entity.Keyword;
import com.teambind.articleserver.service.crud.impl.ArticleCreateService;
import com.teambind.articleserver.service.crud.impl.ArticleReadService;
import com.teambind.articleserver.utils.convertor.Convertor;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@Slf4j
public class ArticleController {
  private final ArticleCreateService articleCreateService;
  private final ArticleReadService articleReadService;
  private final Convertor convertor;

  @PostMapping()
  public ResponseEntity<String> createArticle(@RequestBody ArticleCreateRequest request) {
    List<Keyword> keywords = convertor.convertKeywords(request.getKeywords());
    Board board = convertor.convertBoard(request.getBoard());

    Article article =
        articleCreateService.createArticle(
            request.getTitle(), request.getContent(), request.getWriterId(), board, keywords);

    log.info("게시글이 성공적으로 저장되었습니다. article id : {}", article.getId());

    return ResponseEntity.ok(article.getId());
  }

  @GetMapping("/{articleId}")
  public ResponseEntity<ArticleResponse> fetchArticle(
      @PathVariable(name = "articleId") String articleId) {
    Article article = articleReadService.fetchArticleById(articleId);

    return ResponseEntity.ok(ArticleResponse.fromEntity(article));
  }
}

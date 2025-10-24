package com.teambind.articleserver.controller;

import com.teambind.articleserver.dto.condition.ArticleSearchCriteria;
import com.teambind.articleserver.dto.request.ArticleCreateRequest;
import com.teambind.articleserver.dto.request.ArticleCursorPageRequest;
import com.teambind.articleserver.dto.response.ArticleCursorPageResponse;
import com.teambind.articleserver.dto.response.ArticleResponse;
import com.teambind.articleserver.entity.article.Article;
import com.teambind.articleserver.entity.board.Board;
import com.teambind.articleserver.entity.enums.Status;
import com.teambind.articleserver.entity.keyword.Keyword;
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
  public ResponseEntity<ArticleResponse> createArticle(@RequestBody ArticleCreateRequest request) {
    List<Keyword> keywords = null;
    if (request.getKeywords() != null) {
      keywords = convertor.convertKeywords(request.getKeywords());
    }
    Board board = convertor.convertBoard(request.getBoard());

    Article article =
        articleCreateService.createArticle(
            request.getTitle(), request.getContent(), request.getWriterId(),board,  keywords);

    log.info("게시글이 성공적으로 저장되었습니다. article id : {}", article.getId());

    return ResponseEntity.ok(ArticleResponse.fromEntity(article));
  }


  @PutMapping("/{articleId}")
  public ResponseEntity<ArticleResponse> updateArticle(
      @RequestBody ArticleCreateRequest request, @PathVariable String articleId) {
    List<Keyword> keywords = null;
    if (request.getKeywords() != null) {
      keywords = convertor.convertKeywords(request.getKeywords());
    }
    Board board = convertor.convertBoard(request.getBoard());

    Article article =
        articleCreateService.updateArticle(
            articleId,
            request.getTitle(),
            request.getContent(),
            request.getWriterId(),
            board,
            keywords);

    log.info("게시글이 성공적으로 저장되었습니다. article id : {}", article.getId());

    return ResponseEntity.ok(ArticleResponse.fromEntity(article));
  }


  @GetMapping("/{articleId}")
  public ResponseEntity<ArticleResponse> fetchArticle(
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
      @RequestParam(required = false) String board,
      @RequestParam(required = false, name = "keyword") List<String> keyword,
      @RequestParam(required = false) String title,
      @RequestParam(required = false) String content,
      @RequestParam(required = false, name = "writerIds") List<String> writerIds) {

    ArticleSearchCriteria.ArticleSearchCriteriaBuilder criteriaBuilder =
        ArticleSearchCriteria.builder();

    if (board != null && !board.isBlank()) {
      String safeBoard = board;
      try {
        if (safeBoard.contains("%") || safeBoard.contains("+")) {
          safeBoard =
              java.net.URLDecoder.decode(safeBoard, java.nio.charset.StandardCharsets.UTF_8);
        }
      } catch (IllegalArgumentException e) {
        log.warn("Failed to URL decode board param '{}', using original", board);
      }
      criteriaBuilder.board(convertor.convertBoard(safeBoard));
    }
    if (keyword != null && !keyword.isEmpty()) {
      // 방어적으로 각 키워드 항목에 대해 URL 디코딩 시도 (필요 시)
      List<String> safeKeywords = new java.util.ArrayList<>(keyword.size());
      for (String k : keyword) {
        String v = k;
        if (v != null && (v.contains("%") || v.contains("+"))) {
          try {
            v = java.net.URLDecoder.decode(v, java.nio.charset.StandardCharsets.UTF_8);
          } catch (IllegalArgumentException e) {
            log.warn("Failed to URL decode keyword '{}', using original", k);
          }
        }
        safeKeywords.add(v);
      }
      criteriaBuilder.keywords(convertor.convertKeywords(safeKeywords));
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

package com.teambind.articleserver.controller;

import com.teambind.articleserver.dto.condition.ArticleSearchCriteria;
import com.teambind.articleserver.dto.request.ArticleCreateRequest;
import com.teambind.articleserver.dto.request.ArticleCursorPageRequest;
import com.teambind.articleserver.dto.response.ArticleCursorPageResponse;
import com.teambind.articleserver.dto.response.ArticleResponse;
import com.teambind.articleserver.entity.Article;
import com.teambind.articleserver.entity.Board;
import com.teambind.articleserver.entity.Keyword;
import com.teambind.articleserver.entity.enums.Status;
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
    List<Keyword> keywords = null;
    if (request.getKeywords() != null) {
      keywords = convertor.convertKeywords(request.getKeywords());
    }
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
  

  @GetMapping("/search")
  public ResponseEntity<ArticleCursorPageResponse> searchGet(
      @RequestParam(required = false) Integer size,
      @RequestParam(required = false) String cursorId,
      @RequestParam(required = false) Object board,
      @RequestParam(required = false) List<?> keyword,
      @RequestParam(required = false) String title,
      @RequestParam(required = false) String content,
      @RequestParam(required = false, name = "writerIds") List<String> writerIds) {
	  
    ArticleSearchCriteria.ArticleSearchCriteriaBuilder criteriaBuilder =
        ArticleSearchCriteria.builder();

    if (board != null) {
      criteriaBuilder.board(convertor.convertBoard(board));
    }
    if (keyword != null && !keyword.isEmpty()) {
      criteriaBuilder.keywords(convertor.convertKeywords(keyword));
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

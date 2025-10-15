package com.teambind.articleserver.controller;

import com.teambind.articleserver.dto.request.ArticleCreateRequest;
import com.teambind.articleserver.dto.response.ArticleResponse;
import com.teambind.articleserver.entity.Article;
import com.teambind.articleserver.entity.Keyword;
import com.teambind.articleserver.entity.NoticeArticle;
import com.teambind.articleserver.entity.enums.Status;
import com.teambind.articleserver.repository.NoticeArticleRepository;
import com.teambind.articleserver.service.crud.impl.ArticleCreateService;
import com.teambind.articleserver.service.crud.impl.ArticleReadService;
import com.teambind.articleserver.utils.convertor.Convertor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles/notices")
@RequiredArgsConstructor
@Slf4j
public class NoticeArticleController {
  private final ArticleCreateService articleCreateService;
  private final ArticleReadService articleReadService;
  private final NoticeArticleRepository noticeArticleRepository;
  private final Convertor convertor;

  @PostMapping()
  public ResponseEntity<ArticleResponse> createNoticeArticle(@RequestBody ArticleCreateRequest request) {
    List<Keyword> keywords = null;
    if (request.getKeywords() != null) {
      keywords = convertor.convertKeywords(request.getKeywords());
    }

    NoticeArticle article =
        articleCreateService.createNoticeArticle(
            request.getTitle(), request.getContent(), request.getWriterId(), keywords);

    log.info("공지사항이 성공적으로 저장되었습니다. article id : {}", article.getId());

    return ResponseEntity.ok(ArticleResponse.fromEntity(article));
  }

  @PutMapping("/{articleId}")
  public ResponseEntity<ArticleResponse> updateNoticeArticle(
      @RequestBody ArticleCreateRequest request, @PathVariable String articleId) {
    List<Keyword> keywords = null;
    if (request.getKeywords() != null) {
      keywords = convertor.convertKeywords(request.getKeywords());
    }

    Article article = articleCreateService.updateArticle(
        articleId,
        request.getTitle(),
        request.getContent(),
        request.getWriterId(),
        null, // Board는 변경하지 않음 (항상 "공지사항" Board)
        keywords);

    log.info("공지사항이 성공적으로 수정되었습니다. article id : {}", article.getId());

    return ResponseEntity.ok(ArticleResponse.fromEntity(article));
  }

  @GetMapping("/{articleId}")
  public ResponseEntity<ArticleResponse> fetchNoticeArticle(
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
  public ResponseEntity<Page<ArticleResponse>> getNoticeArticles(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    Page<NoticeArticle> notices = noticeArticleRepository.findByStatusOrderByCreatedAtDesc(
        Status.ACTIVE, PageRequest.of(page, size));

    Page<ArticleResponse> response = notices.map(ArticleResponse::fromEntity);

    return ResponseEntity.ok(response);
  }
}

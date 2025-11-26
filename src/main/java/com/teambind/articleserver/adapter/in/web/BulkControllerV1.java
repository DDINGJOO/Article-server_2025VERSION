package com.teambind.articleserver.adapter.in.web;

import com.teambind.articleserver.adapter.in.web.dto.response.ArticleSimpleResponse;
import com.teambind.articleserver.service.bulk.BulkReadService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 벌크 작업 REST Controller V1
 *
 * <p>Hexagonal Architecture의 Inbound Adapter입니다. 다중 게시글 조회 등의 벌크 작업을 처리합니다.
 */
@RestController
@RequestMapping("/api/v1/bulk")
@RequiredArgsConstructor
@Slf4j
public class BulkControllerV1 {

  private final BulkReadService bulkReadService;

  /**
   * 다중 게시글 조회 GET /api/v1/bulk/articles?ids=1&ids=2&ids=3
   *
   * @param ids 조회할 게시글 ID 목록
   * @return 게시글 간략 정보 목록
   */
  @GetMapping("/articles")
  public ResponseEntity<List<ArticleSimpleResponse>> bulkArticles(
      @RequestParam(name = "ids") List<String> ids) {

    log.info("Bulk fetching articles: count={}", ids.size());
    log.debug("Article IDs: {}", ids);

    List<ArticleSimpleResponse> articles = bulkReadService.fetchSimpleByIds(ids);

    log.info("Bulk fetch completed: found={} of {}", articles.size(), ids.size());

    return ResponseEntity.ok(articles);
  }
}

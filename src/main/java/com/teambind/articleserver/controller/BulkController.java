package com.teambind.articleserver.controller;

import com.teambind.articleserver.dto.response.ArticleSimpleResponse;
import com.teambind.articleserver.service.bulk.BulkReadService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bulk")
@RequiredArgsConstructor
public class BulkController {

  private final BulkReadService bulkReadService;

  // Example: GET /api/v1/bulk/articles?ids=1&ids=2&ids=3
  @GetMapping("/articles")
  public ResponseEntity<List<ArticleSimpleResponse>> bulkArticles(
      @RequestParam(name = "ids") List<String> ids) {
    return ResponseEntity.ok(bulkReadService.fetchSimpleByIds(ids));
  }
}

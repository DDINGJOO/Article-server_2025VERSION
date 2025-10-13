package com.teambind.articleserver.controller;

import com.teambind.articleserver.dto.response.ArticleSimpleResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bulk")
@RequiredArgsConstructor
public class BulkController {

  @GetMapping("/articles")
  public ResponseEntity<List<ArticleSimpleResponse>> bulkArticles( ) {
	  return ResponseEntity.ok(List.of());
  }
}

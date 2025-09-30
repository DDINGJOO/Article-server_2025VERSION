package com.teambind.articleserver.controller;

import static com.teambind.articleserver.utils.DataInitializer.*;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/enums")
@Slf4j
public class EnumsController {

  @GetMapping("/boards")
  public ResponseEntity<Map<Long, String>> getBoards() {
    return ResponseEntity.ok(boardMapReverse);
  }

  public ResponseEntity<Map<Long, String>> getKeywords() {
    return ResponseEntity.ok(keywordMap);
  }
}

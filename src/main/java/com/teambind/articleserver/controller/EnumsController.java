package com.teambind.articleserver.controller;

import static com.teambind.articleserver.utils.DataInitializer.*;

import com.teambind.articleserver.dto.response.common.BoardInfo;
import com.teambind.articleserver.dto.response.common.KeywordInfo;
import com.teambind.articleserver.entity.board.Board;
import com.teambind.articleserver.entity.keyword.Keyword;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
  public ResponseEntity<Map<Long, BoardInfo>> getBoards() {
    Map<Long, BoardInfo> response = new ConcurrentHashMap<>();
    List<Board> boards = boardMap.values().stream().toList();
    for (Board board : boards) {
      response.put(board.getId(), BoardInfo.fromEntity(board));
    }
    return ResponseEntity.ok(response);
  }

  @GetMapping("/keywords")
  public ResponseEntity<Map<Long, KeywordInfo>> getKeywords() {
    Map<Long, KeywordInfo> response = new ConcurrentHashMap<>();
    List<Keyword> keywords = keywordMap.values().stream().toList();
    for (Keyword keyword : keywords) {
      response.put(keyword.getId(), KeywordInfo.fromEntity(keyword));
    }
    return ResponseEntity.ok(response);
  }
}

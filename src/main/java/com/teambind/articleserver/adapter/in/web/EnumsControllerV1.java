package com.teambind.articleserver.adapter.in.web;

import com.teambind.articleserver.adapter.in.web.dto.response.enums.BoardEnumDto;
import com.teambind.articleserver.adapter.in.web.dto.response.enums.KeywordEnumDto;
import com.teambind.articleserver.application.port.in.enums.EnumProviderUseCase;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 열거형 정보 REST Controller V1
 *
 * <p>Hexagonal Architecture의 Inbound Adapter입니다. 시스템에서 사용하는 열거형 값들의 목록을 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/enums")
@RequiredArgsConstructor
@Slf4j
public class EnumsControllerV1 {

  private final EnumProviderUseCase enumProviderUseCase;

  /**
   * 사용 가능한 열거형 목록 조회 GET /api/v1/enums
   *
   * @return 열거형 타입별 사용 가능한 값 목록
   */
  @GetMapping()
  public ResponseEntity<Map<String, List<String>>> getAvailableEnums() {
    log.debug("Fetching available enums");

    Map<String, List<String>> enums = enumProviderUseCase.getAvailableEnums();

    log.debug("Available enums: {}", enums);

    return ResponseEntity.ok(enums);
  }

  /**
   * 게시판 목록 조회 GET /api/v1/enums/boards
   *
   * @return key: 게시판 ID (문자열), value: 게시판 정보 (id, name, url 등)
   */
  @GetMapping("/boards")
  public ResponseEntity<Map<String, BoardEnumDto>> getBoards() {
    log.debug("Fetching board enums");

    Map<String, BoardEnumDto> boards = enumProviderUseCase.getBoards();

    log.debug("Available boards: {}", boards);

    return ResponseEntity.ok(boards);
  }

  /**
   * 키워드 목록 조회 GET /api/v1/enums/keywords
   *
   * @return key: 키워드 ID (문자열), value: 키워드 정보 (id, name, url 등)
   */
  @GetMapping("/keywords")
  public ResponseEntity<Map<String, KeywordEnumDto>> getKeywords() {
    log.debug("Fetching keyword enums");

    Map<String, KeywordEnumDto> keywords = enumProviderUseCase.getKeywords();

    log.debug("Available keywords: {}", keywords);

    return ResponseEntity.ok(keywords);
  }
}

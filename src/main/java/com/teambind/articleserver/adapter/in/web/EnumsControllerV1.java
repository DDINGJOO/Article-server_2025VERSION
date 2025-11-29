package com.teambind.articleserver.adapter.in.web;

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
  @GetMapping
  public ResponseEntity<Map<String, List<String>>> getAvailableEnums() {
    log.debug("Fetching available enums");

    Map<String, List<String>> enums = enumProviderUseCase.getAvailableEnums();

    log.debug("Available enums: {}", enums);

    return ResponseEntity.ok(enums);
  }
}

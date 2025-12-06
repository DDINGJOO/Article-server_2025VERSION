package com.teambind.articleserver.application.port.in.enums;

import com.teambind.articleserver.adapter.in.web.dto.response.enums.BoardEnumDto;
import com.teambind.articleserver.adapter.in.web.dto.response.enums.KeywordEnumDto;
import java.util.List;
import java.util.Map;

/**
 * 열거형 정보 제공 UseCase (Inbound Port)
 *
 * <p>Hexagonal Architecture의 Inbound Port입니다. 시스템에서 사용하는 열거형 값들의 목록을 제공합니다.
 */
public interface EnumProviderUseCase {

  /**
   * 사용 가능한 열거형 목록 조회
   *
   * @return 열거형 타입별 사용 가능한 값 목록
   */
  Map<String, List<String>> getAvailableEnums();

  /**
   * 게시판 목록 조회
   *
   * @return key: 게시판 ID (문자열), value: 게시판 정보
   */
  Map<String, BoardEnumDto> getBoards();

  /**
   * 키워드 목록 조회
   *
   * @return key: 키워드 ID (문자열), value: 키워드 정보
   */
  Map<String, KeywordEnumDto> getKeywords();
}

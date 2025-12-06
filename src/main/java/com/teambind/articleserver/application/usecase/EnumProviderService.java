package com.teambind.articleserver.application.usecase;

import com.teambind.articleserver.adapter.in.web.dto.response.enums.BoardEnumDto;
import com.teambind.articleserver.adapter.in.web.dto.response.enums.KeywordEnumDto;
import com.teambind.articleserver.adapter.out.persistence.entity.board.Board;
import com.teambind.articleserver.adapter.out.persistence.entity.enums.Status;
import com.teambind.articleserver.adapter.out.persistence.entity.keyword.Keyword;
import com.teambind.articleserver.adapter.out.persistence.repository.BoardRepository;
import com.teambind.articleserver.adapter.out.persistence.repository.KeywordRepository;
import com.teambind.articleserver.application.port.in.enums.EnumProviderUseCase;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 열거형 정보 제공 서비스
 *
 * <p>Hexagonal Architecture의 Application Service입니다. 시스템에서 사용하는 열거형 값들을 제공합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnumProviderService implements EnumProviderUseCase {

  private final BoardRepository boardRepository;
  private final KeywordRepository keywordRepository;

  @Override
  public Map<String, List<String>> getAvailableEnums() {
    log.debug("Fetching available enums");

    Map<String, List<String>> enums = new HashMap<>();

    // Article Types
    enums.put("articleTypes", Arrays.asList("REGULAR", "EVENT", "NOTICE"));

    // Article Status
    enums.put(
        "articleStatus",
        Arrays.stream(Status.values()).map(Enum::name).collect(Collectors.toList()));

    // Event Status
    enums.put("eventStatus", Arrays.asList("UPCOMING", "ONGOING", "ENDED"));

    log.debug("Available enums: {}", enums);

    return enums;
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, BoardEnumDto> getBoards() {
    log.debug("Fetching board enums");

    List<Board> boards = boardRepository.findAll();
    Map<String, BoardEnumDto> boardMap = new HashMap<>();

    for (Board board : boards) {
      if (board.getIsActive()) {
        BoardEnumDto dto = BoardEnumDto.builder()
            .boardId(board.getId())
            .boardName(board.getName())
            .description(board.getDescription())
            .url("/api/v1/articles?boardId=" + board.getId())
            .isActive(board.getIsActive())
            .displayOrder(board.getDisplayOrder())
            .build();

        boardMap.put(board.getId().toString(), dto);
      }
    }

    log.debug("Available boards: {}", boardMap);
    return boardMap;
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, KeywordEnumDto> getKeywords() {
    log.debug("Fetching keyword enums");

    List<Keyword> keywords = keywordRepository.findAllWithBoard();
    Map<String, KeywordEnumDto> keywordMap = new HashMap<>();

    for (Keyword keyword : keywords) {
      if (keyword.getIsActive()) {
        KeywordEnumDto dto = KeywordEnumDto.builder()
            .keywordId(keyword.getId())
            .keywordName(keyword.getName())
            .isCommon(keyword.isCommonKeyword())
            .boardId(keyword.getBoard() != null ? keyword.getBoard().getId() : null)
            .boardName(keyword.getBoard() != null ? keyword.getBoard().getName() : null)
            .url("/api/v1/articles?keyword=" + keyword.getName())
            .isActive(keyword.getIsActive())
            .usageCount(keyword.getUsageCount())
            .build();

        keywordMap.put(keyword.getId().toString(), dto);
      }
    }

    log.debug("Available keywords: {}", keywordMap);
    return keywordMap;
  }
}

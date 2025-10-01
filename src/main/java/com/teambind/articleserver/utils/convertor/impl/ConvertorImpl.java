package com.teambind.articleserver.utils.convertor.impl;

import com.teambind.articleserver.entity.Board;
import com.teambind.articleserver.entity.Keyword;
import com.teambind.articleserver.exceptions.CustomException;
import com.teambind.articleserver.exceptions.ErrorCode;
import com.teambind.articleserver.repository.BoardRepository;
import com.teambind.articleserver.repository.KeywordRepository;
import com.teambind.articleserver.utils.convertor.Convertor;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ConvertorImpl implements Convertor {
  private final KeywordRepository keywordRepository;
  private final BoardRepository boardRepository;

  private Board convertBoardId(Long boardId) {
    return boardRepository
        .findById(boardId)
        .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
  }

  private Board convertBoardName(String boardName) {
    return boardRepository
        .findByBoardName(boardName)
        .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
  }

  // Unified methods
  @Override
  public List<Keyword> convertKeywords(List<?> keywordList) {
    if (keywordList == null) throw new CustomException(ErrorCode.REQUIRED_FIELD_NULL);
    if (keywordList.isEmpty()) return new ArrayList<>();
    Object first = keywordList.get(0);
    if (first instanceof Number) {
      @SuppressWarnings("unchecked")
      List<Number> ids = (List<Number>) keywordList;
      // 숫자 타입(Integer, Long 등)에 상관없이 long으로 변환
      List<Long> longIds = new ArrayList<>();
      for (Number n : ids) {
        longIds.add(n.longValue());
      }
      return convertKeywordsInternalFromIds(longIds);
    } else if (first instanceof String) {
      List<String> names = (List<String>) keywordList;
      log.info("Board as String: '{}'", names);
      boolean allNumeric = true;
      for (String s : names) {
        if (s == null || !s.matches("\\d+")) {
          allNumeric = false;
          break;
        }
      }
      if (allNumeric) {
        List<Long> longIds = new ArrayList<>(names.size());
        for (String s : names) {
          try {
            longIds.add(Long.parseLong(s));
          } catch (NumberFormatException e) {
            log.warn("Failed to parse keyword id from string '{}'", s);

            return convertKeywordsInternalFromNames(names);
          }
        }
        return convertKeywordsInternalFromIds(longIds);
      }
      return convertKeywordsInternalFromNames(names);
    }
    throw new CustomException(ErrorCode.REQUIRED_FIELD_NOT_VALID);
  }

  @Override
  public Board convertBoard(Object board) {
    if (board == null) throw new CustomException(ErrorCode.REQUIRED_FIELD_NULL);
    if (board instanceof Number) {
      Long boardId = ((Number) board).longValue();
      log.info("Board ID: {}", boardId);
      return convertBoardId(boardId);
    } else if (board instanceof String) {
      String s = (String) board;
      log.info("Board as String: '{}'", s);
      if (s.matches("\\d+")) {
        try {
          Long boardId = Long.parseLong(s);
          log.info("Parsed Board ID from String: {}", boardId);
          return convertBoardId(boardId);
        } catch (CustomException ex) {
          // 매우 큰 수 등 파싱 실패 시 이름으로 시도하도록 아래로 떨어뜨립니다.
          log.warn("Failed to parse board id from string '{}'", s);
        }
      }
      return convertBoardName(s);
    }
    throw new CustomException(ErrorCode.REQUIRED_FIELD_NOT_VALID);
  }

  // internal helpers
  private List<Keyword> convertKeywordsInternalFromIds(List<Long> keywordList) {
    return keywordRepository.findAllById(keywordList);
  }

  private List<Keyword> convertKeywordsInternalFromNames(List<String> keywordList) {
    return keywordRepository.findAllByKeywordIn(keywordList);
  }
}

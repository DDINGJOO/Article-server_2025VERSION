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
import org.springframework.stereotype.Component;

@Component
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
      @SuppressWarnings("unchecked")
      List<String> names = (List<String>) keywordList;
      return convertKeywordsInternalFromNames(names);
    }
    throw new CustomException(ErrorCode.REQUIRED_FIELD_NOT_VALID);
  }

  @Override
  public Board convertBoard(Object board) {
    if (board == null) throw new CustomException(ErrorCode.REQUIRED_FIELD_NULL);
    if (board instanceof Number) {
      Long boardId = ((Number) board).longValue();
      return convertBoardId(boardId);
    } else if (board instanceof String) {
      return convertBoardName((String) board);
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

package com.teambind.articleserver.utils.convertor.impl;

import static com.teambind.articleserver.utils.DataInitializer.*;

import com.teambind.articleserver.entity.Board;
import com.teambind.articleserver.entity.Keyword;
import com.teambind.articleserver.exceptions.CustomException;
import com.teambind.articleserver.exceptions.ErrorCode;
import com.teambind.articleserver.utils.convertor.Convertor;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ConvertorImpl implements Convertor {

  private Board convertBoardId(Long boardId) {
    String name = boardMapReverse.get(boardId);
    return new Board(boardId, name);
  }

  private Board convertBoardName(String boardName) {
    Long id = boardMap.get(boardName);
    return new Board(id, boardName);
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
    List<Keyword> keywordListConverted = new ArrayList<>();
    for (Long keywordId : keywordList) {
      String value = keywordMap.get(keywordId);
      keywordListConverted.add(new Keyword(keywordId, value));
    }
    return keywordListConverted;
  }

  private List<Keyword> convertKeywordsInternalFromNames(List<String> keywordList) {
    List<Keyword> keywordListConverted = new ArrayList<>();
    for (String keywordName : keywordList) {
      Long keywordId = keywordMapReverse.get(keywordName);
      keywordListConverted.add(new Keyword(keywordId, keywordName));
    }
    return keywordListConverted;
  }
}

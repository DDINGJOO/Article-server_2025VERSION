package com.teambind.articleserver.utils.validator.impl;

import static com.teambind.articleserver.utils.DataInitializer.*;

import com.teambind.articleserver.exceptions.CustomException;
import com.teambind.articleserver.exceptions.ErrorCode;
import com.teambind.articleserver.utils.validator.Validator;
import lombok.extern.slf4j.Slf4j;

//
@Slf4j
public class ValidatorImpl implements Validator {

  void boardIdValidator(Long boardId) {
    nullCheck(boardId);
    if (boardMapReverse.get(boardId) == null) {
      throw new CustomException(ErrorCode.REQUIRED_FIELD_NOT_VALID);
    }
    return;
  }

  void boardNameValidator(String boardName) {
    nullCheck(boardName);
    if (boardMap.get(boardName) == null) {
      throw new CustomException(ErrorCode.REQUIRED_FIELD_NOT_VALID);
    }
  }

  void keywordIdValidator(Long keywordId) {
    nullCheck(keywordId);
    if (keywordMap.get(keywordId) == null) {
      throw new CustomException(ErrorCode.REQUIRED_FIELD_NOT_VALID);
    }
  }

  void keywordNameValidator(String keywordName) {
    nullCheck(keywordName);

    if (keywordMapReverse.get(keywordName) == null) {
      throw new CustomException(ErrorCode.REQUIRED_FIELD_NOT_VALID);
    }
  }

  @Override
  public void boardValidator(Object board) {
    if (board == null) {
      throw new CustomException(ErrorCode.REQUIRED_FIELD_NULL);
    }
    if (board instanceof Long) {
      log.info("Long type board : {}", board);
      boardIdValidator((Long) board);
    }
    if (board instanceof String) {
      log.info("String type board : {}", board);
      boardNameValidator((String) board);
    }
    throw new CustomException(ErrorCode.REQUIRED_FIELD_NOT_VALID);
  }

  @Override
  public void keywordValidator(Object keyword) {
    if (keyword == null) {
      throw new CustomException(ErrorCode.REQUIRED_FIELD_NULL);
    }
    if (keyword instanceof Long) {
      log.info("Long type keyword : {}", keyword);
      keywordIdValidator((Long) keyword);
    }
    if (keyword instanceof String) {
      log.info("String type keyword : {}", keyword);
      keywordNameValidator((String) keyword);
    }
    throw new CustomException(ErrorCode.REQUIRED_FIELD_NOT_VALID);
  }

  private void nullCheck(Object obj) {
    if (obj == null) {
      throw new CustomException(ErrorCode.REQUIRED_FIELD_NULL);
    }
  }
}

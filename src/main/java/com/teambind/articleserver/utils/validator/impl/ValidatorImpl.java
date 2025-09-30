package com.teambind.articleserver.utils.validator.impl;

import com.teambind.articleserver.exceptions.CustomException;
import com.teambind.articleserver.exceptions.ErrorCode;
import com.teambind.articleserver.utils.validator.Validator;

import static com.teambind.articleserver.utils.DataInitializer.*;

public class ValidatorImpl implements Validator {
	@Override
	public void boardIdValidator(Long boardId) {
		nullCheck(boardId);
		if (boardMapReverse.get(boardId) == null) {
			throw new CustomException(ErrorCode.BOARD_ID_NULL);
		}
		return;
		
	}
	
	@Override
	public void boardNameValidator(String boardName) {
		nullCheck(boardName);
		if (boardMap.get(boardName) == null) {
			throw new CustomException(ErrorCode.BOARD_NAME_NULL);
		}
		
	}
	
	@Override
	public void keywordIdValidator(Long keywordId) {
		nullCheck(keywordId);
		if (keywordMap.get(keywordId) == null) {
			throw new CustomException(ErrorCode.KEYWORD_ID_NULL);
		}
		
	}
	
	@Override
	public void keywordNameValidator(String keywordName) {
		nullCheck(keywordName);
		
		if (keywordMapReverse.get(keywordName) == null) {
			throw new CustomException(ErrorCode.KEYWORD_NAME_NULL);
		}
	}
	
	private void nullCheck(Object obj) {
		if (obj == null) {
			throw new CustomException(ErrorCode.REQUIRED_FIELD_NULL);
		}
	}
}

package com.teambind.articleserver.utils.validator;

public interface Validator {
	
	void boardIdValidator(Long boardId);
	
	void boardNameValidator(String boardName);
	
	void keywordIdValidator(Long keywordId);
	
	void keywordNameValidator(String keywordName);
	
	
	// TODO : Object -> isInstance 하면 Id value 한번에 합칠수 있음
}

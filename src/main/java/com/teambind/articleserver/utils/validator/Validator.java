package com.teambind.articleserver.utils.validator;

public interface Validator {
	
	void boardIdValidator(Long boardId);
	
	void boardNameValidator(String boardName);
	
	void keywordIdValidator(Long keywordId);
	
	void keywordNameValidator(String keywordName);
	
	
}

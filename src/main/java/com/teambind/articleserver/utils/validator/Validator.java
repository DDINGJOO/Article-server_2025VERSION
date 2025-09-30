package com.teambind.articleserver.utils.validator;

public interface Validator {
	

	
	// TODO : Object -> isInstance 하면 Id value 한번에 합칠수 있음
	
	void boardValidator(Object board);
	
	void keywordValidator(Object keyword);
}

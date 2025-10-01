package com.teambind.articleserver.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
	BOARD_ID_NULL("REQ_ERR_001", "BOARD_ID_IS_NULL", HttpStatus.BAD_REQUEST),
	REQUIRED_FIELD_NULL("REQ_ERR_002", "REQUIRED_FIELD_IS_NULL", HttpStatus.BAD_REQUEST),
	BOARD_NAME_NULL("REQ_ERR_003", "BOARD_NAME_IS_NULL", HttpStatus.BAD_REQUEST),
	KEYWORD_ID_NULL("REQ_ERR_004", "KEYWORD_ID_IS_NULL", HttpStatus.BAD_REQUEST),
	KEYWORD_NAME_NULL("REQ_ERR_005", "KEYWORD_NAME_IS_NULL", HttpStatus.BAD_REQUEST),
	ARTICLE_NOT_FOUND("ART_ERR_001", "ARTICLE_NOT_FOUND", HttpStatus.NOT_FOUND),
	REQUIRED_FIELD_NOT_VALID("REQ_ERR_006", "REQUIRED_FIELD_NOT_VALIED", HttpStatus.BAD_REQUEST),
  ARTICLE_IS_BLOCKED("ART_ERR_002", "ARTICLE_IS_BLOCKED", HttpStatus.BAD_REQUEST),
  ARTICLE_IS_NULL("ART_ERR_003", "ARTICLE_IS_NULL", HttpStatus.BAD_REQUEST),
  BOARD_NOT_FOUND("ART_ERR_004", "BOARD_NOT_FOUND", HttpStatus.NOT_FOUND),
  ;

	private final String errCode;
	private final String message;
	private final HttpStatus status;
	
	ErrorCode(String errCode, String message, HttpStatus status) {
		
		this.status = status;
		this.errCode = errCode;
		this.message = message;
	}
	
	@Override
	public String toString() {
		return "ErrorCode{" +
				" status='" + status + '\'' +
				"errCode='" + errCode + '\'' +
				", message='" + message + '\'' +
				'}';
	}
	
}

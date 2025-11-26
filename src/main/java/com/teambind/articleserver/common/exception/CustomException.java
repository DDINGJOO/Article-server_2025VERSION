package com.teambind.articleserver.common.exception;

import org.springframework.http.HttpStatus;

public class CustomException extends RuntimeException {
  private final ErrorCode errorcode;

  public CustomException(ErrorCode errorcode) {
    super(errorcode.toString());
    this.errorcode = errorcode;
  }

  public CustomException(ErrorCode errorcode, String message) {
    super(errorcode.toString() + ": " + message);
    this.errorcode = errorcode;
  }

  public HttpStatus getStatus() {
    return errorcode.getStatus();
  }
}

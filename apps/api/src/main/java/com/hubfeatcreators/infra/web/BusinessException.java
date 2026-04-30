package com.hubfeatcreators.infra.web;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
  private final String code;
  private final HttpStatus status;

  public BusinessException(String code, String message, HttpStatus status) {
    super(message);
    this.code = code;
    this.status = status;
  }

  public static BusinessException notFound(String entity) {
    return new BusinessException(entity + "_NOT_FOUND", entity + " não encontrado.", HttpStatus.NOT_FOUND);
  }

  public static BusinessException conflict(String code, String message) {
    return new BusinessException(code, message, HttpStatus.CONFLICT);
  }

  public static BusinessException badRequest(String code, String message) {
    return new BusinessException(code, message, HttpStatus.BAD_REQUEST);
  }

  public static BusinessException unauthorized(String message) {
    return new BusinessException("UNAUTHORIZED", message, HttpStatus.UNAUTHORIZED);
  }

  public String getCode() { return code; }
  public HttpStatus getStatus() { return status; }
}

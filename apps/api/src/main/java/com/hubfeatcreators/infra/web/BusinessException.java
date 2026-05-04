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
        return new BusinessException(
                entity + "_NOT_FOUND", entity + " não encontrado.", HttpStatus.NOT_FOUND);
    }

    public static BusinessException notFound() {
        return new BusinessException("NOT_FOUND", "Recurso não encontrado.", HttpStatus.NOT_FOUND);
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

    public static BusinessException unprocessable(String code, String message) {
        return new BusinessException(code, message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static BusinessException tooManyRequests(String code, String message) {
        return new BusinessException(code, message, HttpStatus.TOO_MANY_REQUESTS);
    }

    public static BusinessException gone(String code, String message) {
        return new BusinessException(code, message, HttpStatus.GONE);
    }

    public static BusinessException forbidden(String code, String message) {
        return new BusinessException(code, message, HttpStatus.FORBIDDEN);
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

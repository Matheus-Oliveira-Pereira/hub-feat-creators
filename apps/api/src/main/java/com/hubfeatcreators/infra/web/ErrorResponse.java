package com.hubfeatcreators.infra.web;

import java.util.List;

public record ErrorResponse(Error error) {

    public record Error(String code, String message, String traceId, List<Detail> details) {}

    public record Detail(String field, String message) {}

    public static ErrorResponse of(String code, String message, String traceId) {
        return new ErrorResponse(new Error(code, message, traceId, List.of()));
    }

    public static ErrorResponse of(
            String code, String message, String traceId, List<Detail> details) {
        return new ErrorResponse(new Error(code, message, traceId, details));
    }
}

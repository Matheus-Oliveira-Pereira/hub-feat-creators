package com.hubfeatcreators.infra.social;

public class SocialApiException extends RuntimeException {

    private final int statusCode;

    public SocialApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public SocialApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}

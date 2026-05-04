package com.hubfeatcreators.domain.whatsapp;

public class MetaApiException extends RuntimeException {
    public MetaApiException(String message) { super(message); }
    public MetaApiException(String message, Throwable cause) { super(message, cause); }
}

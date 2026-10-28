package com.smartdesk.client;

import org.springframework.http.HttpStatus;

public class AnaliseException extends RuntimeException {
    private final HttpStatus status;
    public AnaliseException(HttpStatus status, String message) { super(message); this.status = status; }
    public HttpStatus status() { return status; }
}

package com.clarify.common.error;

import org.springframework.http.HttpStatus;

/** Base for errors that are part of the API contract: rendered as {"message": "..."}. */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

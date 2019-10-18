package org.commonjava.indy.model.rest;

import java.time.LocalDateTime;

public class IndyRestMapperResponse {

    String message;
    Throwable cause;
    LocalDateTime timestamp;

    public IndyRestMapperResponse() {
    }

    public IndyRestMapperResponse(String message, Throwable cause, LocalDateTime timestamp) {
        this.message = message;
        this.cause = cause;
        this.timestamp = timestamp;
    }

    public IndyRestMapperResponse(String message, LocalDateTime timestamp) {
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Throwable getCause() {
        return cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

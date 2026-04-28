package com.openclaw.delayqueue.model;

public record HttpCallResult(Integer statusCode, String responseBody, String errorMessage) {
    public boolean success() {
        return errorMessage == null && statusCode != null && statusCode >= 200 && statusCode < 300;
    }
}

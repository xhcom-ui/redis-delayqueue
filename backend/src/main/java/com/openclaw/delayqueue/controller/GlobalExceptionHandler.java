package com.openclaw.delayqueue.controller;

import com.openclaw.delayqueue.model.ApiResponse;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncDisconnect() {
        // SSE 客户端主动断开时无需写 JSON 响应。
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<String> handle(Exception e) {
        return ApiResponse.fail(e.getMessage());
    }
}
